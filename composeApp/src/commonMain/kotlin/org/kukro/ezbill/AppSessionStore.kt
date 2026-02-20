package org.kukro.ezbill

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import com.russhwolf.settings.Settings
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.models.SpaceMember
import kotlin.random.Random
import kotlin.time.Clock

object AppSessionStore {
    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        println("AppSessionStore.unhandledCoroutineError message=${throwable.message}")
    }
    private val scopeContext: CoroutineContext = SupervisorJob() + Dispatchers.Main + coroutineErrorHandler
    private val scope = CoroutineScope(scopeContext)
    private var started = false
    private var sessionJob: Job? = null
    private val bootstrapMutex = Mutex()
    private var isBootstrapping = false
    private var isSigningOut = false

    private var expensesChannel: RealtimeChannel? = null
    private var membersChannel: RealtimeChannel? = null
    private var expensesCollectJob: Job? = null
    private var membersCollectJob: Job? = null
    private var foregroundRecoverJob: Job? = null
    private var lastForegroundRecoverAtMs: Long = 0L
    private val settings = Settings()

    private val _state = MutableStateFlow(AppSessionState())
    val state: StateFlow<AppSessionState> = _state.asStateFlow()

    fun start() {
        if (started) return
        started = true
        sessionJob = safeLaunch("sessionStatus.collect") {
            while (isActive) {
                try {
                    supabase.auth.sessionStatus.collect { status ->
                        when (status) {
                            is SessionStatus.Initializing -> {
                                if (isSigningOut) return@collect
                                _state.value = _state.value.copy(status = AppSessionStatus.Initializing)
                            }

                            is SessionStatus.NotAuthenticated -> {
                                clearData(AppSessionStatus.Unauthenticated)
                            }

                            is SessionStatus.Authenticated -> {
                                if (isSigningOut) return@collect
                                if (isBootstrapping) return@collect
                                val authUserId = supabase.auth.currentUserOrNull()?.id
                                val current = _state.value
                                val alreadyReadyForSameUser =
                                    current.status is AppSessionStatus.Ready &&
                                            current.currentUserId != null &&
                                            current.currentUserId == authUserId
                                if (!alreadyReadyForSameUser) {
                                    bootstrapAuthenticated()
                                }
                            }

                            is SessionStatus.RefreshFailure -> {
                                if (isSigningOut) return@collect
                                _state.value = _state.value.copy(
                                    status = AppSessionStatus.Error("Session refresh failed")
                                )
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("AppSessionStore.sessionStatus.collect failed message=${e.message}")
                    delay(1_000)
                }
            }
        }
    }

    fun onAppForeground() {
        start()
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastForegroundRecoverAtMs < FOREGROUND_RECOVER_MIN_INTERVAL_MS) return
        lastForegroundRecoverAtMs = now

        foregroundRecoverJob?.cancel()
        foregroundRecoverJob = safeLaunch("foregroundRecover") {
            recoverAfterForeground()
        }
    }

    fun onAppBackground() {
        clearSubscriptions()
    }

    suspend fun switchSpace(space: Space) {
        val current = _state.value
        if (space.id.isBlank()) return
        _state.value = current.copy(status = AppSessionStatus.Loading, selectedSpace = space)
        try {
            val members = loadMembers(space.id)
            val expenses = loadExpenses(space.id)
            val memberProfiles = loadProfilesForMembers(members, _state.value.profile)
            _state.value = _state.value.copy(
                status = AppSessionStatus.Ready,
                memberProfiles = memberProfiles,
                members = members,
                expenses = expenses
            )
            saveSelectedSpaceId(space.id)
            subscribeForSpace(space.id)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = AppSessionStatus.Error(e.message ?: "Switch space failed")
            )
        }
    }

    suspend fun createSpace(name: String, displayName: String) {
        val space = SupabaseService.createSpace(name, displayName)
        val current = _state.value
        val mergedSpaces = (current.spaces + space).distinctBy { it.id }
        _state.value = current.copy(
            status = AppSessionStatus.Loading,
            spaces = mergedSpaces,
            selectedSpace = space
        )
        bootstrapAuthenticated(selectedSpaceId = space.id, fallbackSelectedSpace = space)
    }

    suspend fun joinSpace(code: String, displayName: String) {
        val space = supabase.postgrest.rpc(
            "join_space_by_code",
            mapOf(
                "p_code" to code,
                "p_display_name" to displayName
            )
        ).decodeAs<Space>()
        val current = _state.value
        val mergedSpaces = (current.spaces + space).distinctBy { it.id }
        _state.value = current.copy(
            status = AppSessionStatus.Loading,
            spaces = mergedSpaces,
            selectedSpace = space
        )
        bootstrapAuthenticated(selectedSpaceId = space.id, fallbackSelectedSpace = space)
    }

    suspend fun updateAvatarOnly(imageBytes: ByteArray) {
        val avatarUrl = SupabaseService.uploadAvatarBytes(imageBytes)
        val profile = SupabaseService.saveProfile(avatarUrl = avatarUrl)
        mergeProfile(profile)
    }

    suspend fun updateUsernameOnly(username: String) {
        val profile = SupabaseService.saveProfile(username = username)
        mergeProfile(profile)
    }

    suspend fun updateProfileWithNewAvatar(username: String, imageBytes: ByteArray) {
        val avatarUrl = SupabaseService.uploadAvatarBytes(imageBytes)
        val profile = SupabaseService.saveProfile(username = username, avatarUrl = avatarUrl)
        mergeProfile(profile)
    }

    suspend fun chooseAnonymous() {
        signInAnonymous()
    }

    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        enforceSingleSession()
    }

    suspend fun signUpWithEmail(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        enforceSingleSession()
    }

    suspend fun signOut() = withContext(NonCancellable) {
        isSigningOut = true
        try {
            bootstrapMutex.withLock {
                clearSubscriptions()
                _state.value = AppSessionState(
                    status = AppSessionStatus.Unauthenticated
                )
                runCatching {
                    supabase.auth.signOut(scope = SignOutScope.LOCAL)
                }.onFailure {
                    println("AppSessionStore.signOut local failed message=${it.message}")
                }
            }
        } finally {
            isSigningOut = false
        }
    }

    suspend fun bootstrapAuthenticated(
        selectedSpaceId: String? = null,
        fallbackSelectedSpace: Space? = null
    ) {
        if (isSigningOut) return
        bootstrapMutex.withLock {
            if (isSigningOut) return@withLock
            isBootstrapping = true
            _state.value = _state.value.copy(status = AppSessionStatus.Loading)
            try {
                println("AppSessionStore.bootstrap start selectedSpaceId=$selectedSpaceId")
                val authUser = runCatching {
                    supabase.auth.retrieveUserForCurrentSession(updateSession = false)
                }.getOrElse { supabase.auth.currentUserOrNull() }
                val currentUserId = authUser?.id
                val currentUserEmail = authUser?.email
                    ?.takeIf { it.isNotBlank() }
                    ?: authUser?.newEmail
                        ?.takeIf { it.isNotBlank() }
                    ?: authUser?.userMetadata
                        ?.get("email")
                        ?.toString()
                        ?.trim('"')
                        ?.takeIf { it.isNotBlank() }
                val provider = authUser?.appMetadata
                    ?.get("provider")
                    ?.toString()
                    ?.trim('"')
                    ?.lowercase()
                val providers = authUser?.appMetadata
                    ?.get("providers")
                    ?.toString()
                    ?.lowercase()
                    .orEmpty()
                val isAnonymousUser = provider == "anonymous" || "anonymous" in providers

                // As soon as auth info is known, expose it so UI can enter HomeScreen early.
                _state.value = _state.value.copy(
                    status = AppSessionStatus.Loading,
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    isAnonymousUser = isAnonymousUser
                )
                if (isSigningOut) return@withLock

                println("auth email=${authUser?.email}, newEmail=${authUser?.newEmail}, currentUserEmail=$currentUserEmail, provider=$provider")
                println("AppSessionStore.bootstrap step=profile start")
                val profile = SupabaseService.getOrCreateMyProfile()
                println("AppSessionStore.bootstrap step=profile done userId=${profile.userId}")

                println("AppSessionStore.bootstrap step=spaces start")
                val spaces = loadSpaces()
                val spacesWithFallback = if (
                    fallbackSelectedSpace != null && spaces.none { it.id == fallbackSelectedSpace.id }
                ) {
                    listOf(fallbackSelectedSpace) + spaces
                } else {
                    spaces
                }
                println("AppSessionStore.bootstrap step=spaces done count=${spacesWithFallback.size}")
                val selected = selectSpace(spacesWithFallback, selectedSpaceId, fallbackSelectedSpace)
                println("AppSessionStore.bootstrap step=select done selectedId=${selected?.id}")
                saveSelectedSpaceId(selected?.id)

                val members = selected?.id?.let { sid ->
                    println("AppSessionStore.bootstrap step=members start spaceId=$sid")
                    loadMembers(sid)
                }.orEmpty()
                println("AppSessionStore.bootstrap step=members done count=${members.size}")

                val expenses = selected?.id?.let { sid ->
                    println("AppSessionStore.bootstrap step=expenses start spaceId=$sid")
                    loadExpenses(sid)
                }.orEmpty()
                println("AppSessionStore.bootstrap step=expenses done count=${expenses.size}")

                println("AppSessionStore.bootstrap step=memberProfiles start")
                val memberProfiles = loadProfilesForMembers(members, profile)
                println("AppSessionStore.bootstrap step=memberProfiles done count=${memberProfiles.size}")

                _state.value = AppSessionState(
                    status = AppSessionStatus.Ready,
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    isAnonymousUser = isAnonymousUser,
                    profile = profile,
                    memberProfiles = memberProfiles,
                    spaces = spacesWithFallback,
                    selectedSpace = selected,
                    members = members,
                    expenses = expenses
                )
                println("AppSessionStore.bootstrap done status=Ready")
                if (isSigningOut) return@withLock
                subscribeForSpace(selected?.id)
            } catch (e: Exception) {
                println("AppSessionStore.bootstrap failed message=${e.message}")
                _state.value = _state.value.copy(
                    status = AppSessionStatus.Error(e.message ?: "Bootstrap failed")
                )
            } finally {
                isBootstrapping = false
                println("AppSessionStore.bootstrap end")
            }
        }
    }

    private suspend fun signInAnonymous() {
        val username = generateUsername()
        supabase.auth.signInAnonymously(
            data = mapOf(
                "username" to username,
                "avatar" to AppConfig.DEFAULT_AVATAR
            )
        )
    }

    private fun clearData(status: AppSessionStatus) {
        _state.value = AppSessionState(
            status = status
        )
        clearSubscriptions()
    }

    private suspend fun loadSpaces(): List<Space> {
        val myCreatedList = SupabaseService.fetchMyCreatedSpaces()
        val myJoinedList = SupabaseService.fetchJoinedSpaces()
        return (myCreatedList + myJoinedList).distinctBy { it.id }
    }

    private suspend fun loadMembers(spaceId: String): List<SpaceMember> {
        val result = supabase.postgrest["space_memberships"].select {
            filter { eq("space_id", spaceId) }
            order("joined_at", Order.ASCENDING)
        }
        return result.decodeList()
    }

    private suspend fun loadExpenses(spaceId: String): List<Expense> {
        val result = supabase.postgrest["expenses"].select {
            filter { eq("space_id", spaceId) }
            order("created_at", Order.DESCENDING)
        }
        return result.decodeList()
    }

    private fun selectSpace(
        spaces: List<Space>,
        selectedSpaceId: String?,
        fallbackSelectedSpace: Space? = null
    ): Space? {
        if (spaces.isEmpty()) return fallbackSelectedSpace
        val preferred = selectedSpaceId
            ?: _state.value.selectedSpace?.id
            ?: readSelectedSpaceId()
        return spaces.firstOrNull { it.id == preferred }
            ?: fallbackSelectedSpace
            ?: spaces.first()
    }

    private fun saveSelectedSpaceId(spaceId: String?) {
        if (spaceId.isNullOrBlank()) {
            settings.remove(KEY_SELECTED_SPACE_ID)
            return
        }
        settings.putString(KEY_SELECTED_SPACE_ID, spaceId)
    }

    private fun readSelectedSpaceId(): String? = settings.getStringOrNull(KEY_SELECTED_SPACE_ID)

    private fun subscribeForSpace(spaceId: String?) {
        clearSubscriptions()
        if (spaceId.isNullOrBlank()) return
        subscribeExpenses(spaceId)
        subscribeMembers(spaceId)
    }

    private fun subscribeExpenses(spaceId: String) {
        val channel = supabase.realtime.channel("expenses-$spaceId")
        expensesChannel = channel
        expensesCollectJob = safeLaunch("expenses.collect spaceId=$spaceId") {
            channel
                .postgresChangeFlow<PostgresAction.Insert>("public") {
                    table = "expenses"
                    filter("space_id", FilterOperator.EQ, spaceId)
                }
                .collect { payload ->
                    val item = runCatching { payload.decodeRecord<Expense>() }.getOrNull() ?: return@collect
                    val current = _state.value
                    if (current.selectedSpace?.id != spaceId) return@collect
                    if (current.expenses.any { it.id == item.id }) return@collect
                    _state.value = current.copy(expenses = listOf(item) + current.expenses)
                }
        }
        safeLaunch("expenses.subscribe spaceId=$spaceId") {
            channel.subscribe()
        }
    }

    private fun subscribeMembers(spaceId: String) {
        val channel = supabase.realtime.channel("members-$spaceId")
        membersChannel = channel
        membersCollectJob = safeLaunch("members.collect spaceId=$spaceId") {
            channel
                .postgresChangeFlow<PostgresAction.Insert>("public") {
                    table = "space_memberships"
                    filter("space_id", FilterOperator.EQ, spaceId)
                }
                .collect { payload ->
                    val item = runCatching { payload.decodeRecord<SpaceMember>() }.getOrNull() ?: return@collect
                    val current = _state.value
                    if (current.selectedSpace?.id != spaceId) return@collect
                    if (current.members.any { it.userId == item.userId }) return@collect
                    val profile = loadProfileByUserId(item.userId)
                    val updatedProfiles = if (profile != null) {
                        current.memberProfiles + (item.userId to profile)
                    } else {
                        current.memberProfiles
                    }
                    _state.value = current.copy(
                        members = current.members + item,
                        memberProfiles = updatedProfiles
                    )
                }
        }
        safeLaunch("members.subscribe spaceId=$spaceId") {
            channel.subscribe()
        }
    }

    private fun clearSubscriptions() {
        expensesCollectJob?.cancel()
        membersCollectJob?.cancel()
        expensesCollectJob = null
        membersCollectJob = null

        val oldExpensesChannel = expensesChannel
        val oldMembersChannel = membersChannel
        expensesChannel = null
        membersChannel = null

        safeLaunch("clearSubscriptions.unsubscribe") {
            runCatching { oldExpensesChannel?.unsubscribe() }
                .onFailure { println("AppSessionStore.unsubscribe expenses failed message=${it.message}") }
            runCatching { oldMembersChannel?.unsubscribe() }
                .onFailure { println("AppSessionStore.unsubscribe members failed message=${it.message}") }
        }
    }

    private suspend fun recoverAfterForeground() {
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return
        val current = _state.value
        val selectedSpace = current.selectedSpace
        val selectedSpaceId = selectedSpace?.id?.takeIf { it.isNotBlank() }

        val shouldBootstrap = current.currentUserId == null ||
                current.currentUserId != authUserId ||
                current.status is AppSessionStatus.Unauthenticated ||
                current.status is AppSessionStatus.Error ||
                selectedSpaceId == null

        if (shouldBootstrap) {
            bootstrapAuthenticated(
                selectedSpaceId = selectedSpaceId,
                fallbackSelectedSpace = selectedSpace
            )
            return
        }

        try {
            val members = loadMembers(selectedSpaceId)
            val expenses = loadExpenses(selectedSpaceId)
            val memberProfiles = loadProfilesForMembers(members, current.profile)
            _state.value = _state.value.copy(
                status = AppSessionStatus.Ready,
                members = members,
                expenses = expenses,
                memberProfiles = memberProfiles
            )
        } catch (e: Exception) {
            println("AppSessionStore.recoverAfterForeground refresh failed message=${e.message}")
        } finally {
            subscribeForSpace(selectedSpaceId)
        }
    }

    private suspend fun loadProfilesForMembers(
        members: List<SpaceMember>,
        currentProfile: Profile?
    ): Map<String, Profile> {
        val map = mutableMapOf<String, Profile>()
        currentProfile?.let { if (it.userId.isNotBlank()) map[it.userId] = it }
        for (member in members) {
            if (map.containsKey(member.userId)) continue
            loadProfileByUserId(member.userId)?.let { map[member.userId] = it }
        }
        return map
    }

    private suspend fun loadProfileByUserId(userId: String): Profile? {
        val rows = supabase.postgrest["profiles"].select {
            filter { eq("user_id", userId) }
            limit(1)
        }.decodeList<Profile>()
        return rows.firstOrNull()
    }

    private fun mergeProfile(profile: Profile) {
        val current = _state.value
        val uid = profile.userId
        val updatedMap = if (uid.isBlank()) current.memberProfiles else {
            current.memberProfiles + (uid to profile)
        }
        val updatedCurrentProfile = if (current.currentUserId == uid) profile else current.profile
        _state.value = current.copy(profile = updatedCurrentProfile, memberProfiles = updatedMap)
    }

    private suspend fun enforceSingleSession() {
        runCatching {
            supabase.auth.signOut(scope = SignOutScope.OTHERS)
        }.onFailure {
            println("AppSessionStore.enforceSingleSession failed message=${it.message}")
        }
    }

    private fun safeLaunch(
        name: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("AppSessionStore.safeLaunch($name) failed message=${e.message}")
        }
    }

    private const val FOREGROUND_RECOVER_MIN_INTERVAL_MS = 1_500L
    private const val KEY_SELECTED_SPACE_ID = "selected_space_id"
}

private fun generateUsername(): String {
    val adjectives = listOf(
        "大大的", "小小的", "勤奋的", "可爱的", "憨憨的", "聪明的",
        "萌萌的", "酷酷的", "温柔的", "活泼的", "懒懒的", "调皮的",
        "认真的", "开心的", "慢吞吞的", "急匆匆的", "圆滚滚的", "亮晶晶的"
    )
    val nouns = listOf(
        "猕猴桃", "小蜜蜂", "小猫咪", "小狗狗", "小兔子", "小松鼠",
        "小熊猫", "小老虎", "小绵羊", "小鸭子", "小金鱼", "小蜗牛",
        "小草莓", "小西瓜", "小葡萄", "小苹果", "小橘子", "小蘑菇"
    )
    return adjectives[Random.nextInt(adjectives.size)] + nouns[Random.nextInt(nouns.size)]
}

package org.kukro.ezbill

import io.github.jan.supabase.auth.auth
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.models.SpaceMember
import kotlin.random.Random

object AppSessionStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false
    private var sessionJob: Job? = null

    private var expensesChannel: RealtimeChannel? = null
    private var membersChannel: RealtimeChannel? = null
    private var expensesCollectJob: Job? = null
    private var membersCollectJob: Job? = null

    private val _state = MutableStateFlow(AppSessionState())
    val state: StateFlow<AppSessionState> = _state.asStateFlow()

    fun start() {
        if (started) return
        started = true
        sessionJob = scope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        _state.value = _state.value.copy(status = AppSessionStatus.Initializing)
                    }

                    is SessionStatus.NotAuthenticated -> {
                        clearData(AppSessionStatus.Unauthenticated)
                        signInAnonymous()
                    }

                    is SessionStatus.Authenticated -> {
                        bootstrapAuthenticated()
                    }

                    is SessionStatus.RefreshFailure -> {
                        _state.value = _state.value.copy(
                            status = AppSessionStatus.Error("Session refresh failed")
                        )
                    }
                }
            }
        }
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
            subscribeForSpace(space.id)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = AppSessionStatus.Error(e.message ?: "Switch space failed")
            )
        }
    }

    suspend fun createSpace(name: String, displayName: String) {
        val space = SupabaseService.createSpace(name, displayName)
        bootstrapAuthenticated(selectedSpaceId = space.id)
    }

    suspend fun joinSpace(code: String, displayName: String) {
        val space = supabase.postgrest.rpc(
            "join_space_by_code",
            mapOf(
                "p_code" to code,
                "p_display_name" to displayName
            )
        ).decodeAs<Space>()
        bootstrapAuthenticated(selectedSpaceId = space.id)
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

    suspend fun bootstrapAuthenticated(selectedSpaceId: String? = null) {
        _state.value = _state.value.copy(status = AppSessionStatus.Loading)
        try {
            val authUser = supabase.auth.currentUserOrNull()
            val currentUserId = authUser?.id
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
            val profile = SupabaseService.getOrCreateMyProfile()
            val spaces = loadSpaces()
            val selected = selectSpace(spaces, selectedSpaceId)
            val members = selected?.id?.let { loadMembers(it) }.orEmpty()
            val expenses = selected?.id?.let { loadExpenses(it) }.orEmpty()
            val memberProfiles = loadProfilesForMembers(members, profile)

            _state.value = AppSessionState(
                status = AppSessionStatus.Ready,
                currentUserId = currentUserId,
                isAnonymousUser = isAnonymousUser,
                profile = profile,
                memberProfiles = memberProfiles,
                spaces = spaces,
                selectedSpace = selected,
                members = members,
                expenses = expenses
            )
            subscribeForSpace(selected?.id)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = AppSessionStatus.Error(e.message ?: "Bootstrap failed")
            )
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
        _state.value = AppSessionState(status = status)
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

    private fun selectSpace(spaces: List<Space>, selectedSpaceId: String?): Space? {
        if (spaces.isEmpty()) return null
        val preferred = selectedSpaceId
            ?: _state.value.selectedSpace?.id
        return spaces.firstOrNull { it.id == preferred } ?: spaces.first()
    }

    private fun subscribeForSpace(spaceId: String?) {
        clearSubscriptions()
        if (spaceId.isNullOrBlank()) return
        subscribeExpenses(spaceId)
        subscribeMembers(spaceId)
    }

    private fun subscribeExpenses(spaceId: String) {
        val channel = supabase.realtime.channel("expenses-$spaceId")
        expensesChannel = channel
        expensesCollectJob = scope.launch {
            channel
                .postgresChangeFlow<PostgresAction.Insert>("public") {
                    table = "expenses"
                }
                .collect { payload ->
                    val item = runCatching { payload.decodeRecord<Expense>() }.getOrNull() ?: return@collect
                    val current = _state.value
                    if (current.selectedSpace?.id != spaceId) return@collect
                    _state.value = current.copy(expenses = current.expenses + item)
                }
        }
        scope.launch { channel.subscribe() }
    }

    private fun subscribeMembers(spaceId: String) {
        val channel = supabase.realtime.channel("members-$spaceId")
        membersChannel = channel
        membersCollectJob = scope.launch {
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
        scope.launch { channel.subscribe() }
    }

    private fun clearSubscriptions() {
        expensesCollectJob?.cancel()
        membersCollectJob?.cancel()
        expensesCollectJob = null
        membersCollectJob = null
        scope.launch {
            expensesChannel?.unsubscribe()
            membersChannel?.unsubscribe()
            expensesChannel = null
            membersChannel = null
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

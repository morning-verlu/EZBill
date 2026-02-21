package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.di.AppGraph
import org.kukro.ezbill.domain.usecase.SessionUseCases
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.models.SpaceMember

class HomeScreenModel(
    private val sessionUseCases: SessionUseCases = AppGraph.sessionUseCases
) : ScreenModel {
    var state by mutableStateOf(HomeState())
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Idle)
        private set

    init {
        sessionUseCases.start()
        observeSessionState()
    }

    private fun observeSessionState() {
        screenModelScope.launch {
            try {
                sessionUseCases.sessionState.collect { appState ->
                val hasRenderableData =
                    appState.profile != null ||
                            appState.selectedSpace != null ||
                            appState.members.isNotEmpty() ||
                            appState.expenses.isNotEmpty()
                val isSessionBootstrapping =
                    (appState.status is AppSessionStatus.Initializing ||
                            appState.status is AppSessionStatus.Loading) &&
                            appState.currentUserId != null &&
                            !hasRenderableData
                state = state.copy(
                    currentUserId = appState.currentUserId,
                    isAnonymousUser = appState.isAnonymousUser,
                    profile = appState.profile ?: Profile(),
                    spaceList = appState.spaces,
                    space = appState.selectedSpace ?: Space(id = "", code = ""),
                    memberProfiles = appState.memberProfiles,
                    spaceMembers = appState.members,
                    expenses = appState.expenses,
                    expenseParticipantIds = appState.expenseParticipantIds,
                    isDataLoading = isSessionBootstrapping
                )
                println(
                    "HomeScreenModel.observeSessionState " +
                            "status=${statusName(appState.status)}, " +
                            "currentUserId=${appState.currentUserId}, " +
                            "profileNull=${appState.profile == null}, " +
                            "spaces=${appState.spaces.size}, " +
                            "expenses=${appState.expenses.size}, " +
                            "isDataLoading=${state.isDataLoading}, " +
                            "uiState=${uiState::class.simpleName}, " +
                            "isAvatarUploading=${state.isAvatarUploading}"
                )
            }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("HomeScreenModel.observeSessionState failed message=${e.message}")
            }
        }
    }

    private suspend fun emitSnackBar(msg: String) {
        _snackBar.emit(msg)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ToggleSpace -> handleToggleSpace(intent.space)
            HomeIntent.RefreshCurrentSpace -> handleRefreshCurrentSpace()
            is HomeIntent.SubmitNewSpace -> handleSubmitNewSpace(
                newSpaceName = intent.newSpaceName,
                displayName = intent.displayName
            )
            is HomeIntent.SubmitJoinSpace -> handleSubmitJoinSpace(
                code = intent.code,
                displayName = intent.displayName
            )
        }
    }

    fun onMenuExpanded(expanded: Boolean) {
        state = state.copy(menuExpanded = expanded)
    }

    fun onSpaceListExpanded(expanded: Boolean) {
        state = state.copy(spaceListExpanded = expanded)
    }

    fun onToggleSpace(space: Space) {
        onIntent(HomeIntent.ToggleSpace(space))
    }

    fun onShowCreateDialog(show: Boolean) {
        state = state.copy(showCreateSpaceDialog = show)
        if (!show) state = state.copy(newSpaceName = "")
    }

    fun onShowJoinDialog(show: Boolean) {
        state = state.copy(showJoinSpaceDialog = show)
        if (!show) state = state.copy(joinSpaceCode = "")
    }

    fun onNewSpaceNameChange(value: String) {
        state = state.copy(newSpaceName = value)
    }

    fun onDisplayNameChange(value: String) {
        state = state.copy(displayName = value)
    }

    fun onJoinSpaceCodeChange(value: String) {
        state = state.copy(joinSpaceCode = value)
    }

    fun onDismissJoinDialog() {
        state = state.copy(showJoinSpaceDialog = false, joinSpaceCode = "", displayName = "")
    }

    fun onDismissCreateDialog() {
        state = state.copy(showCreateSpaceDialog = false, newSpaceName = "", displayName = "")
    }

    fun onExpandedFabButtons(show: Boolean) {
        state = state.copy(expandedFabButtons = show)
    }

    fun refreshExpenses() {
        onIntent(HomeIntent.RefreshCurrentSpace)
    }

    private fun setLoading() {
        uiState = HomeUiState.Loading
        println("HomeScreenModel.setLoading uiState=Loading")
    }

    private suspend fun setError(msg: String) {
        emitSnackBar(msg)
    }

    private fun setIdle() {
        uiState = HomeUiState.Idle
        println("HomeScreenModel.setIdle uiState=Idle")
    }

    fun submitNewSpace(
        newSpaceName: String = state.newSpaceName,
        displayName: String = state.displayName
    ) {
        onIntent(
            HomeIntent.SubmitNewSpace(
                newSpaceName = newSpaceName,
                displayName = displayName
            )
        )
    }

    private fun handleSubmitNewSpace(
        newSpaceName: String,
        displayName: String
    ) {
        screenModelScope.launch {
            setLoading()
            try {
                val finalDisplayName = displayName.trim().ifBlank { state.profile.username }
                sessionUseCases.createSpace(newSpaceName, finalDisplayName)
                emitSnackBar("创建成功")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(e.message.toString())
            } finally {
                setIdle()
            }
        }
    }

    fun submitJoinSpace(
        code: String = state.joinSpaceCode,
        displayName: String = state.displayName
    ) {
        onIntent(
            HomeIntent.SubmitJoinSpace(
                code = code,
                displayName = displayName
            )
        )
    }

    private fun handleSubmitJoinSpace(
        code: String,
        displayName: String
    ) {
        screenModelScope.launch {
            setLoading()
            try {
                if (code.isBlank()) {
                    emitSnackBar("分享码不能为空")
                    return@launch
                }
                val finalDisplayName = displayName.trim().ifBlank { state.profile.username }
                sessionUseCases.joinSpace(code, finalDisplayName)
                emitSnackBar("加入成功")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(e.message.toString())
            } finally {
                setIdle()
            }
        }
    }

    private fun handleToggleSpace(space: Space) {
        state = state.copy(space = space)
        if (space.id.isBlank()) return
        screenModelScope.launch {
            setLoading()
            try {
                sessionUseCases.switchSpace(space)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(e.message.toString())
            } finally {
                setIdle()
            }
        }
    }

    private fun handleRefreshCurrentSpace() {
        val currentSpace = state.space
        if (currentSpace.id.isBlank()) return
        if (state.isPullRefreshing) return
        screenModelScope.launch {
            state = state.copy(isPullRefreshing = true)
            try {
                sessionUseCases.refreshCurrentSpace()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(e.message.toString())
            } finally {
                state = state.copy(isPullRefreshing = false)
            }
        }
    }

    suspend fun updateUsernameOnly(username: String): Profile {
        sessionUseCases.updateUsernameOnly(username)
        return state.profile
    }

    suspend fun updateProfileWithNewAvatar(
        username: String,
        imageBytes: ByteArray
    ): Profile {
        sessionUseCases.updateProfileWithNewAvatar(username, imageBytes)
        return state.profile
    }
}

private fun statusName(status: AppSessionStatus): String = when (status) {
    is AppSessionStatus.Initializing -> "Initializing"
    is AppSessionStatus.Loading -> "Loading"
    is AppSessionStatus.Ready -> "Ready"
    is AppSessionStatus.Unauthenticated -> "Unauthenticated"
    is AppSessionStatus.Error -> "Error(${status.message})"
}

sealed interface HomeIntent {
    data class ToggleSpace(val space: Space) : HomeIntent
    data object RefreshCurrentSpace : HomeIntent
    data class SubmitNewSpace(
        val newSpaceName: String,
        val displayName: String
    ) : HomeIntent
    data class SubmitJoinSpace(
        val code: String,
        val displayName: String
    ) : HomeIntent
}

data class HomeState(
    val currentUserId: String? = null,
    val isAnonymousUser: Boolean = false,
    var profile: Profile = Profile(),
    val menuExpanded: Boolean = false,
    val spaceListExpanded: Boolean = false,
    val showJoinSpaceDialog: Boolean = false,
    val joinSpaceCode: String = "",
    val showCreateSpaceDialog: Boolean = false,
    val newSpaceName: String = "",
    val displayName: String = "",
    val space: Space = Space(id = "", code = ""),
    val spaceList: List<Space> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val expenseParticipantIds: Map<String, List<String>> = emptyMap(),
    val expandedFabButtons: Boolean = false,
    val memberProfiles: Map<String, Profile> = emptyMap(),
    val spaceMembers: List<SpaceMember> = emptyList(),
    val isAvatarUploading: Boolean = false,
    val isDataLoading: Boolean = false,
    val isPullRefreshing: Boolean = false
)

sealed class HomeUiState {
    data object Idle : HomeUiState()
    data object Loading : HomeUiState()
    data class Error(val msg: String) : HomeUiState()
    data class Success(val msg: String) : HomeUiState()
}

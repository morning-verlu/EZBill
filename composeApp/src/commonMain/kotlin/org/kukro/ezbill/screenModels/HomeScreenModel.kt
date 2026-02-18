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
import org.kukro.ezbill.AppSessionStore
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.models.SpaceMember

class HomeScreenModel : ScreenModel {
    var state by mutableStateOf(HomeState())
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Idle)
        private set

    init {
        AppSessionStore.start()
        observeSessionState()
    }

    private fun observeSessionState() {
        screenModelScope.launch {
            try {
                AppSessionStore.state.collect { appState ->
                val isSessionBootstrapping = when (appState.status) {
                    is AppSessionStatus.Initializing -> true
                    is AppSessionStatus.Loading -> appState.currentUserId != null && appState.profile == null
                    else -> false
                }
                state = state.copy(
                    currentUserId = appState.currentUserId,
                    isAnonymousUser = appState.isAnonymousUser,
                    profile = appState.profile ?: Profile(),
                    spaceList = appState.spaces,
                    space = appState.selectedSpace ?: Space(id = "", code = ""),
                    memberProfiles = appState.memberProfiles,
                    spaceMembers = appState.members,
                    expenses = appState.expenses,
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

    fun onMenuExpanded(expanded: Boolean) {
        state = state.copy(menuExpanded = expanded)
    }

    fun onSpaceListExpanded(expanded: Boolean) {
        state = state.copy(spaceListExpanded = expanded)
    }

    fun onToggleSpace(space: Space) {
        state = state.copy(space = space)
        if (space.id.isBlank()) return
        screenModelScope.launch {
            setLoading()
            try {
                AppSessionStore.switchSpace(space)
            } catch (e: Exception) {
                setError(e.message.toString())
            } finally {
                setIdle()
            }
        }
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

    suspend fun submitNewSpace(
        newSpaceName: String = state.newSpaceName,
        displayName: String = state.displayName
    ) {
        setLoading()
        try {
            val finalDisplayName = displayName.trim().ifBlank { state.profile.username }
            AppSessionStore.createSpace(newSpaceName, finalDisplayName)
            emitSnackBar("创建成功")
        } catch (e: Exception) {
            setError(e.message.toString())
        } finally {
            setIdle()
        }
    }

    suspend fun submitJoinSpace(
        code: String = state.joinSpaceCode,
        displayName: String = state.displayName
    ) {
        setLoading()
        try {
            if (code.isBlank()) {
                emitSnackBar("分享码不能为空")
                return
            }
            val finalDisplayName = displayName.trim().ifBlank { state.profile.username }
            AppSessionStore.joinSpace(code, finalDisplayName)
            emitSnackBar("加入成功")
        } catch (e: Exception) {
            setError(e.message.toString())
        } finally {
            setIdle()
        }
    }

    suspend fun updateUsernameOnly(username: String): Profile {
        AppSessionStore.updateUsernameOnly(username)
        return state.profile
    }

    suspend fun updateAvatarOnly(imageBytes: ByteArray): Profile {
        state = state.copy(isAvatarUploading = true)
        emitSnackBar("开始上传头像...")
        return try {
            AppSessionStore.updateAvatarOnly(imageBytes)
            emitSnackBar("头像更新成功")
            state.profile
        } catch (e: Exception) {
            emitSnackBar("头像更新失败: ${e.message ?: "unknown error"}")
            state.profile
        } finally {
            state = state.copy(isAvatarUploading = false)
        }
    }

    suspend fun updateProfileWithNewAvatar(
        username: String,
        imageBytes: ByteArray
    ): Profile {
        AppSessionStore.updateProfileWithNewAvatar(username, imageBytes)
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
    val expandedFabButtons: Boolean = false,
    val memberProfiles: Map<String, Profile> = emptyMap(),
    val spaceMembers: List<SpaceMember> = emptyList(),
    val isAvatarUploading: Boolean = false,
    val isDataLoading: Boolean = false
)

sealed class HomeUiState {
    data object Idle : HomeUiState()
    data object Loading : HomeUiState()
    data class Error(val msg: String) : HomeUiState()
    data class Success(val msg: String) : HomeUiState()
}

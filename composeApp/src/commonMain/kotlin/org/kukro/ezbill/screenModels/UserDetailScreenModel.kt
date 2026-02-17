package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.AppSessionStore
import org.kukro.ezbill.SupabaseService
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Profile

class UserDetailScreenModel : ScreenModel {
    var state by mutableStateOf(UserDetailState())
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    init {
        AppSessionStore.start()
        observeSessionState()
    }

    private fun observeSessionState() {
        screenModelScope.launch {
            AppSessionStore.state.collect { appState ->
                state = state.copy(
                    currentUserId = appState.currentUserId,
                    profile = appState.profile ?: Profile(),
                    memberProfiles = appState.memberProfiles,
                    isDataLoading = appState.status is AppSessionStatus.Initializing
                            || appState.status is AppSessionStatus.Loading
                )
            }
        }
    }

    private suspend fun emitSnackBar(msg: String) {
        _snackBar.emit(msg)
    }

    fun onShowUpdateUserDialog(value: Boolean) {
        state = state.copy(showUpdateUserDialog = value)
    }

    fun onShowTopLoading(value: Boolean) {
        state = state.copy(showTopLoading = value)
    }

    fun onUpdateUserInputEmailChange(email: String) {
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                email = email
            )
        )
    }

    fun onUpdateUserInputPasswordChange(password: String) {
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                password = password
            )
        )
    }

    fun onUpdateUserInputConfirmPasswordChange(confirmPassword: String) {
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                confirmPassword = confirmPassword
            )
        )
    }

    fun onDismissUpdateUserDialog() {
        onShowUpdateUserDialog(false)
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                email = "",
                password = "",
                confirmPassword = ""
            ),
        )
    }

    fun updateUser() {
        val uEmail = state.updateUserInput.email
        val uPassword = state.updateUserInput.password
        if (uEmail.isEmpty() || uPassword.isEmpty()) return
        try {
            onShowTopLoading(true)
            screenModelScope.launch {
                SupabaseService.updateUser(uEmail, uPassword)
                onShowTopLoading(false)
                emitSnackBar("update successfully")
            }
        } catch (e: Exception) {
            println("updateUser" + e.message)
            onShowTopLoading(false)
            screenModelScope.launch {
                emitSnackBar("update failed")
            }
        }

    }

}

data class UserDetailState(
    val currentUserId: String? = null,
    var profile: Profile = Profile(),
    val expandedFabButtons: Boolean = false,
    val memberProfiles: Map<String, Profile> = emptyMap(),
    val isAvatarUploading: Boolean = false,
    val isDataLoading: Boolean = false,
    val showUpdateUserDialog: Boolean = false,
    val updateUserInput: UpdateUserInput = UpdateUserInput(
        email = "", password = "", confirmPassword = ""
    ),
    val showTopLoading: Boolean = false
)

data class UpdateUserInput(
    val email: String,
    val password: String,
    val confirmPassword: String
)
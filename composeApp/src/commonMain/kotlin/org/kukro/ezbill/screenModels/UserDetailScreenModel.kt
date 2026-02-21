package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.di.AppGraph
import org.kukro.ezbill.domain.usecase.AccountUseCases
import org.kukro.ezbill.domain.usecase.SessionUseCases
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Profile

class UserDetailScreenModel(
    private val sessionUseCases: SessionUseCases = AppGraph.sessionUseCases,
    private val accountUseCases: AccountUseCases = AppGraph.accountUseCases
) : ScreenModel {
    var state by mutableStateOf(UserDetailState())
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()
    private val maxAvatarBytes = 5 * 1024 * 1024

    init {
        sessionUseCases.start()
        observeSessionState()
    }

    private fun observeSessionState() {
        screenModelScope.launch {
            sessionUseCases.sessionState.collect { appState ->
                state = state.copy(
                    currentUserId = appState.currentUserId,
                    currentUserEmail = appState.currentUserEmail,
                    isAnonymousUser = appState.isAnonymousUser,
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
            ),
            emailError = null
        )
    }

    fun onUpdateUserInputPasswordChange(password: String) {
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                password = password
            ),
            passwordError = null,
            confirmPasswordError = null
        )
    }

    fun onUpdateUserInputConfirmPasswordChange(confirmPassword: String) {
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                confirmPassword = confirmPassword
            ),
            confirmPasswordError = null
        )
    }

    fun onTogglePasswordVisible() {
        state = state.copy(showPasswordVisible = !state.showPasswordVisible)
    }

    fun onToggleConfirmPasswordVisible() {
        state = state.copy(showConfirmPasswordVisible = !state.showConfirmPasswordVisible)
    }

    fun onDismissUpdateUserDialog() {
        onShowUpdateUserDialog(false)
        state = state.copy(
            updateUserInput = state.updateUserInput.copy(
                email = "",
                password = "",
                confirmPassword = ""
            ),
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            showPasswordVisible = false,
            showConfirmPasswordVisible = false
        )
    }

    fun updateUser(email: String, password: String, confirmPassword: String) {
        if (!validateUpdateInput(email, password, confirmPassword)) {
            screenModelScope.launch {
                emitSnackBar("请先修正输入错误")
            }
            return
        }
        val uEmail = email.trim()
        val uPassword = password

        onDismissUpdateUserDialog()

        screenModelScope.launch {
            onShowTopLoading(true)
            emitSnackBar("开始升级账户...")
            try {
                accountUseCases.upgradeAccount(uEmail, uPassword)
                emitSnackBar("升级成功，请检查邮箱完成验证")
            } catch (e: AuthRestException) {
                val msg = when (e.errorCode) {
                    AuthErrorCode.EmailExists,
                    AuthErrorCode.UserAlreadyExists,
                    AuthErrorCode.Conflict -> "该邮箱已绑定，请更换邮箱"
                    else -> "升级失败: ${e.message ?: "unknown error"}"
                }
                emitSnackBar(msg)
            } catch (e: Exception) {
                val raw = e.message.orEmpty().lowercase()
                val msg = if ("email_exists" in raw) {
                    "该邮箱已绑定，请更换邮箱"
                } else {
                    "升级失败: ${e.message ?: "unknown error"}"
                }
                emitSnackBar(msg)
            } finally {
                onShowTopLoading(false)
            }
        }
    }

    fun submitPickedAvatar(imageBytes: ByteArray?) {
        if (imageBytes == null) return
        if (imageBytes.size > maxAvatarBytes) {
            screenModelScope.launch {
                emitSnackBar("不能大于5MB")
            }
            return
        }
        updateAvatarOnly(imageBytes)
    }

    fun signOut() {
        screenModelScope.launch {
            onShowTopLoading(true)
            try {
                sessionUseCases.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitSnackBar("退出失败: ${e.message ?: "unknown error"}")
            } finally {
                onShowTopLoading(false)
            }
        }
    }

    private fun validateUpdateInput(
        rawEmail: String,
        password: String,
        confirm: String
    ): Boolean {
        val email = rawEmail.trim()

        var emailError: String? = null
        var passwordError: String? = null
        var confirmError: String? = null

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (email.isBlank()) {
            emailError = "请输入邮箱地址"
        } else if (!emailRegex.matches(email)) {
            emailError = "邮箱格式不正确"
        }

        val minPasswordLength = 8
        val maxPasswordLength = 64
        if (password.isBlank()) {
            passwordError = "请输入密码"
        } else if (password.length < minPasswordLength) {
            passwordError = "密码至少 8 位"
        } else if (password.length > maxPasswordLength) {
            passwordError = "密码不能超过 64 位"
        } else if (password.any { it.isWhitespace() }) {
            passwordError = "密码不能包含空格"
        } else {
            val hasLetter = password.any { it.isLetter() }
            val hasDigit = password.any { it.isDigit() }
            if (!hasLetter || !hasDigit) {
                passwordError = "密码需包含字母和数字"
            }
        }

        if (confirm.isBlank()) {
            confirmError = "请确认密码"
        } else if (confirm != password) {
            confirmError = "两次输入密码不一致"
        }

        state = state.copy(
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmError
        )

        return emailError == null && passwordError == null && confirmError == null
    }

    fun updateAvatarOnly(imageBytes: ByteArray) {
        if (state.isAvatarUploading) return
        screenModelScope.launch {
            state = state.copy(isAvatarUploading = true)
            emitSnackBar("开始上传头像...")
            try {
                sessionUseCases.updateAvatarOnly(imageBytes)
                emitSnackBar("头像更新成功")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitSnackBar("头像更新失败: ${e.message ?: "unknown error"}")
            } finally {
                state = state.copy(isAvatarUploading = false)
            }
        }
    }

}

data class UserDetailState(
    val currentUserId: String? = null,
    val currentUserEmail: String? = null,
    val isAnonymousUser: Boolean = false,
    var profile: Profile = Profile(),
    val expandedFabButtons: Boolean = false,
    val memberProfiles: Map<String, Profile> = emptyMap(),
    val isAvatarUploading: Boolean = false,
    val isDataLoading: Boolean = false,
    val showUpdateUserDialog: Boolean = false,
    val updateUserInput: UpdateUserInput = UpdateUserInput(
        email = "", password = "", confirmPassword = ""
    ),
    val showTopLoading: Boolean = false,
    val showPasswordVisible: Boolean = false,
    val showConfirmPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

data class UpdateUserInput(
    val email: String,
    val password: String,
    val confirmPassword: String
)

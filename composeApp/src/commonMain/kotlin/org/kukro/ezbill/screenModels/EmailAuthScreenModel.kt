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
import org.kukro.ezbill.AppSessionStore

class EmailAuthScreenModel : ScreenModel {
    var state by mutableStateOf(EmailAuthState())
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun onEmailChange(value: String) {
        state = state.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        state = state.copy(password = value)
    }

    fun signIn() {
        submitWithEmailCredentials { email, password ->
            AppSessionStore.signInWithEmail(email, password)
        }
    }

    fun signUp() {
        submitWithEmailCredentials { email, password ->
            AppSessionStore.signUpWithEmail(email, password)
        }
    }

    fun chooseAnonymous() {
        submitAction {
            AppSessionStore.chooseAnonymous()
        }
    }

    private fun submitWithEmailCredentials(
        action: suspend (email: String, password: String) -> Unit
    ) {
        if (state.loading) return
        val email = state.email.trim()
        val password = state.password
        validateInput(email, password)?.let { msg ->
            screenModelScope.launch {
                emitSnackBar(msg)
            }
            return
        }
        submitAction {
            action(email, password)
        }
    }

    private fun submitAction(action: suspend () -> Unit) {
        if (state.loading) return
        screenModelScope.launch {
            state = state.copy(loading = true)
            try {
                action()
                emitSnackBar("操作成功")
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthRestException) {
                val msg = when (e.errorCode) {
                    AuthErrorCode.InvalidCredentials -> "邮箱或密码错误"
                    AuthErrorCode.EmailExists -> "该邮箱已注册，请直接登录"
                    AuthErrorCode.EmailNotConfirmed -> "邮箱未验证，请先查收邮件"
                    else -> "操作失败: ${e.message ?: "unknown error"}"
                }
                emitSnackBar(msg)
            } catch (e: Exception) {
                emitSnackBar("操作失败: ${e.message ?: "unknown error"}")
            } finally {
                state = state.copy(loading = false)
            }
        }
    }

    private fun validateInput(email: String, password: String): String? {
        if (!emailRegex.matches(email)) return "请输入正确邮箱"
        if (password.length < 8) return "密码至少8位"
        return null
    }

    private suspend fun emitSnackBar(msg: String) {
        _snackBar.emit(msg)
    }
}

data class EmailAuthState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false
)


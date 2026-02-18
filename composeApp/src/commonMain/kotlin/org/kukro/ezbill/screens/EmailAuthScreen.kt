package org.kukro.ezbill.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.launch
import org.kukro.ezbill.AppSessionStore
import org.kukro.ezbill.LocalSnackBarHostState

class EmailAuthScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val hostState = LocalSnackBarHostState.current

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }

        val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }

        fun validateInput(): Boolean {
            val trimmedEmail = email.trim()
            if (!emailRegex.matches(trimmedEmail)) {
                scope.launch { hostState.showSnackbar("请输入正确邮箱") }
                return false
            }
            if (password.length < 8) {
                scope.launch { hostState.showSnackbar("密码至少8位") }
                return false
            }
            return true
        }

        fun handleAuth(action: suspend () -> Unit) {
            if (!validateInput()) return
            loading = true
            scope.launch {
                try {
                    action()
                    hostState.showSnackbar("操作成功")
                } catch (e: AuthRestException) {
                    val msg = when (e.errorCode) {
                        AuthErrorCode.InvalidCredentials -> "邮箱或密码错误"
                        AuthErrorCode.EmailExists -> "该邮箱已注册，请直接登录"
                        AuthErrorCode.EmailNotConfirmed -> "邮箱未验证，请先查收邮件"
                        else -> "操作失败: ${e.message ?: "unknown error"}"
                    }
                    hostState.showSnackbar(msg)
                } catch (e: Exception) {
                    hostState.showSnackbar("操作失败: ${e.message ?: "unknown error"}")
                } finally {
                    loading = false
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "邮箱登录",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        handleAuth {
                            AppSessionStore.signInWithEmail(email.trim(), password)
                        }
                    },
                    enabled = !loading
                ) {
                    Text("登录")
                }
                Button(
                    onClick = {
                        handleAuth {
                            AppSessionStore.signUpWithEmail(email.trim(), password)
                        }
                    },
                    enabled = !loading
                ) {
                    Text("注册")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { navigator.pop() },
                    enabled = !loading
                ) {
                    Text("返回")
                }
                TextButton(
                    onClick = {
                        handleAuth {
                            AppSessionStore.chooseAnonymous()
                        }
                    },
                    enabled = !loading
                ) {
                    Text("改用匿名")
                }
            }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }
        }
    }
}

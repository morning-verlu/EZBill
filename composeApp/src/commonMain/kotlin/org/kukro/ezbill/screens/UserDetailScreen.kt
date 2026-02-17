package org.kukro.ezbill.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.screenModels.UserDetailScreenModel

class UserDetailScreen(
    private val userId: String
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userDetailScreenModel = rememberScreenModel { UserDetailScreenModel() }
        val hostState = LocalSnackBarHostState.current

        LaunchedEffect(Unit) {
            userDetailScreenModel.snackBar.collect { msg ->
                hostState.showSnackbar(msg)
            }
        }

        Scaffold(
            bottomBar = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {}) {
                        Text("隐私权政策")
                    }
                    Text("·")
                    TextButton(onClick = {}) {
                        Text("服务条款")
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedVisibility(userDetailScreenModel.state.showTopLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "user.email ?: empty",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = userDetailScreenModel.state.profile.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp + 24.dp).clip(CircleShape)
                            .clickable(onClick = {

//                                        scope.launch {
//                                            val bytes = picker.pickImageBytes()
//                                            println("picked bytes = ${bytes?.size}")
//                                            bytes?.size?.let {
//                                                if (it > 5 * 1024 * 1024) {
//                                                    hostState.showSnackbar("不能大于5MB")
//                                                    return@launch
//                                                }
//                                            }
//                                            bytes?.let {
//                                                homeScreenModel.updateAvatarOnly(it)
//                                            }
//                                        }
                            })
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "${userDetailScreenModel.state.profile.username},您好!",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        userDetailScreenModel.onShowUpdateUserDialog(true)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Upgrade, "upgrade")
                    Text("升级为正式账户")
                }

                if (userDetailScreenModel.state.showUpdateUserDialog) {
                    UpdateUserDialog(userDetailScreenModel)
                }
            }
        }
    }
}

@Composable
private fun UpdateUserDialog(userDetailScreenModel: UserDetailScreenModel) {
    Dialog(
        onDismissRequest = {
            userDetailScreenModel.onShowUpdateUserDialog(false)
        }
    ) {
        Card {
            Column {
                Text(
                    "Update Account",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                TextField(
                    value = userDetailScreenModel.state.updateUserInput.email,
                    onValueChange = { userDetailScreenModel.onUpdateUserInputEmailChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("请输入邮箱地址") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                TextField(
                    value = userDetailScreenModel.state.updateUserInput.password,
                    onValueChange = { userDetailScreenModel.onUpdateUserInputPasswordChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("请输入密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                TextField(
                    value = userDetailScreenModel.state.updateUserInput.confirmPassword,
                    onValueChange = {
                        userDetailScreenModel.onUpdateUserInputConfirmPasswordChange(
                            it
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("请确认密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    TextButton(onClick = {
                        userDetailScreenModel.onDismissUpdateUserDialog()
                    }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = {
                        userDetailScreenModel.updateUser()
                        userDetailScreenModel.onDismissUpdateUserDialog()
                    }) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

package org.kukro.ezbill.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ezbill.composeapp.generated.resources.Res
import ezbill.composeapp.generated.resources.ezbill_logo
import org.jetbrains.compose.resources.painterResource
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.screenModels.AuthChoiceScreenModel

class AuthChoiceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authChoiceScreenModel = rememberScreenModel { AuthChoiceScreenModel() }
        val hostState = LocalSnackBarHostState.current
        val loading = authChoiceScreenModel.loading

        LaunchedEffect(Unit) {
            authChoiceScreenModel.snackBar.collect { msg ->
                hostState.showSnackbar(msg)
            }
        }

        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (loading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(Res.drawable.ezbill_logo),
                    contentDescription = "EzBill",
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                )

                Spacer(Modifier.height(20.dp))


                Spacer(Modifier.height(24.dp))

                TextButton(
                    onClick = authChoiceScreenModel::chooseAnonymous,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("继续匿名使用")
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { navigator.push(EmailAuthScreen()) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("邮箱登录 / 注册")
                }
            }
        }
    }
}

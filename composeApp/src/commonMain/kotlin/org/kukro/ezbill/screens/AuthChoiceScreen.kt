package org.kukro.ezbill.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "选择登录方式",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "首次选择后会记住你的偏好",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Button(
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

            if (loading) {
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator()
            }
        }
    }
}

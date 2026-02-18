package org.kukro.ezbill

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.AuthPreference
import org.kukro.ezbill.screens.AuthChoiceScreen
import org.kukro.ezbill.screens.EmailAuthScreen
import org.kukro.ezbill.screens.HomeScreen
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.ui.theme.EzBillTheme

@Composable
fun App() {
    EzBillTheme{
        val hostState = remember { SnackbarHostState() }
        val homeScreen = remember { HomeScreen() }
        val authChoiceScreen = remember { AuthChoiceScreen() }
        val emailAuthScreen = remember { EmailAuthScreen() }
        LaunchedEffect(Unit) {
            AppSessionStore.start()
        }
        val appState by AppSessionStore.state.collectAsState()

        LaunchedEffect(appState.status, appState.preferredAuthMethod) {
            if (appState.status is AppSessionStatus.Unauthenticated &&
                appState.preferredAuthMethod == AuthPreference.ANONYMOUS
            ) {
                runCatching { AppSessionStore.chooseAnonymous() }
            }
        }

        CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState) }
            ) {
                val rootScreen = when {
                    appState.currentUserId != null -> {
                        homeScreen
                    }

                    appState.status is AppSessionStatus.Initializing ||
                            appState.status is AppSessionStatus.Loading -> {
                        homeScreen
                    }

                    appState.status is AppSessionStatus.Unauthenticated &&
                            appState.preferredAuthMethod == AuthPreference.EMAIL -> {
                        emailAuthScreen
                    }

                    else -> {
                        authChoiceScreen
                    }
                }
                Navigator(rootScreen) {
                    SlideTransition(it)
                }
            }
        }
    }
}

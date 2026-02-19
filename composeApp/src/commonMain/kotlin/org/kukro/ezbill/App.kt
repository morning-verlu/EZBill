package org.kukro.ezbill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.AuthPreference
import org.kukro.ezbill.screens.AuthChoiceScreen
import org.kukro.ezbill.screens.EmailAuthScreen
import org.kukro.ezbill.screens.HomeScreen
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.ui.theme.EzBillTheme

private enum class RootRoute {
    LOADING,
    HOME,
    AUTH_CHOICE,
    EMAIL_AUTH
}

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
                appState.hasChosenAuthMethod &&
                appState.preferredAuthMethod == AuthPreference.ANONYMOUS
            ) {
                runCatching { AppSessionStore.chooseAnonymous() }
            }
        }

        CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState) }
            ) { innerPadding ->
                val rootRoute = when {
                    appState.status is AppSessionStatus.Unauthenticated &&
                            appState.preferredAuthMethod == AuthPreference.EMAIL -> {
                        RootRoute.EMAIL_AUTH
                    }

                    appState.status is AppSessionStatus.Unauthenticated -> {
                        RootRoute.AUTH_CHOICE
                    }

                    appState.currentUserId != null -> {
                        RootRoute.HOME
                    }

                    appState.status is AppSessionStatus.Initializing ||
                            appState.status is AppSessionStatus.Loading -> {
                        RootRoute.LOADING
                    }

                    else -> {
                        RootRoute.AUTH_CHOICE
                    }
                }
                val rootScreen = when (rootRoute) {
                    RootRoute.LOADING -> null
                    RootRoute.HOME -> homeScreen
                    RootRoute.AUTH_CHOICE -> authChoiceScreen
                    RootRoute.EMAIL_AUTH -> emailAuthScreen
                }
                key(rootRoute) {
                    if (rootScreen == null) {
                        RootLoadingScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        Navigator(rootScreen) {
                            SlideTransition(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

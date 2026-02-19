package org.kukro.ezbill

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import ezbill.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.screens.AuthChoiceScreen
import org.kukro.ezbill.screens.HomeScreen
import org.kukro.ezbill.ui.theme.EzBillTheme

private enum class RootRoute {
    LOADING,
    HOME,
    AUTH_CHOICE
}

@Composable
fun App() {
    EzBillTheme {
        val hostState = remember { SnackbarHostState() }
        val homeScreen = remember { HomeScreen() }
        val authChoiceScreen = remember { AuthChoiceScreen() }
        var startupAnimationFinished by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            AppSessionStore.start()
        }
        val appState by AppSessionStore.state.collectAsState()

        CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState) }
            ) { innerPadding ->
                val sessionRoute = when {
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
                val shouldKeepLoading =
                    !startupAnimationFinished || sessionRoute == RootRoute.LOADING
                val rootRoute = if (shouldKeepLoading) RootRoute.LOADING else sessionRoute
                val rootScreen = when (rootRoute) {
                    RootRoute.LOADING -> null
                    RootRoute.HOME -> homeScreen
                    RootRoute.AUTH_CHOICE -> authChoiceScreen
                }
                key(rootRoute) {
                    if (rootScreen == null) {
                        RootLoadingScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            playAnimation = !startupAnimationFinished,
                            onAnimationFinished = { startupAnimationFinished = true }
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

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun RootLoadingScreen(
    modifier: Modifier = Modifier,
    playAnimation: Boolean,
    onAnimationFinished: () -> Unit
) {
    val animationSpeed = 2.0f

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/welcome.json").decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = playAnimation && composition != null,
        iterations = 1,
        speed = animationSpeed
    )

    LaunchedEffect(playAnimation, composition) {
        if (playAnimation && composition == null) {
            delay(1800)
            if (composition == null) {
                onAnimationFinished()
            }
        }
    }
    LaunchedEffect(playAnimation, composition, progress) {
        if (playAnimation && composition != null && progress >= 0.999f) {
            onAnimationFinished()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = { progress }
            ),
            contentDescription = "Welcome animation",
            modifier = Modifier.size(220.dp)
        )
    }
}

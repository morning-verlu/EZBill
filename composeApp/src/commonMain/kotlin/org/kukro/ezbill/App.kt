package org.kukro.ezbill

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import org.kukro.ezbill.app.AppRootIntent
import org.kukro.ezbill.app.RootDestination
import org.kukro.ezbill.di.AppGraph
import org.kukro.ezbill.screens.AuthChoiceScreen
import org.kukro.ezbill.screens.HomeScreen
import org.kukro.ezbill.ui.theme.EzBillTheme

@Composable
fun App() {
    EzBillTheme {
        val hostState = remember { SnackbarHostState() }
        val homeScreen = remember { HomeScreen() }
        val authChoiceScreen = remember { AuthChoiceScreen() }
        val rootStateMachine = remember { AppGraph.rootStateMachine }
        val rootState by rootStateMachine.state.collectAsState()

        LaunchedEffect(Unit) {
            rootStateMachine.dispatch(AppRootIntent.Start)
        }

        CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState) }
            ) { innerPadding ->
                val rootScreen = when (rootState.destination) {
                    RootDestination.LOADING -> null
                    RootDestination.HOME -> homeScreen
                    RootDestination.AUTH_CHOICE -> authChoiceScreen
                }
                key(rootState.destination) {
                    if (rootScreen == null) {
                        RootLoadingScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            playAnimation = rootState.playStartupAnimation,
                            onAnimationFinished = {
                                rootStateMachine.dispatch(AppRootIntent.StartupAnimationFinished)
                            }
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
    if (!playAnimation) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
        modifier = modifier.fillMaxSize(),
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

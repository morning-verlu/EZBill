package org.kukro.ezbill

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.kukro.ezbill.screens.HomeScreen
import org.kukro.ezbill.screens.LocalSnackBarHostState

@Composable
fun App() {

    val hostState = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState) }
        ) {
            Navigator(HomeScreen()) {
                SlideTransition(it)
            }
        }
    }


}
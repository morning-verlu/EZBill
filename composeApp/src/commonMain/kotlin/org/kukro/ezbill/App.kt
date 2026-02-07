package org.kukro.ezbill

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.kukro.ezbill.screens.HomeScreen

@Composable
fun App() {
    Navigator(HomeScreen()) {
        SlideTransition(it)
    }
}
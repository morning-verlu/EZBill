package org.kukro.ezbill.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackBarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackBarHostState not provided")
}
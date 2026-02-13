package org.kukro.ezbill

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
val LocalSnackBarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackBarHostState not provided")
}
package org.kukro.ezbill.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.domain.usecase.AppLifecycleEvent
import org.kukro.ezbill.domain.usecase.SessionUseCases
import org.kukro.ezbill.models.AppSessionStatus

enum class RootDestination {
    LOADING,
    HOME,
    AUTH_CHOICE
}

data class AppRootState(
    val destination: RootDestination = RootDestination.LOADING,
    val playStartupAnimation: Boolean = true
)

sealed interface AppRootIntent {
    data object Start : AppRootIntent
    data object StartupAnimationFinished : AppRootIntent
    data object AppForeground : AppRootIntent
    data object AppBackground : AppRootIntent
}

class AppRootStateMachine(
    private val sessionUseCases: SessionUseCases
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(AppRootState())
    val state = _state.asStateFlow()

    private var started = false
    private var observeJob: Job? = null
    private var startupAnimationFinished = false
    private var latestSessionDestination = RootDestination.LOADING
    private var lastStableDestination: RootDestination? = null

    fun dispatch(intent: AppRootIntent) {
        when (intent) {
            AppRootIntent.Start -> startIfNeeded()
            AppRootIntent.StartupAnimationFinished -> {
                startupAnimationFinished = true
                updateState()
            }
            AppRootIntent.AppForeground -> {
                startIfNeeded()
                sessionUseCases.handleLifecycle(AppLifecycleEvent.Foreground)
            }
            AppRootIntent.AppBackground -> {
                sessionUseCases.handleLifecycle(AppLifecycleEvent.Background)
            }
        }
    }

    private fun startIfNeeded() {
        if (started) return
        started = true
        sessionUseCases.start()

        observeJob?.cancel()
        observeJob = scope.launch {
            sessionUseCases.sessionState.collect { appState ->
                latestSessionDestination = when {
                    appState.status is AppSessionStatus.Unauthenticated -> RootDestination.AUTH_CHOICE
                    appState.currentUserId != null -> RootDestination.HOME
                    appState.status is AppSessionStatus.Initializing ||
                            appState.status is AppSessionStatus.Loading -> RootDestination.LOADING
                    else -> RootDestination.LOADING
                }
                if (latestSessionDestination != RootDestination.LOADING) {
                    lastStableDestination = latestSessionDestination
                }
                updateState()
            }
        }
    }

    private fun updateState() {
        val destination = when {
            !startupAnimationFinished -> RootDestination.LOADING
            latestSessionDestination != RootDestination.LOADING -> latestSessionDestination
            lastStableDestination != null -> lastStableDestination!!
            else -> RootDestination.LOADING
        }
        _state.value = AppRootState(
            destination = destination,
            playStartupAnimation = !startupAnimationFinished
        )
    }
}


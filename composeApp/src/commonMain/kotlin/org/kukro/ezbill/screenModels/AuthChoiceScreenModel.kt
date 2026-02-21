package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.di.AppGraph
import org.kukro.ezbill.domain.usecase.SessionUseCases

class AuthChoiceScreenModel(
    private val sessionUseCases: SessionUseCases = AppGraph.sessionUseCases
) : ScreenModel {
    var state by mutableStateOf(AuthChoiceState())
        private set
    val loading: Boolean
        get() = state.loading

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    fun onIntent(intent: AuthChoiceIntent) {
        when (intent) {
            AuthChoiceIntent.ChooseAnonymous -> chooseAnonymous()
        }
    }

    fun chooseAnonymous() {
        if (loading) return
        screenModelScope.launch {
            state = state.copy(loading = true)
            try {
                sessionUseCases.chooseAnonymous()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitSnackBar("匿名登录失败: ${e.message ?: "unknown error"}")
            } finally {
                state = state.copy(loading = false)
            }
        }
    }

    private suspend fun emitSnackBar(msg: String) {
        _snackBar.emit(msg)
    }
}

sealed interface AuthChoiceIntent {
    data object ChooseAnonymous : AuthChoiceIntent
}

data class AuthChoiceState(
    val loading: Boolean = false
)

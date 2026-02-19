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
import org.kukro.ezbill.AppSessionStore

class AuthChoiceScreenModel : ScreenModel {
    var loading by mutableStateOf(false)
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    fun chooseAnonymous() {
        if (loading) return
        screenModelScope.launch {
            loading = true
            try {
                AppSessionStore.chooseAnonymous()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitSnackBar("匿名登录失败: ${e.message ?: "unknown error"}")
            } finally {
                loading = false
            }
        }
    }

    private suspend fun emitSnackBar(msg: String) {
        _snackBar.emit(msg)
    }
}


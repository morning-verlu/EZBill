package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.kukro.ezbill.SupabaseService
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense

class EditExpenseScreenModel(
    val spaceId: String, val userId: String
) : ScreenModel {
    var state by mutableStateOf<EditExpenseUIState>(EditExpenseUIState.Idle)
        private set

    var expenseAmountText by mutableStateOf("")
    var selectedPayerId: String? by mutableStateOf(userId)
    var createdId by mutableStateOf("")
    private var job: Job? = null

    var expenseData by mutableStateOf(
        CreateExpenseData(
            spaceId = spaceId,
            payerId = userId,
            amount = 0.0,
            note = "",
            createdBy = userId
        )
    )
        private set

    fun onSelectPayerId(id: String) {
        selectedPayerId = id
        expenseData = expenseData.copy(
            payerId = id, createdBy = id
        )
    }

    fun onAmountChange(value: String) {
        expenseAmountText = value
        val parsed = value.toDoubleOrNull()
        if (parsed != null) {
            expenseData = expenseData.copy(amount = parsed)
        }
    }

    fun onNoteChange(value: String) {
        expenseData = expenseData.copy(note = value)
    }

    fun onDismissLoadingDialog() {
        state = EditExpenseUIState.Idle
        job?.cancel()
    }

    suspend fun createExpense(
        expenseData: CreateExpenseData
    ): Expense {
        return SupabaseService.createExpense(expenseData)
    }

    fun submit(onDone: () -> Unit) {
        if (state is EditExpenseUIState.Loading) return
        state = EditExpenseUIState.Loading
        job?.cancel()
        job = screenModelScope.launch {
            val amountValue = expenseData.amount
            if (amountValue <= 0) {
                state = EditExpenseUIState.Error("金额不合法")
                return@launch
            }

            try {
                createExpense(expenseData)
                state = EditExpenseUIState.Success("创建成功")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("create expense failed,${e.message}")
                state = EditExpenseUIState.Error(e.message ?: "创建失败")
            } finally {
                if (state is EditExpenseUIState.Loading) {
                    state = EditExpenseUIState.Idle
                }
            }
        }
    }
}

sealed class EditExpenseUIState {
    object Idle : EditExpenseUIState()
    object Loading : EditExpenseUIState()
    data class Success(val msg: String) : EditExpenseUIState()
    data class Error(val msg: String) : EditExpenseUIState()
}

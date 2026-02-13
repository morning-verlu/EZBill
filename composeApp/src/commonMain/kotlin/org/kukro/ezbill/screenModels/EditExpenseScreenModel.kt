package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense

class EditExpenseScreenModel(
    val spaceId: String, val payerId: String
) : ScreenModel {
    var state by mutableStateOf<EditExpenseUIState>(EditExpenseUIState.Idle)
        private set

    var expenseAmountText by mutableStateOf("")

    var expenseData by mutableStateOf(
        CreateExpenseData(
            spaceId = spaceId, payerId = payerId, amount = 0.0, note = "", createdBy = payerId
        )
    )
        private set

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

    suspend fun createExpense(
        expenseData: CreateExpenseData
    ): Expense {
        val result = supabase.postgrest["expenses"].insert(expenseData) {
            select()
        }

        return result.decodeList<Expense>().first()
    }

    fun submit(onDone: () -> Unit) {
        screenModelScope.launch {
            val amountValue = expenseData.amount
            if (amountValue <= 0) {
                state = EditExpenseUIState.Error("金额不合法")
                return@launch
            }

            state = EditExpenseUIState.Loading
            try {
                createExpense(expenseData)
                state = EditExpenseUIState.Success("创建成功")
                onDone()
            } catch (e: Throwable) {
                state = EditExpenseUIState.Error(e.message ?: "创建失败")
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


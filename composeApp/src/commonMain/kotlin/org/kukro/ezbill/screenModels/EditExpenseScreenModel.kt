package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.kukro.ezbill.di.AppGraph
import org.kukro.ezbill.domain.usecase.ExpenseUseCases
import org.kukro.ezbill.domain.usecase.SessionUseCases
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.SpaceMember

class EditExpenseScreenModel(
    val spaceId: String,
    val userId: String,
    private val expenseUseCases: ExpenseUseCases = AppGraph.expenseUseCases,
    private val sessionUseCases: SessionUseCases = AppGraph.sessionUseCases
) : ScreenModel {
    companion object {
        private const val MAX_EXPENSE_AMOUNT = 999999.99
        private const val MAX_EXPENSE_AMOUNT_LABEL = "999999.99"
        private val AMOUNT_INPUT_REGEX = Regex("^\\d{0,6}(\\.\\d{0,2})?$")
    }

    var state by mutableStateOf<EditExpenseUIState>(EditExpenseUIState.Idle)
        private set

    var spaceMembers by mutableStateOf<List<SpaceMember>>(emptyList())
        private set

    var expenseAmountText by mutableStateOf("")
    var amountInputError by mutableStateOf<String?>(null)
        private set
    var selectedPayerId: String? by mutableStateOf(userId)
    var selectedParticipantIds by mutableStateOf<Set<String>>(emptySet())
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

    private var participantsInitialized = false

    init {
        sessionUseCases.start()
        syncMembersFromSession(sessionUseCases.sessionState.value)
        screenModelScope.launch {
            sessionUseCases.sessionState.collect { syncMembersFromSession(it) }
        }
    }

    private fun syncMembersFromSession(appState: AppSessionState) {
        if (appState.selectedSpace?.id != spaceId) return
        val members = appState.members
        spaceMembers = members
        val ids = members
            .map { it.userId.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (!participantsInitialized && ids.isNotEmpty()) {
            selectedParticipantIds = ids.toSet()
            participantsInitialized = true
            if (userId !in ids) {
                selectedPayerId = ids.first()
                expenseData = expenseData.copy(payerId = ids.first())
            } else {
                selectedPayerId = userId
                expenseData = expenseData.copy(payerId = userId)
            }
        }
    }

    fun onSelectPayerId(id: String) {
        selectedPayerId = id
        expenseData = expenseData.copy(
            payerId = id,
            createdBy = userId
        )
    }

    fun onToggleParticipant(userId: String) {
        selectedParticipantIds = if (selectedParticipantIds.contains(userId)) {
            selectedParticipantIds - userId
        } else {
            selectedParticipantIds + userId
        }
    }

    fun onAmountChange(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            expenseAmountText = ""
            amountInputError = null
            expenseData = expenseData.copy(amount = 0.0)
            return
        }

        if (!AMOUNT_INPUT_REGEX.matches(normalized)) {
            amountInputError = "金额格式不正确（最多 6 位整数 + 2 位小数）"
            return
        }

        val parsed = normalized.toDoubleOrNull()
        if (parsed != null && parsed > MAX_EXPENSE_AMOUNT) {
            amountInputError = "金额不能超过 ¥$MAX_EXPENSE_AMOUNT_LABEL"
            return
        }

        expenseAmountText = normalized
        amountInputError = null
        expenseData = expenseData.copy(amount = parsed ?: 0.0)
    }

    fun onNoteChange(value: String) {
        expenseData = expenseData.copy(note = value)
    }

    fun onDismissLoadingDialog() {
        state = EditExpenseUIState.Idle
        job?.cancel()
    }

    suspend fun createExpense(expenseData: CreateExpenseData): Expense {
        return expenseUseCases.createExpense(
            expenseData = expenseData,
            participantUserIds = selectedParticipantIds.toList()
        )
    }

    fun submit(onDone: () -> Unit) {
        if (state is EditExpenseUIState.Loading) return
        state = EditExpenseUIState.Loading
        job?.cancel()
        job = screenModelScope.launch {
            val amountValue = expenseData.amount
            if (!amountInputError.isNullOrBlank()) {
                state = EditExpenseUIState.Error(amountInputError ?: "金额不合法")
                return@launch
            }
            if (amountValue <= 0) {
                state = EditExpenseUIState.Error("金额不合法")
                return@launch
            }
            if (amountValue > MAX_EXPENSE_AMOUNT) {
                state = EditExpenseUIState.Error("金额不能超过 ¥$MAX_EXPENSE_AMOUNT_LABEL")
                return@launch
            }
            if (selectedParticipantIds.isEmpty()) {
                state = EditExpenseUIState.Error("请至少选择一位参与成员")
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

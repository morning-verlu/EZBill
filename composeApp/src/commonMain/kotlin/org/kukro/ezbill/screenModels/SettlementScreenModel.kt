package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.ExpenseShareRow
import org.kukro.ezbill.models.SettlementInsertRow
import org.kukro.ezbill.models.SettlementItemInsertRow
import org.kukro.ezbill.models.SettlementRow
import org.kukro.ezbill.models.SpaceMember
import kotlin.math.round

class SettlementScreenModel(
    private val spaceId: String,
    private val members: List<SpaceMember>
) : ScreenModel {

    var uiState by mutableStateOf<SettlementUiState>(SettlementUiState.Loading)
        private set

    var note by mutableStateOf("")
        private set

    fun onNoteChange(value: String) {
        note = value
    }

    fun recalculate() {
        screenModelScope.launch {
            uiState = SettlementUiState.Loading
            try {
                val expenses = fetchExpenses(spaceId)
                val shares = fetchSharesByExpenseIds(expenses.map { it.id })
                val preview = buildPreview(members, expenses, shares)
                uiState = SettlementUiState.Success(preview)
            } catch (e: Throwable) {
                uiState = SettlementUiState.Error(e.message ?: "结算计算失败")
            }
        }
    }

    fun save(onDone: () -> Unit) {
        screenModelScope.launch {
            val success = uiState as? SettlementUiState.Success
            if (success == null) {
                uiState = SettlementUiState.Error("没有可保存的结算结果")
                return@launch
            }

            try {
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("当前用户未登录")

                val settlementRow = supabase.postgrest["settlements"].insert(
                    SettlementInsertRow(
                        spaceId = spaceId,
                        createdBy = currentUserId,
                        note = note.ifBlank { null },
                        status = "open",
                        rangeStart = null,
                        rangeEnd = null
                    )
                ) {
                    select()
                }.decodeList<SettlementRow>().first()

                val transferRows = success.preview.transfers.map {
                    SettlementItemInsertRow(
                        settlementId = settlementRow.id,
                        fromUserId = it.fromUserId,
                        toUserId = it.toUserId,
                        amount = it.amount
                    )
                }

                if (transferRows.isNotEmpty()) {
                    supabase.postgrest["settlement_items"].insert(transferRows)
                }

                uiState = SettlementUiState.Success(success.preview, "结算保存成功")
                onDone()
            } catch (e: Throwable) {
                uiState = SettlementUiState.Error(e.message ?: "保存结算失败")
            }
        }
    }

    private suspend fun fetchExpenses(spaceId: String): List<Expense> {
        return supabase.postgrest["expenses"].select {
            filter { eq("space_id", spaceId) }
        }.decodeList()
    }

    private suspend fun fetchSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow> {
        if (expenseIds.isEmpty()) return emptyList()
        return supabase.postgrest["expense_shares"].select {
            filter { isIn("expense_id", expenseIds) }
        }.decodeList()
    }

    private fun buildPreview(
        members: List<SpaceMember>,
        expenses: List<Expense>,
        shares: List<ExpenseShareRow>
    ): SettlementPreview {
        fun round2(v: Double): Double = round(v * 100.0) / 100.0

        val nameMap = members.associate { m ->
            m.userId to (m.displayName?.takeIf { it.isNotBlank() } ?: m.userId.take(6))
        }

        val paidMap = mutableMapOf<String, Double>()
        expenses.forEach { e ->
            paidMap[e.payerId] = (paidMap[e.payerId] ?: 0.0) + e.amount
        }

        val shareMap = mutableMapOf<String, Double>()
        val sharesByExpenseId = shares.groupBy { it.expenseId }
        val memberIds = members.map { it.userId }

        expenses.forEach { expense ->
            val shareRows = sharesByExpenseId[expense.id].orEmpty()
            if (shareRows.isNotEmpty()) {
                shareRows.forEach { row ->
                    shareMap[row.userId] = (shareMap[row.userId] ?: 0.0) + row.shareAmount
                }
            } else if (memberIds.isNotEmpty()) {
                val split = expense.amount / memberIds.size
                memberIds.forEach { uid ->
                    shareMap[uid] = (shareMap[uid] ?: 0.0) + split
                }
            }
        }

        val userIds = (members.map { it.userId } + paidMap.keys + shareMap.keys).toSet()
        val nets = userIds.map { uid ->
            val paid = round2(paidMap[uid] ?: 0.0)
            val share = round2(shareMap[uid] ?: 0.0)
            val net = round2(paid - share)
            UserNet(
                userId = uid,
                displayName = nameMap[uid],
                paid = paid,
                share = share,
                net = net
            )
        }.sortedByDescending { it.net }

        val creditors = nets.filter { it.net > 0.0 }
            .map { it.userId to it.net }
            .toMutableList()

        val debtors = nets.filter { it.net < 0.0 }
            .map { it.userId to -it.net }
            .toMutableList()

        val transfers = mutableListOf<TransferSuggestion>()
        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val (debtorId, debtLeft) = debtors[i]
            val (creditorId, creditLeft) = creditors[j]

            val pay = round2(minOf(debtLeft, creditLeft))
            if (pay > 0.0) {
                transfers += TransferSuggestion(
                    fromUserId = debtorId,
                    fromName = nameMap[debtorId],
                    toUserId = creditorId,
                    toName = nameMap[creditorId],
                    amount = pay
                )
            }

            val newDebt = round2(debtLeft - pay)
            val newCredit = round2(creditLeft - pay)

            debtors[i] = debtorId to newDebt
            creditors[j] = creditorId to newCredit

            if (newDebt == 0.0) i++
            if (newCredit == 0.0) j++
        }

        return SettlementPreview(
            nets = nets,
            transfers = transfers
        )
    }

}

sealed class SettlementUiState {
    object Loading : SettlementUiState()
    data class Error(val msg: String) : SettlementUiState()
    data class Success(val preview: SettlementPreview, val msg: String? = null) :
        SettlementUiState()
}

data class SettlementPreview(
    val nets: List<UserNet>,
    val transfers: List<TransferSuggestion>
)

data class UserNet(
    val userId: String,
    val displayName: String?,
    val paid: Double,
    val share: Double,
    val net: Double
)

data class TransferSuggestion(
    val fromUserId: String,
    val fromName: String?,
    val toUserId: String,
    val toName: String?,
    val amount: Double
)


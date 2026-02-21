package org.kukro.ezbill.data.datasource

import org.kukro.ezbill.SupabaseService
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.ExpenseShareRow
import org.kukro.ezbill.models.SettlementRow
import org.kukro.ezbill.models.SettlementTransferInput

object SupabaseExpenseDataSource : ExpenseDataSource {
    override suspend fun createExpense(expenseData: CreateExpenseData, participantUserIds: List<String>): Expense {
        return SupabaseService.createExpense(
            expenseData = expenseData,
            participantUserIds = participantUserIds
        )
    }

    override suspend fun fetchExpensesBySpace(spaceId: String): List<Expense> {
        return SupabaseService.fetchExpensesBySpace(spaceId)
    }

    override suspend fun fetchExpenseSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow> {
        return SupabaseService.fetchExpenseSharesByExpenseIds(expenseIds)
    }

    override suspend fun saveSettlement(
        spaceId: String,
        note: String?,
        transfers: List<SettlementTransferInput>
    ): SettlementRow {
        return SupabaseService.saveSettlement(spaceId, note, transfers)
    }
}


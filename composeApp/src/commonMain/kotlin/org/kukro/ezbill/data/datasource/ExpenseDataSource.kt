package org.kukro.ezbill.data.datasource

import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.ExpenseShareRow
import org.kukro.ezbill.models.SettlementRow
import org.kukro.ezbill.models.SettlementTransferInput

interface ExpenseDataSource {
    suspend fun createExpense(expenseData: CreateExpenseData, participantUserIds: List<String>): Expense
    suspend fun fetchExpensesBySpace(spaceId: String): List<Expense>
    suspend fun fetchExpenseSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow>
    suspend fun saveSettlement(spaceId: String, note: String?, transfers: List<SettlementTransferInput>): SettlementRow
}


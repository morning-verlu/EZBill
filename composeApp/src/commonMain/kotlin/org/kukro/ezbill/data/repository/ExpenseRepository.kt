package org.kukro.ezbill.data.repository

import org.kukro.ezbill.data.datasource.ExpenseDataSource
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.ExpenseShareRow
import org.kukro.ezbill.models.SettlementRow
import org.kukro.ezbill.models.SettlementTransferInput

interface ExpenseRepository {
    suspend fun createExpense(expenseData: CreateExpenseData, participantUserIds: List<String>): Expense
    suspend fun fetchExpensesBySpace(spaceId: String): List<Expense>
    suspend fun fetchExpenseSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow>
    suspend fun saveSettlement(spaceId: String, note: String?, transfers: List<SettlementTransferInput>): SettlementRow
}

class DefaultExpenseRepository(
    private val dataSource: ExpenseDataSource
) : ExpenseRepository {
    override suspend fun createExpense(expenseData: CreateExpenseData, participantUserIds: List<String>): Expense {
        return dataSource.createExpense(expenseData, participantUserIds)
    }

    override suspend fun fetchExpensesBySpace(spaceId: String): List<Expense> {
        return dataSource.fetchExpensesBySpace(spaceId)
    }

    override suspend fun fetchExpenseSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow> {
        return dataSource.fetchExpenseSharesByExpenseIds(expenseIds)
    }

    override suspend fun saveSettlement(
        spaceId: String,
        note: String?,
        transfers: List<SettlementTransferInput>
    ): SettlementRow {
        return dataSource.saveSettlement(spaceId, note, transfers)
    }
}


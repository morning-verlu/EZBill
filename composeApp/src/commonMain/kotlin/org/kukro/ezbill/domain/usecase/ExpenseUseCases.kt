package org.kukro.ezbill.domain.usecase

import org.kukro.ezbill.data.repository.ExpenseRepository
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.ExpenseShareRow
import org.kukro.ezbill.models.SettlementRow
import org.kukro.ezbill.models.SettlementTransferInput

class ExpenseUseCases(
    private val expenseRepository: ExpenseRepository
) {
    suspend fun createExpense(
        expenseData: CreateExpenseData,
        participantUserIds: List<String>
    ): Expense {
        return expenseRepository.createExpense(expenseData, participantUserIds)
    }

    suspend fun fetchExpensesBySpace(spaceId: String): List<Expense> {
        return expenseRepository.fetchExpensesBySpace(spaceId)
    }

    suspend fun fetchExpenseSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow> {
        return expenseRepository.fetchExpenseSharesByExpenseIds(expenseIds)
    }

    suspend fun saveSettlement(
        spaceId: String,
        note: String?,
        transfers: List<SettlementTransferInput>
    ): SettlementRow {
        return expenseRepository.saveSettlement(spaceId, note, transfers)
    }
}


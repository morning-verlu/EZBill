package org.kukro.ezbill.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    @SerialName("space_id") val spaceId: String,
    @SerialName("payer_id") val payerId: String,
    val amount: Double,
    val note: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class CreateExpenseData(
    @SerialName("space_id") val spaceId: String,
    @SerialName("payer_id") val payerId: String,
    val amount: Double,
    val note: String?,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class ExpenseShareInsertRow(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("share_amount") val shareAmount: Double
)

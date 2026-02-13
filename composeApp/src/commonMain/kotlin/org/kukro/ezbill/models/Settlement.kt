package org.kukro.ezbill.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseShareRow(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("share_amount") val shareAmount: Double
)

@Serializable
data class SettlementInsertRow(
    @SerialName("space_id") val spaceId: String,
    @SerialName("created_by") val createdBy: String,
    val note: String? = null,
    val status: String = "open",
    @SerialName("range_start") val rangeStart: String? = null,
    @SerialName("range_end") val rangeEnd: String? = null
)

@Serializable
data class SettlementRow(
    val id: String
)

@Serializable
data class SettlementItemInsertRow(
    @SerialName("settlement_id") val settlementId: String,
    @SerialName("from_user_id") val fromUserId: String,
    @SerialName("to_user_id") val toUserId: String,
    val amount: Double
)


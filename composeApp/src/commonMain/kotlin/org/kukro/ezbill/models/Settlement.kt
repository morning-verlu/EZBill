package org.kukro.ezbill.models

import kotlinx.serialization.Serializable

@Serializable
data class SettlementResult(
    val settlement: Settlement,
    val items: List<SettlementItem>
)

@Serializable
data class Settlement(
    val spaceId: String,
    val note: String?,
    val rangeStart: String?,
    val rangeEnd: String?
)

@Serializable
data class SettlementItem(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)

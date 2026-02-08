package org.kukro.ezbill.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Space(
    val id: String,
    val code: String,
    val name: String? = null,
    @SerialName("owner_id")
    val ownerId: String? = null,
    val enabled: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null
)


@Serializable
data class MembershipWithSpace(
    @SerialName("space_id") val spaceId: String,
    val spaces: Space
)
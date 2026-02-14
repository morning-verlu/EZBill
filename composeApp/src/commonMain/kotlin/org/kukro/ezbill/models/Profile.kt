package org.kukro.ezbill.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.kukro.ezbill.AppConfig

@Serializable
data class Profile(
    @SerialName("user_id")
    val userId: String = "",
    val username: String = "未命名用户",
    @SerialName("avatar_url")
    var avatarUrl: String = AppConfig.DEFAULT_AVATAR,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

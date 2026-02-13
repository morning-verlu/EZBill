package org.kukro.ezbill.models

import kotlinx.serialization.Serializable
import org.kukro.ezbill.AppConfig

@Serializable
data class UserInfo(var username: String, var avatar: String = AppConfig.DEFAULT_AVATAR)



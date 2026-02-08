package org.kukro.ezbill.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(var username: String?, var avatar: String = "https://suibian.s3.bitiful.net/avatar_fox.png")

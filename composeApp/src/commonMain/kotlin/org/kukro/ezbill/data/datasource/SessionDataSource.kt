package org.kukro.ezbill.data.datasource

import kotlinx.coroutines.flow.StateFlow
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.Space

interface SessionDataSource {
    val state: StateFlow<AppSessionState>

    fun start()
    fun onAppForeground()
    fun onAppBackground()

    suspend fun chooseAnonymous()
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun signOut(): Result<Unit>
    suspend fun bootstrapAuthenticated(selectedSpaceId: String? = null, fallbackSelectedSpace: Space? = null)

    suspend fun switchSpace(space: Space)
    suspend fun createSpace(name: String, displayName: String)
    suspend fun joinSpace(code: String, displayName: String)

    suspend fun updateAvatarOnly(imageBytes: ByteArray)
    suspend fun updateUsernameOnly(username: String)
    suspend fun updateProfileWithNewAvatar(username: String, imageBytes: ByteArray)
}

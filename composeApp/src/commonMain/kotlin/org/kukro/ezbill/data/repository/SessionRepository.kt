package org.kukro.ezbill.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.kukro.ezbill.data.datasource.SessionDataSource
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.Space

interface SessionRepository {
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

class DefaultSessionRepository(
    private val dataSource: SessionDataSource
) : SessionRepository {
    override val state: StateFlow<AppSessionState> = dataSource.state

    override fun start() = dataSource.start()
    override fun onAppForeground() = dataSource.onAppForeground()
    override fun onAppBackground() = dataSource.onAppBackground()

    override suspend fun chooseAnonymous() = dataSource.chooseAnonymous()
    override suspend fun signInWithEmail(email: String, password: String) = dataSource.signInWithEmail(email, password)
    override suspend fun signUpWithEmail(email: String, password: String) = dataSource.signUpWithEmail(email, password)
    override suspend fun signOut(): Result<Unit> = dataSource.signOut()
    override suspend fun bootstrapAuthenticated(selectedSpaceId: String?, fallbackSelectedSpace: Space?) {
        dataSource.bootstrapAuthenticated(selectedSpaceId, fallbackSelectedSpace)
    }

    override suspend fun switchSpace(space: Space) = dataSource.switchSpace(space)
    override suspend fun createSpace(name: String, displayName: String) = dataSource.createSpace(name, displayName)
    override suspend fun joinSpace(code: String, displayName: String) = dataSource.joinSpace(code, displayName)

    override suspend fun updateAvatarOnly(imageBytes: ByteArray) = dataSource.updateAvatarOnly(imageBytes)
    override suspend fun updateUsernameOnly(username: String) = dataSource.updateUsernameOnly(username)
    override suspend fun updateProfileWithNewAvatar(username: String, imageBytes: ByteArray) {
        dataSource.updateProfileWithNewAvatar(username, imageBytes)
    }
}

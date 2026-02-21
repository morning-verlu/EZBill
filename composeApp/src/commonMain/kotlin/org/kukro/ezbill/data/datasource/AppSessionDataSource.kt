package org.kukro.ezbill.data.datasource

import kotlinx.coroutines.flow.StateFlow
import org.kukro.ezbill.AppSessionStore
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.Space

object AppSessionDataSource : SessionDataSource {
    override val state: StateFlow<AppSessionState> = AppSessionStore.state

    override fun start() = AppSessionStore.start()

    override fun onAppForeground() = AppSessionStore.onAppForeground()

    override fun onAppBackground() = AppSessionStore.onAppBackground()

    override suspend fun chooseAnonymous() = AppSessionStore.chooseAnonymous()

    override suspend fun signInWithEmail(email: String, password: String) {
        AppSessionStore.signInWithEmail(email, password)
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        AppSessionStore.signUpWithEmail(email, password)
    }

    override suspend fun signOut() = AppSessionStore.signOut()

    override suspend fun bootstrapAuthenticated(selectedSpaceId: String?, fallbackSelectedSpace: Space?) {
        AppSessionStore.bootstrapAuthenticated(
            selectedSpaceId = selectedSpaceId,
            fallbackSelectedSpace = fallbackSelectedSpace
        )
    }

    override suspend fun switchSpace(space: Space) = AppSessionStore.switchSpace(space)

    override suspend fun createSpace(name: String, displayName: String) {
        AppSessionStore.createSpace(name, displayName)
    }

    override suspend fun joinSpace(code: String, displayName: String) {
        AppSessionStore.joinSpace(code, displayName)
    }

    override suspend fun updateAvatarOnly(imageBytes: ByteArray) = AppSessionStore.updateAvatarOnly(imageBytes)

    override suspend fun updateUsernameOnly(username: String) = AppSessionStore.updateUsernameOnly(username)

    override suspend fun updateProfileWithNewAvatar(username: String, imageBytes: ByteArray) {
        AppSessionStore.updateProfileWithNewAvatar(username, imageBytes)
    }
}


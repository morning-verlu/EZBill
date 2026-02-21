package org.kukro.ezbill.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.kukro.ezbill.models.AppSessionState
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.data.repository.SessionRepository

enum class AppLifecycleEvent {
    Foreground,
    Background
}

class SessionUseCases(
    private val sessionRepository: SessionRepository
) {
    val sessionState: StateFlow<AppSessionState> = sessionRepository.state

    fun start() {
        sessionRepository.start()
    }

    fun handleLifecycle(event: AppLifecycleEvent) {
        when (event) {
            AppLifecycleEvent.Foreground -> sessionRepository.onAppForeground()
            AppLifecycleEvent.Background -> sessionRepository.onAppBackground()
        }
    }

    suspend fun chooseAnonymous() = sessionRepository.chooseAnonymous()

    suspend fun signInWithEmail(email: String, password: String) {
        sessionRepository.signInWithEmail(email, password)
    }

    suspend fun signUpWithEmail(email: String, password: String) {
        sessionRepository.signUpWithEmail(email, password)
    }

    suspend fun signOut() = sessionRepository.signOut()

    suspend fun bootstrapAuthenticated(
        selectedSpaceId: String? = null,
        fallbackSelectedSpace: Space? = null
    ) {
        sessionRepository.bootstrapAuthenticated(
            selectedSpaceId = selectedSpaceId,
            fallbackSelectedSpace = fallbackSelectedSpace
        )
    }

    suspend fun switchSpace(space: Space) = sessionRepository.switchSpace(space)

    suspend fun refreshCurrentSpace() {
        val current = sessionState.value.selectedSpace ?: return
        if (current.id.isBlank()) return
        sessionRepository.switchSpace(current)
    }

    suspend fun createSpace(name: String, displayName: String) {
        sessionRepository.createSpace(name, displayName)
    }

    suspend fun joinSpace(code: String, displayName: String) {
        sessionRepository.joinSpace(code, displayName)
    }

    suspend fun updateAvatarOnly(imageBytes: ByteArray) {
        sessionRepository.updateAvatarOnly(imageBytes)
    }

    suspend fun updateUsernameOnly(username: String) {
        sessionRepository.updateUsernameOnly(username)
    }

    suspend fun updateProfileWithNewAvatar(username: String, imageBytes: ByteArray) {
        sessionRepository.updateProfileWithNewAvatar(username, imageBytes)
    }
}


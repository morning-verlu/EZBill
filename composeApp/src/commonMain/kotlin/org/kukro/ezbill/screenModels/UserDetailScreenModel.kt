package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.kukro.ezbill.AppSessionStore
import org.kukro.ezbill.models.AppSessionStatus
import org.kukro.ezbill.models.Profile

class UserDetailScreenModel : ScreenModel {
    var state by mutableStateOf(UserDetailState())
        private set

    init {
        AppSessionStore.start()
        observeSessionState()
    }

    private fun observeSessionState() {
        screenModelScope.launch {
            AppSessionStore.state.collect { appState ->
                state = state.copy(
                    currentUserId = appState.currentUserId,
                    profile = appState.profile ?: Profile(),
                    memberProfiles = appState.memberProfiles,
                    isDataLoading = appState.status is AppSessionStatus.Initializing
                            || appState.status is AppSessionStatus.Loading
                )
            }
        }
    }
}

data class UserDetailState(
    val currentUserId: String? = null,
    var profile: Profile = Profile(),
    val expandedFabButtons: Boolean = false,
    val memberProfiles: Map<String, Profile> = emptyMap(),
    val isAvatarUploading: Boolean = false,
    val isDataLoading: Boolean = false
)
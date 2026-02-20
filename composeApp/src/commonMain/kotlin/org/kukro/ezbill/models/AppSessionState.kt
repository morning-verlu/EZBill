package org.kukro.ezbill.models

sealed interface AppSessionStatus {
    data object Initializing : AppSessionStatus
    data object Unauthenticated : AppSessionStatus
    data object Loading : AppSessionStatus
    data object Ready : AppSessionStatus
    data class Error(val message: String) : AppSessionStatus
}

data class AppSessionState(
    val status: AppSessionStatus = AppSessionStatus.Initializing,
    val currentUserId: String? = null,
    val currentUserEmail: String? = null,
    val isAnonymousUser: Boolean = false,
    val profile: Profile? = null,
    val memberProfiles: Map<String, Profile> = emptyMap(),
    val spaces: List<Space> = emptyList(),
    val selectedSpace: Space? = null,
    val members: List<SpaceMember> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val expenseParticipantIds: Map<String, List<String>> = emptyMap()
)

package org.kukro.ezbill

import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.models.SpaceMember

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
    val profile: Profile? = null,
    val spaces: List<Space> = emptyList(),
    val selectedSpace: Space? = null,
    val members: List<SpaceMember> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

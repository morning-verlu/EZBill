package org.kukro.ezbill.domain.usecase

import org.kukro.ezbill.data.repository.AccountRepository
import org.kukro.ezbill.data.repository.SessionRepository

class AccountUseCases(
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun upgradeAccount(email: String, password: String) {
        accountRepository.updateUser(email, password)
        sessionRepository.bootstrapAuthenticated()
    }
}


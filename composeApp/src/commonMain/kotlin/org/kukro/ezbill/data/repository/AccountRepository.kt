package org.kukro.ezbill.data.repository

import org.kukro.ezbill.data.datasource.AccountDataSource

interface AccountRepository {
    suspend fun updateUser(email: String, password: String)
}

class DefaultAccountRepository(
    private val dataSource: AccountDataSource
) : AccountRepository {
    override suspend fun updateUser(email: String, password: String) {
        dataSource.updateUser(email, password)
    }
}


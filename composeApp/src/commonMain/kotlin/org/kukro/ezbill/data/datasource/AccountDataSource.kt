package org.kukro.ezbill.data.datasource

interface AccountDataSource {
    suspend fun updateUser(email: String, password: String)
}


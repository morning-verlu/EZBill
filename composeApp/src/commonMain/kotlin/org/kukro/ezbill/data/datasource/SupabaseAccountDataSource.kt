package org.kukro.ezbill.data.datasource

import org.kukro.ezbill.SupabaseService

object SupabaseAccountDataSource : AccountDataSource {
    override suspend fun updateUser(email: String, password: String) {
        SupabaseService.updateUser(email, password)
    }
}


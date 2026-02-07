package org.kukro.ezbill

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get

class SupabaseSessionManager(private val settings: Settings) {
    private val sessionKey = "supabase_auth_session"

    fun saveSession(session: String) {
        settings.putString(sessionKey, session)
    }

    fun getSession(): String? {
        return settings.get<String>(sessionKey)
    }

}
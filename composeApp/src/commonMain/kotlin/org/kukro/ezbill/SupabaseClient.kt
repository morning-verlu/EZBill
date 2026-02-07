package org.kukro.ezbill

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

object SupabaseClient {
    val supabase = createSupabaseClient(
        supabaseUrl = AppConfig.SUPABASE_URL,
        supabaseKey = AppConfig.SUPABASE_KEY
    ) {
        install(Auth) {

        }
        install(Postgrest)
        //install other modules
        install(Storage) {
            transferTimeout = 90.seconds // Default: 120 seconds
        }
        install(Realtime) {
            reconnectDelay = 5.seconds // Default: 7 seconds
        }
    }
}
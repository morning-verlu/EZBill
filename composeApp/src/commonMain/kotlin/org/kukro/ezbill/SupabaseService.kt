package org.kukro.ezbill

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.MembershipWithSpace
import org.kukro.ezbill.models.Space
import kotlin.random.Random

object SupabaseService {

    suspend fun createSpace(name: String, displayName: String): Space {
        val result = supabase.postgrest.rpc(
            "create_space_with_owner",
            mapOf("p_name" to name, "p_display_name" to displayName)
        )
        return result.decodeAs<Space>()
    }


    suspend fun fetchMyCreatedSpaces(): List<Space> {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return emptyList()
        println("userId = $userId")

        val result = supabase.postgrest["spaces"]
            .select {
                filter {
                    eq("owner_id", userId)
                }
            }

        return result.decodeList<Space>()
    }

    suspend fun fetchJoinedSpaces(): List<Space> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        println("userId = $userId")

        val result = supabase.postgrest["space_memberships"]
            .select(columns = Columns.raw("space_id, spaces(*)")) {
                filter { eq("user_id", userId) }
            }

        return result.decodeList<MembershipWithSpace>().map { it.spaces }
    }
}

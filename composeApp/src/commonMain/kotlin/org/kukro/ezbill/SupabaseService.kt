package org.kukro.ezbill

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.MembershipWithSpace
import org.kukro.ezbill.models.Space
import kotlin.random.Random

object SupabaseService {

    suspend fun createSpace(
        name: String,
        maxRetry: Int = 5
    ): Space {
        var lastError: Throwable? = null

        repeat(maxRetry) {
            val code = generateSpaceCode()
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                require(!userId.isNullOrBlank())

                val result = supabase.postgrest["spaces"].insert(
                    mapOf(
                        "code" to code,
                        "name" to name,
                        "owner_id" to userId
                    )
                ) {
                    select()
                }

                return result.decodeList<Space>().first()
            } catch (e: Throwable) {
                lastError = e
                // 如果是 code 冲突，继续重试；否则直接抛出
                // 这里简单粗暴：都重试，超过次数再抛
            }
        }

        throw lastError ?: IllegalStateException("Create space failed")
    }

    suspend fun fetchMyCreatedSpaces(): List<Space> {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return emptyList()
        println("userId = $userId")

//        val result = supabase.postgrest["spaces"]
//            .select {
//                filter {
//                    eq("owner_id", userId)
//                }
//            }
        val result = supabase.postgrest["spaces"].select()

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

private fun generateSpaceCode(length: Int = 6): String {
    val safeChars = listOf(
        // 数字：剔除 0、1
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '9',
        // 大写字母：剔除 O、I、L
        'A',
        'B',
        'C',
        'D',
        'E',
        'F',
        'G',
        'H',
        'J',
        'K',
        'M',
        'N',
        'P',
        'Q',
        'R',
        'S',
        'T',
        'U',
        'V',
        'W',
        'X',
        'Y',
        'Z',
        // 小写字母：剔除 o、i、l
        'a',
        'b',
        'c',
        'd',
        'e',
        'f',
        'g',
        'h',
        'j',
        'k',
        'm',
        'n',
        'p',
        'q',
        'r',
        's',
        't',
        'u',
        'v',
        'w',
        'x',
        'y',
        'z'
    )

    return buildString {
        repeat(length) {
            append(safeChars[Random.nextInt(safeChars.size)])
        }
    }
}

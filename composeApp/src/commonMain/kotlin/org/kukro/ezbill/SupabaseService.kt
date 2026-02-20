package org.kukro.ezbill

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.CreateExpenseData
import org.kukro.ezbill.models.Expense
import org.kukro.ezbill.models.ExpenseShareInsertRow
import org.kukro.ezbill.models.ExpenseShareRow
import org.kukro.ezbill.models.MembershipWithSpace
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.models.SettlementInsertRow
import org.kukro.ezbill.models.SettlementItemInsertRow
import org.kukro.ezbill.models.SettlementRow
import org.kukro.ezbill.models.SettlementTransferInput
import org.kukro.ezbill.models.Space
import kotlin.time.Clock
import kotlin.math.roundToLong

object SupabaseService {

    suspend fun createSpace(name: String, displayName: String): Space {
        val result = supabase.postgrest.rpc(
            "create_space_with_owner",
            mapOf("p_name" to name, "p_display_name" to displayName)
        )
        return result.decodeAs<Space>()
    }

    suspend fun createExpense(expenseData: CreateExpenseData): Expense {
        return createExpense(expenseData, participantUserIds = emptyList())
    }

    suspend fun createExpense(
        expenseData: CreateExpenseData,
        participantUserIds: List<String>
    ): Expense {
        val result = supabase.postgrest["expenses"].insert(expenseData) {
            select()
        }
        val expense = result.decodeList<Expense>().first()

        val participantIds = participantUserIds
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (participantIds.isEmpty()) return expense

        val totalCents = (expense.amount * 100.0).roundToLong().toInt()
        val base = totalCents / participantIds.size
        val remainder = totalCents % participantIds.size

        val shareRows = participantIds.mapIndexed { index, userId ->
            val cents = base + if (index < remainder) 1 else 0
            ExpenseShareInsertRow(
                expenseId = expense.id,
                userId = userId,
                shareAmount = cents / 100.0
            )
        }
        supabase.postgrest["expense_shares"].insert(shareRows)
        return expense
    }

    suspend fun fetchExpensesBySpace(spaceId: String): List<Expense> {
        return supabase.postgrest["expenses"].select {
            filter { eq("space_id", spaceId) }
        }.decodeList()
    }

    suspend fun fetchExpenseSharesByExpenseIds(expenseIds: List<String>): List<ExpenseShareRow> {
        if (expenseIds.isEmpty()) return emptyList()
        return supabase.postgrest["expense_shares"].select {
            filter { isIn("expense_id", expenseIds) }
        }.decodeList()
    }

    suspend fun saveSettlement(
        spaceId: String,
        note: String?,
        transfers: List<SettlementTransferInput>
    ): SettlementRow {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("当前用户未登录")

        val settlementRow = supabase.postgrest["settlements"].insert(
            SettlementInsertRow(
                spaceId = spaceId,
                createdBy = currentUserId,
                note = note,
                status = "open",
                rangeStart = null,
                rangeEnd = null
            )
        ) {
            select()
        }.decodeList<SettlementRow>().first()

        val transferRows = transfers.map {
            SettlementItemInsertRow(
                settlementId = settlementRow.id,
                fromUserId = it.fromUserId,
                toUserId = it.toUserId,
                amount = it.amount
            )
        }

        if (transferRows.isNotEmpty()) {
            supabase.postgrest["settlement_items"].insert(transferRows)
        }

        return settlementRow
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

    suspend fun uploadAvatarBytes(imageBytes: ByteArray): String {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Not authenticated")

        val path = "$userId/avatar.jpg"

        supabase.storage.from("avatars").upload(path, imageBytes) {
            upsert = true
            contentType = ContentType("image", "jpeg")
        }

        val publicUrl = supabase.storage.from("avatars").publicUrl(path)

        return "$publicUrl?t=${Clock.System.now().toEpochMilliseconds()}"
    }

    suspend fun saveProfile(
        username: String? = null,
        avatarUrl: String? = null
    ): Profile {
        val authUser = supabase.auth.currentUserOrNull() ?: error("Not authenticated")
        val userId = authUser.id

        val existing = loadMyProfileOrNull()

        val metaUsername = authUser.userMetadata
            ?.get("username")
            ?.toString()
            ?.trim('"')
            .orEmpty()

        val finalUsername = (username ?: existing?.username ?: metaUsername)
            .trim()
            .ifBlank { "user-${userId.take(6)}" }

        val finalAvatarUrl = (avatarUrl ?: existing?.avatarUrl)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: AppConfig.DEFAULT_AVATAR

        supabase.postgrest["profiles"].upsert(
            mapOf(
                "user_id" to userId,
                "username" to finalUsername,
                "avatar_url" to finalAvatarUrl
            )
        ) {
            onConflict = "user_id"
        }

        return loadMyProfileOrNull() ?: Profile(
            userId = userId,
            username = finalUsername,
            avatarUrl = finalAvatarUrl
        )
    }

    suspend fun getOrCreateMyProfile(): Profile {
        val authUser = supabase.auth.currentUserOrNull() ?: error("Not authenticated")
        val existing = loadMyProfileOrNull()
        if (existing != null) return existing

        val metaUsername = authUser.userMetadata
            ?.get("username")
            ?.toString()
            ?.trim('"')
            .orEmpty()

        val metaAvatar = authUser.userMetadata
            ?.get("avatar")
            ?.toString()
            ?.trim('"')
            .orEmpty()

        return saveProfile(
            username = metaUsername.ifBlank { "user-${authUser.id.take(6)}" },
            avatarUrl = metaAvatar.ifBlank { AppConfig.DEFAULT_AVATAR }
        )
    }

    suspend fun loadMyProfileOrNull(): Profile? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        val rows = supabase.postgrest["profiles"].select {
            filter { eq("user_id", userId) }
            limit(1)
        }.decodeList<Profile>()
        return rows.firstOrNull()
    }

    suspend fun updateUser(uEmail: String, uPassword: String) {
        supabase.auth.updateUser {
            email = uEmail
            password = uPassword
        }
    }

}

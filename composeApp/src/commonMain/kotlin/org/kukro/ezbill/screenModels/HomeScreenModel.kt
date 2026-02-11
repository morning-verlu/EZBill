package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.SupabaseService
import org.kukro.ezbill.models.Space
import org.kukro.ezbill.models.UserInfo
import kotlin.random.Random

class HomeScreenModel : ScreenModel {
    var state by mutableStateOf(HomeState())
        private set

    private val _snackBar = MutableSharedFlow<String>()
    val snackBar = _snackBar.asSharedFlow()

    private suspend fun emitSnackBar(msg: String) {
        _snackBar.emit(msg)
    }

    init {
        observeSession()
    }

    private fun observeSession() {
        screenModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        // 等待，不做事
                    }

                    is SessionStatus.Authenticated -> {
                        println("Authenticated")
                        emitSnackBar("login")
                        val user = supabase.auth.currentUserOrNull()
                        state = state.copy(
                            userInfo = state.userInfo.copy(
                                username = user?.userMetadata?.get("username").toString()
                            )
                        )
                        screenModelScope.launch {
                            getAllSpaces()
                        }
                    }

                    is SessionStatus.NotAuthenticated -> {
                        println("NotAuthenticated")
                        emitSnackBar("There's no session,sign in anonymously")
                        val username = generateUsername()
                        supabase.auth.signInAnonymously(
                            data = mapOf(
                                "username" to username,
                                "avatar" to "https://suibian.s3.bitiful.net/avatar_fox.png"
                            )
                        )
                        state = state.copy(
                            userInfo = state.userInfo.copy(
                                username = username
                            )
                        )
                    }

                    is SessionStatus.RefreshFailure -> {

                    }
                }
            }
        }
    }

    fun onMenuExpanded(expanded: Boolean) {
        state = state.copy(menuExpanded = expanded)
    }

    fun onSpaceListExpanded(expanded: Boolean) {
        state = state.copy(spaceListExpanded = expanded)
    }

    fun onToggleSpace(space: Space) {
        state = state.copy(space = space)
    }

    fun onShowCreateDialog(show: Boolean) {
        state = state.copy(showCreateSpaceDialog = show)
        if (!show) state = state.copy(newSpaceName = "")
    }

    fun onShowJoinDialog(show: Boolean) {
        state = state.copy(showJoinSpaceDialog = show)
        if (!show) state = state.copy(joinSpaceCode = "")
    }

    fun onNewSpaceNameChange(value: String) {
        state = state.copy(newSpaceName = value)
    }

    fun onJoinSpaceCodeChange(value: String) {
        state = state.copy(joinSpaceCode = value)
    }

    fun onDismissJoinDialog() {
        state = state.copy(showJoinSpaceDialog = false, joinSpaceCode = "")
    }

    fun onDismissCreateDialog() {
        state = state.copy(showCreateSpaceDialog = false, newSpaceName = "")
    }

    suspend fun submitNewSpace() {
        val space = SupabaseService.createSpace(state.newSpaceName)
        state = state.copy(space = space)
        getAllSpaces()
    }

    suspend fun submitJoinSpace() {
        val code: String = state.joinSpaceCode
        if (code.isEmpty()) {
            println("!!!! join code is empty !!!!")
            return
        }

        val space = supabase.postgrest.rpc(
            "join_space_by_code",
            mapOf(
                "p_code" to code,
                "p_display_name" to null
            )
        ).decodeAs<Space>()

        state = state.copy(spaceList = state.spaceList + listOf(space))
    }


    suspend fun getAllSpaces() {
        val myCreatedList = SupabaseService.fetchMyCreatedSpaces()
        val myJoinedList = SupabaseService.fetchJoinedSpaces()
        val merged = (myCreatedList + myJoinedList)
            .distinctBy { it.id }

        state = if (merged.isNotEmpty()) {
            state.copy(spaceList = merged, space = merged[0])
        } else {
            state.copy(spaceList = emptyList())
        }

    }

}

data class HomeState(
    var userInfo: UserInfo = UserInfo(username = ""),
    val menuExpanded: Boolean = false,
    val spaceListExpanded: Boolean = false,
    val showJoinSpaceDialog: Boolean = false,
    val joinSpaceCode: String = "",
    val showCreateSpaceDialog: Boolean = false,
    val newSpaceName: String = "",
    val space: Space = Space(id = "", code = ""),
    val spaceList: List<Space> = emptyList()
)


private fun generateUsername(): String {
    val adjectives = listOf(
        "大大的", "小小的", "勤奋的", "可爱的", "憨憨的", "聪明的",
        "萌萌的", "酷酷的", "温柔的", "活泼的", "懒懒的", "调皮的",
        "认真的", "开心的", "慢吞吞的", "急匆匆的", "圆滚滚的", "亮晶晶的"
    )

    val nouns = listOf(
        "猕猴桃", "小蜜蜂", "小猫咪", "小狗狗", "小兔子", "小松鼠",
        "小熊猫", "小老虎", "小绵羊", "小鸭子", "小金鱼", "小蜗牛",
        "小草莓", "小西瓜", "小葡萄", "小苹果", "小橘子", "小蘑菇"
    )

    val randomAdjective = adjectives[Random.nextInt(adjectives.size)]
    val randomNoun = nouns[Random.nextInt(nouns.size)]

    return "$randomAdjective$randomNoun"
}


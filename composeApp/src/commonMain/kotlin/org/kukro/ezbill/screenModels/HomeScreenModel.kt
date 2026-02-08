package org.kukro.ezbill.screenModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.auth.status.SessionStatus
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
                        state.userInfo = state.userInfo.copy(
                            username = user?.userMetadata?.get("username").toString()
                        )
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
                        state.userInfo = state.userInfo.copy(username = username)
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

    fun onShowCreateDialog(show: Boolean) {
        state = state.copy(showCreateSpaceDialog = show)
        if (!show) state = state.copy(newSpaceName = "")
    }

    fun onSpaceNameChange(value: String) {
        state = state.copy(newSpaceName = value)
    }

    fun onDismissCreateDialog() {
        state = state.copy(showCreateSpaceDialog = false, newSpaceName = "")
    }

    suspend fun submitNewSpace() {
        val space = SupabaseService.createSpace(state.newSpaceName)
        state = state.copy(space = space)
    }

}

data class HomeState(
    var userInfo: UserInfo = UserInfo(username = ""),
    val menuExpanded: Boolean = false,
    val showCreateSpaceDialog: Boolean = false,
    val newSpaceName: String = "",
    val space: Space = Space(id = "", code = "")
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


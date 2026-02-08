package org.kukro.ezbill.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.models.UserInfo
import kotlin.random.Random

class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var menuExpanded by remember { mutableStateOf(false) }
        val hostState = LocalSnackBarHostState.current
        val scope = rememberCoroutineScope()
        var userInfo: UserInfo by remember { mutableStateOf(UserInfo(username = "")) }


        LaunchedEffect(Unit) {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        // 等待，不做事
                    }

                    is SessionStatus.Authenticated -> {
                        hostState.showSnackbar("Login directly")
                        userInfo = userInfo.copy(
                            username = supabase.auth.currentUserOrNull()?.userMetadata?.get("username")
                                .toString(),
                        )
                    }

                    is SessionStatus.NotAuthenticated -> {
                        hostState.showSnackbar("There's no session, signInAnonymously")
                        supabase.auth.signInAnonymously()
                        val username = generateUsername()
                        supabase.auth.updateUser {
                            data {
                                "username" to username
                                "avatar" to "https://suibian.s3.bitiful.net/avatar_fox.png"
                            }
                        }
                        userInfo = userInfo.copy(username = username)
                    }

                    is SessionStatus.RefreshFailure -> {
                        hostState.showSnackbar("SessionStatus RefreshFailure")
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { /* TODO: Open drawer */ }) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "菜单"
                            )
                        }
                    },
                    actions = {
                        Row {
                            Column {
                                IconButton(onClick = {
                                    menuExpanded = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Create Space") },
                                        onClick = { /* Do something... */ }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Join Space") },
                                        onClick = { /* Do something... */ }
                                    )
                                }
                            }

                            Column {
                                AsyncImage(
                                    model = userInfo.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Column {

                }
            }
        }
    }
}

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
package org.kukro.ezbill.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import org.kukro.ezbill.SupabaseClient.supabase

class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var menuExpanded by remember { mutableStateOf(false) }
        val hostState = LocalSnackBarHostState.current
        val scope = rememberCoroutineScope()

        fun toggleMenuExpanded() {
            menuExpanded = !menuExpanded
        }

        LaunchedEffect(Unit) {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        // 等待，不做事
                    }
                    is SessionStatus.Authenticated -> {
                        hostState.showSnackbar("Login directly")
                    }
                    is SessionStatus.NotAuthenticated -> {
                        hostState.showSnackbar("There's no session, signInAnonymously")
                        supabase.auth.signInAnonymously()
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
                        Column {
                            IconButton(onClick = {
                                toggleMenuExpanded()
                            }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = null)
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

            }
        }
    }
}
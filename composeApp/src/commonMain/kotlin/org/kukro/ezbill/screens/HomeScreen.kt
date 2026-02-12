package org.kukro.ezbill.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import org.kukro.ezbill.SupabaseClient.supabase
import org.kukro.ezbill.screenModels.HomeScreenModel

class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val hostState = LocalSnackBarHostState.current
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        val homeScreenModel = rememberScreenModel { HomeScreenModel() }
        val clipboard = LocalClipboardManager.current
        val navigator = LocalNavigator.current


        LaunchedEffect(Unit) {
            homeScreenModel.snackBar.collect { msg ->
                hostState.showSnackbar(msg)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .clickable { homeScreenModel.onSpaceListExpanded(true) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row {
                                Text(
                                    text = homeScreenModel.state.space.name ?: "Have fun",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (homeScreenModel.state.spaceList.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }
                            Row {
                                if (homeScreenModel.state.space.code.isNotEmpty()) {
                                    Text(
                                        text = homeScreenModel.state.space.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                }

                            }
                        }

                        DropdownMenu(
                            expanded = homeScreenModel.state.spaceListExpanded,
                            onDismissRequest = {
                                homeScreenModel.onSpaceListExpanded(false)
                            }
                        ) {
                            homeScreenModel.state.spaceList.forEach { space ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("${space.name}")
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(
                                                        AnnotatedString(
                                                            homeScreenModel.state.space.code
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy code"
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        homeScreenModel.onToggleSpace(space)
                                        homeScreenModel.onSpaceListExpanded(false)
                                    },
                                )

                            }
                        }
                    },
                    actions = {
                        Row {
                            Column {
                                IconButton(onClick = {
                                    homeScreenModel.onMenuExpanded(true)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddCircleOutline,
                                        contentDescription = "Add"
                                    )
                                }
                                DropdownMenu(
                                    expanded = homeScreenModel.state.menuExpanded,
                                    onDismissRequest = {
                                        homeScreenModel.onMenuExpanded(false)
                                    }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Create Space") },
                                        onClick = {
                                            homeScreenModel.onShowCreateDialog(true)
                                            homeScreenModel.onMenuExpanded(false)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Join Space") },
                                        onClick = {
                                            homeScreenModel.onShowJoinDialog(true)
                                            homeScreenModel.onMenuExpanded(false)
                                        }
                                    )
                                }
                            }

                            Column {
                                AsyncImage(
                                    model = homeScreenModel.state.userInfo.avatar,
                                    modifier = Modifier.size(48.dp),
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                val canShow = homeScreenModel.state.currentUserId != null
                        && homeScreenModel.state.space.id.isNotBlank()
                AnimatedVisibility(canShow) {
                    FloatingActionButton(onClick = {
                        navigator?.push(
                            EditExpenseScreen(
                                spaceId = homeScreenModel.state.space.id,
                                payerId = homeScreenModel.state.currentUserId!!
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Create,
                            contentDescription = "create new expense"
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (homeScreenModel.state.showCreateSpaceDialog) {
                    Dialog(
                        onDismissRequest = {
                            homeScreenModel.onShowCreateDialog(false)
                        },
                        properties = DialogProperties()
                    ) {
                        val keyboard = LocalSoftwareKeyboardController.current
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .widthIn(min = 280.dp, max = 360.dp)
                            ) {
                                Text(
                                    text = "空间名称",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = homeScreenModel.state.newSpaceName,
                                    onValueChange = { homeScreenModel.onNewSpaceNameChange(it) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { homeScreenModel.onDismissCreateDialog() }) {
                                        Text("Cancel")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                homeScreenModel.submitNewSpace()
                                                homeScreenModel.onDismissCreateDialog()
                                            }
                                        },
                                        enabled = homeScreenModel.state.newSpaceName.isNotEmpty()
                                    ) {
                                        Text("Create")
                                    }
                                }
                            }
                        }
                    }
                }
                if (homeScreenModel.state.showJoinSpaceDialog) {
                    Dialog(
                        onDismissRequest = {
                            homeScreenModel.onShowJoinDialog(false)
                        },
                        properties = DialogProperties()
                    ) {
                        val keyboard = LocalSoftwareKeyboardController.current
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .widthIn(min = 280.dp, max = 360.dp)
                            ) {
                                Text(
                                    text = "空间分享code",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = homeScreenModel.state.joinSpaceCode,
                                    onValueChange = { homeScreenModel.onJoinSpaceCodeChange(it) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { homeScreenModel.onDismissJoinDialog() }) {
                                        Text("Cancel")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                homeScreenModel.submitJoinSpace()
                                                homeScreenModel.onDismissJoinDialog()
                                            }
                                        },
                                        enabled = homeScreenModel.state.joinSpaceCode.isNotEmpty()
                                    ) {
                                        Text("Join")
                                    }
                                }
                            }
                        }
                    }
                }
                Column {
                    homeScreenModel.state.spaceList.forEach { space ->
                        Text(space.toString())
                    }

                }
            }
        }
    }
}

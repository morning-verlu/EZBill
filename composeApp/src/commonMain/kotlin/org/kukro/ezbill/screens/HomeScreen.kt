package org.kukro.ezbill.screens

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
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.kukro.ezbill.screenModels.HomeScreenModel

class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val hostState = LocalSnackBarHostState.current
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        val homeScreenModel = rememberScreenModel { HomeScreenModel() }


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
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            Row {
                                Text(
                                    text = homeScreenModel.state.space.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
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
                                    text = { Text("${space.name}") },
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
            }
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
                                    onValueChange = { homeScreenModel.onSpaceNameChange(it) },
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
                Column {
                    homeScreenModel.state.spaceList.forEach { space ->
                        Text(space.toString())
                    }

                }
            }
        }
    }
}

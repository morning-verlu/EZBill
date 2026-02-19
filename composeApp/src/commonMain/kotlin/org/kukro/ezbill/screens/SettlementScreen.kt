package org.kukro.ezbill.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.models.SpaceMember
import org.kukro.ezbill.screenModels.SettlementScreenModel
import org.kukro.ezbill.screenModels.SettlementUiState

class SettlementScreen(
    private val spaceId: String,
    private val members: List<SpaceMember>
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val hostState = LocalSnackBarHostState.current

        val screenModel = rememberScreenModel {
            SettlementScreenModel(spaceId = spaceId, members = members)
        }

        LaunchedEffect(Unit) {
            screenModel.recalculate()
        }

        LaunchedEffect(screenModel.uiState) {
            val ui = screenModel.uiState
            if (ui is SettlementUiState.Error) {
                hostState.showSnackbar(ui.msg)
            }
            if (ui is SettlementUiState.Success && !ui.msg.isNullOrBlank()) {
                hostState.showSnackbar(ui.msg)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("结算分摊") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = screenModel.note,
                    onValueChange = { screenModel.onNoteChange(it) },
                    label = { Text("结算备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )

                when (val ui = screenModel.uiState) {
                    is SettlementUiState.Loading -> {
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        Spacer(Modifier.weight(1f))
                    }

                    is SettlementUiState.Error -> {
                        Spacer(Modifier.weight(1f))
                        Text("计算失败，请返回重试")
                        Spacer(Modifier.weight(1f))
                    }

                    is SettlementUiState.Success -> {
                        Text("成员净额")
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ui.preview.nets, key = { it.userId }) { net ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(net.displayName ?: net.userId.take(6))
                                        Text("付出: ¥${net.paid}  分摊: ¥${net.share}")
                                        Text("净额: ¥${net.net}")
                                    }
                                }
                            }

                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            item {
                                Text("转账建议")
                            }

                            if (ui.preview.transfers.isEmpty()) {
                                item { Text("当前已平账，无需转账") }
                            } else {
                                items(ui.preview.transfers) { t ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "${t.fromName ?: t.fromUserId.take(6)} -> ${
                                                    t.toName ?: t.toUserId.take(
                                                        6
                                                    )
                                                }"
                                            )
                                            Text("¥${t.amount}")
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { screenModel.save { navigator.pop() } },
                            enabled = !screenModel.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (screenModel.isSaving) {
                                Text("保存中...")
                            } else {
                                Text("保存本次结算")
                            }
                        }
                    }
                }
            }
        }
    }
}

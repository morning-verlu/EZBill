package org.kukro.ezbill.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.round
import org.kukro.ezbill.AppConfig
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.screenModels.SettlementPreview
import org.kukro.ezbill.screenModels.SettlementScreenModel
import org.kukro.ezbill.screenModels.SettlementUiState
import org.kukro.ezbill.screenModels.UserNet

class SettlementScreen(
    private val spaceId: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val hostState = LocalSnackBarHostState.current

        val screenModel = rememberScreenModel {
            SettlementScreenModel(spaceId = spaceId)
        }

        var showNoteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { screenModel.recalculate() }

        LaunchedEffect(screenModel.uiState) {
            val ui = screenModel.uiState
            if (ui is SettlementUiState.Error) hostState.showSnackbar(ui.msg)
            if (ui is SettlementUiState.Success && !ui.msg.isNullOrBlank()) hostState.showSnackbar(ui.msg)
        }

        if (showNoteDialog) {
            NoteDialog(
                note = screenModel.note,
                onNoteChange = screenModel::onNoteChange,
                onDismiss = { showNoteDialog = false }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("结算") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            bottomBar = {
                if (screenModel.uiState is SettlementUiState.Success) {
                    BottomAppBar(
                        actions = {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        role = Role.Button,
                                        onClick = { showNoteDialog = true }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (screenModel.note.isNotBlank()) screenModel.note else "添加备注",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        },
                        floatingActionButton = {
                            if (screenModel.isSaving) {
                                FloatingActionButton(
                                    onClick = {},
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                ExtendedFloatingActionButton(
                                    onClick = { screenModel.save { navigator.pop() } },
                                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                                    text = { Text("保存结算") }
                                )
                            }
                        }
                    )
                }
            }
        ) { padding ->
            when (val ui = screenModel.uiState) {
                is SettlementUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("正在计算...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                is SettlementUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "结算计算失败",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("请检查网络后重试", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = screenModel::recalculate) { Text("重新计算") }
                    }
                }

                is SettlementUiState.Success -> {
                    SuccessContent(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        preview = ui.preview,
                        currentUserId = screenModel.currentUserId,
                        memberProfiles = screenModel.memberProfiles
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("结算备注") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text("输入备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun SuccessContent(
    modifier: Modifier = Modifier,
    preview: SettlementPreview,
    currentUserId: String?,
    memberProfiles: Map<String, Profile>
) {
    val sortedNets = remember(preview.nets, currentUserId) {
        if (currentUserId.isNullOrBlank()) preview.nets
        else preview.nets.sortedWith(compareBy { if (it.userId == currentUserId) 0 else 1 })
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "hero") {
            OverviewHero(preview)
        }

        item(key = "transfer_header") {
            SectionHeader(
                title = "转账建议",
                subtitle = if (preview.transfers.isNotEmpty()) "按箭头方向转账即可平账" else null
            )
        }

        item(key = "transfers") {
            if (preview.transfers.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "当前已平账，无需转账",
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                SettlementTransferGraph(
                    transfers = preview.transfers,
                    memberProfiles = memberProfiles,
                    nets = preview.nets
                )
            }
        }

        item(key = "member_header") {
            SectionHeader(title = "分摊明细")
        }

        item(key = "members") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    sortedNets.forEachIndexed { i, net ->
                        MemberRow(
                            net = net,
                            isCurrentUser = !currentUserId.isNullOrBlank() && net.userId == currentUserId,
                            memberProfiles = memberProfiles
                        )
                        if (i < sortedNets.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewHero(preview: SettlementPreview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "总支出",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "¥${formatCurrency(preview.totalExpense)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${preview.expenseCount}笔账单 · ${preview.participantCount}位成员",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemberRow(
    net: UserNet,
    isCurrentUser: Boolean,
    memberProfiles: Map<String, Profile>
) {
    val balance = round2(net.net)
    val accent = when {
        balance > 0.0 -> MaterialTheme.colorScheme.primary
        balance < 0.0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val balanceLabel = when {
        balance > 0.0 -> "应收"
        balance < 0.0 -> "应付"
        else -> "已平"
    }
    val displayName = net.displayName?.takeIf { it.isNotBlank() }
        ?: memberProfiles[net.userId]?.username?.takeIf { it.isNotBlank() }
        ?: net.userId.take(6)
    val avatarUrl = memberProfiles[net.userId]?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: AppConfig.DEFAULT_AVATAR

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentUser) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "我",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Text(
                text = "实付 ¥${formatCurrency(net.paid)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "应摊 ¥${formatCurrency(net.share)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accent.copy(alpha = 0.15f)
            ) {
                Text(
                    text = balanceLabel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
            Text(
                text = "¥${formatCurrency(abs(balance))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
        }
    }
}

private fun round2(value: Double): Double = round(value * 100.0) / 100.0

private fun formatCurrency(value: Double): String {
    val rounded = round2(if (value == -0.0) 0.0 else value)
    val text = rounded.toString()
    val dot = text.indexOf('.')
    return when {
        dot < 0 -> "$text.00"
        text.length - dot - 1 == 1 -> "${text}0"
        else -> text
    }
}

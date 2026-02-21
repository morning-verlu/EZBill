package org.kukro.ezbill.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlin.math.abs
import kotlin.math.round
import org.kukro.ezbill.LocalSnackBarHostState
import org.kukro.ezbill.models.SpaceMember
import org.kukro.ezbill.screenModels.SettlementPreview
import org.kukro.ezbill.screenModels.SettlementScreenModel
import org.kukro.ezbill.screenModels.SettlementUiState
import org.kukro.ezbill.screenModels.TransferSuggestion
import org.kukro.ezbill.screenModels.UserNet

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
            when (val ui = screenModel.uiState) {
                is SettlementUiState.Loading -> {
                    LoadingSettlementContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    )
                }

                is SettlementUiState.Error -> {
                    ErrorSettlementContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        onRetry = screenModel::recalculate
                    )
                }

                is SettlementUiState.Success -> {
                    SettlementSuccessContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        preview = ui.preview,
                        currentUserId = screenModel.currentUserId,
                        note = screenModel.note,
                        isSaving = screenModel.isSaving,
                        onNoteChange = screenModel::onNoteChange,
                        onSave = { screenModel.save { navigator.pop() } }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingSettlementContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = "正在计算结算建议...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorSettlementContent(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "结算计算失败",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "请检查网络后重试",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重新计算")
        }
    }
}

@Composable
private fun SettlementSuccessContent(
    modifier: Modifier = Modifier,
    preview: SettlementPreview,
    currentUserId: String?,
    note: String,
    isSaving: Boolean,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val displayNets = if (currentUserId.isNullOrBlank()) {
        preview.nets
    } else {
        preview.nets.sortedWith(compareBy { if (it.userId == currentUserId) 0 else 1 })
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettlementOverviewCard(preview = preview)
            }

            item {
                Text(
                    text = "成员分摊",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(displayNets, key = { it.userId }) { net ->
                MemberSettlementCard(
                    net = net,
                    isCurrentUser = !currentUserId.isNullOrBlank() && net.userId == currentUserId
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "转账建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (preview.transfers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "当前已平账，无需转账",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                items(preview.transfers) { transfer ->
                    TransferSuggestionCard(transfer = transfer)
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("结算备注（可选）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text("保存中...")
                }
            } else {
                Text("保存本次结算")
            }
        }
    }
}

@Composable
private fun SettlementOverviewCard(preview: SettlementPreview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "本次结算概览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewMetric(
                    label = "总支出",
                    value = "¥${formatCurrency(preview.totalExpense)}",
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    label = "参与人数",
                    value = "${preview.participantCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewMetric(
                    label = "账单数量",
                    value = "${preview.expenseCount}",
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    label = "转账建议",
                    value = "${preview.transfers.size}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MemberSettlementCard(
    net: UserNet,
    isCurrentUser: Boolean
) {
    val balance = round2(net.net)
    val balanceLabel = when {
        balance > 0.0 -> "应收"
        balance < 0.0 -> "应付"
        else -> "已平账"
    }
    val balanceValue = "¥${formatCurrency(abs(balance))}"
    val labelContainerColor = when {
        balance > 0.0 -> MaterialTheme.colorScheme.primary
        balance < 0.0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val labelContentColor = when {
        balance > 0.0 -> MaterialTheme.colorScheme.onPrimary
        balance < 0.0 -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val balanceColor = when {
        balance > 0.0 -> MaterialTheme.colorScheme.primary
        balance < 0.0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentUser) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = net.displayName ?: net.userId.take(6),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (isCurrentUser) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "当前账号",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "支出：¥${formatCurrency(net.paid)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "分摊：¥${formatCurrency(net.share)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = labelContainerColor
                ) {
                    Text(
                        text = balanceLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = labelContentColor
                    )
                }
                Text(
                    text = balanceValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
private fun TransferSuggestionCard(transfer: TransferSuggestion) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transfer.fromName ?: transfer.fromUserId.take(6),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transfer.toName ?: transfer.toUserId.take(6),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "转账金额",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¥${formatCurrency(transfer.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
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

package org.kukro.ezbill.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kukro.ezbill.screenModels.EditExpenseScreenModel
import org.kukro.ezbill.screenModels.EditExpenseUIState

class EditExpenseScreen(
    private val spaceId: String,
    private val userId: String
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val editExpenseScreenModel =
            rememberScreenModel {
                EditExpenseScreenModel(
                    spaceId = spaceId,
                    userId = userId
                )
            }

        val isLoading = editExpenseScreenModel.state is EditExpenseUIState.Loading
        val submitEnabled =
            !isLoading &&
                editExpenseScreenModel.amountInputError.isNullOrBlank() &&
                editExpenseScreenModel.expenseData.amount > 0 &&
                editExpenseScreenModel.selectedParticipantIds.isNotEmpty()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("新建账单") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
                    Button(
                        onClick = {
                            editExpenseScreenModel.submit {
                                navigator.pop()
                            }
                        },
                        enabled = submitEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Text(if (isLoading) "保存中..." else "保存")
                    }
                }
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = editExpenseScreenModel.expenseAmountText,
                        onValueChange = { editExpenseScreenModel.onAmountChange(it) },
                        label = { Text("金额") },
                        supportingText = {
                            Text(
                                editExpenseScreenModel.amountInputError
                                    ?: "上限 ¥999999.99"
                            )
                        },
                        isError = !editExpenseScreenModel.amountInputError.isNullOrBlank(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = editExpenseScreenModel.expenseData.note ?: "",
                        onValueChange = { editExpenseScreenModel.onNoteChange(it) },
                        label = { Text("备注（可选）") },
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "付款人",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                items(
                    items = editExpenseScreenModel.spaceMembers,
                    key = { "payer-${it.userId}" }
                ) { member ->
                    PayerRow(
                        name = member.displayName ?: member.userId,
                        selected = editExpenseScreenModel.selectedPayerId == member.userId,
                        onClick = { editExpenseScreenModel.onSelectPayerId(member.userId) }
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "参与成员",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${editExpenseScreenModel.selectedParticipantIds.size}/${editExpenseScreenModel.spaceMembers.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (editExpenseScreenModel.selectedParticipantIds.isEmpty()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                items(
                    items = editExpenseScreenModel.spaceMembers,
                    key = { "participant-${it.userId}" }
                ) { member ->
                    val checked = editExpenseScreenModel.selectedParticipantIds.contains(member.userId)
                    ParticipantRow(
                        name = member.displayName ?: member.userId,
                        checked = checked,
                        onClick = { editExpenseScreenModel.onToggleParticipant(member.userId) }
                    )
                }

                if (editExpenseScreenModel.state is EditExpenseUIState.Error) {
                    item {
                        Text(
                            text = (editExpenseScreenModel.state as EditExpenseUIState.Error).msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Dialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("正在保存账单…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PayerRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ParticipantRow(
    name: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = checked,
                onClick = onClick,
                role = Role.Checkbox
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

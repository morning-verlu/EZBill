package org.kukro.ezbill.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kukro.ezbill.models.SpaceMember
import org.kukro.ezbill.screenModels.EditExpenseScreenModel
import org.kukro.ezbill.screenModels.EditExpenseUIState

class EditExpenseScreen(
    private val spaceId: String,
    private val userId: String,
    private val members: List<SpaceMember>
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val editExpenseScreenModel =
            rememberScreenModel { EditExpenseScreenModel(spaceId = spaceId, userId = userId) }

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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editExpenseScreenModel.expenseAmountText,
                    onValueChange = { editExpenseScreenModel.onAmountChange(it) },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editExpenseScreenModel.expenseData.note ?: "",
                    onValueChange = { editExpenseScreenModel.onNoteChange(it) },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "付款人",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                )

                members.forEach { member ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (editExpenseScreenModel.selectedPayerId == member.userId),
                                onClick = {
                                    editExpenseScreenModel.onSelectPayerId(member.userId)
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (editExpenseScreenModel.selectedPayerId == member.userId),
                            onClick = null
                        )
                        Text(
                            text = member.displayName ?: member.userId,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        editExpenseScreenModel.submit {
                            navigator.pop()
                        }
                    },
                    enabled = editExpenseScreenModel.expenseData.amount.toString()
                        .isNotBlank() && editExpenseScreenModel.expenseData.amount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
               AnimatedVisibility(editExpenseScreenModel.state is EditExpenseUIState.Loading){
                   Dialog(onDismissRequest = {

                   }) {
                       LoadingIndicator()
                   }
               }
            }
        }
    }
}
package com.vine.ht_operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vine.designsystem.component.ZaikoScreenScaffold

@Composable
fun HtOutboundRoute(
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    viewModel: HtOutboundViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    HtOutboundScreen(
        uiState = uiState,
        onBack = onBack,
        onProductCodeChanged = viewModel::onProductCodeChanged,
        onLocationCodeChanged = viewModel::onLocationCodeChanged,
        onQuantityChanged = viewModel::onQuantityChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onSubmit = viewModel::submit,
        onExportJson = viewModel::exportJson,
        onFinish = {
            val message = uiState.completedMessage ?: "出庫を登録しました"
            viewModel.clearCompletedState()
            onComplete(message)
        },
    )
}

@Composable
private fun HtOutboundScreen(
    uiState: HtOutboundUiState,
    onBack: () -> Unit,
    onProductCodeChanged: (String) -> Unit,
    onLocationCodeChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onExportJson: () -> Unit,
    onFinish: () -> Unit,
) {
    ZaikoScreenScaffold(
        title = "HT 出庫登録",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.productCode,
                onValueChange = onProductCodeChanged,
                label = { Text("商品コード") },
                singleLine = true,
                enabled = !uiState.isSaving && !uiState.isExporting,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.locationCode,
                onValueChange = onLocationCodeChanged,
                label = { Text("出庫元ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving && !uiState.isExporting,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.quantity,
                onValueChange = onQuantityChanged,
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSaving && !uiState.isExporting,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = onNoteChanged,
                label = { Text("備考") },
                singleLine = true,
                enabled = !uiState.isSaving && !uiState.isExporting,
            )

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            uiState.completedMessage?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "保存完了",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(text = message)

                        uiState.exportMessage?.let { exportMessage ->
                            Text(
                                text = exportMessage,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = onExportJson,
                                enabled = !uiState.isExporting,
                            ) {
                                Text(
                                    if (uiState.isExporting) "書き出し中..." else "JSON書き出し"
                                )
                            }

                            Button(
                                onClick = onFinish,
                                enabled = !uiState.isExporting,
                            ) {
                                Text("完了")
                            }
                        }
                    }
                }
            }

            if (uiState.completedMessage == null) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSubmit,
                    onClick = onSubmit,
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text("登録")
                    }
                }
            }
        }
    }
}
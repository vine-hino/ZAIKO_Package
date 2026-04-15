package com.vine.ht_operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vine.designsystem.component.ZaikoScreenScaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HtStockListScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HtStockListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ZaikoScreenScaffold(
        title = "HT 在庫照会",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.keyword,
                onValueChange = viewModel::onKeywordChanged,
                label = { Text("商品コード / バーコード") },
                singleLine = true,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::search,
                enabled = !uiState.isLoading,
            ) {
                Text(if (uiState.isLoading) "検索中..." else "検索")
            }

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!uiState.isLoading && uiState.items.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "該当する在庫がありません",
                    )
                }
            }

            uiState.items.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${item.productCode} / ${item.productName}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "倉庫: ${item.warehouseCode} ${item.warehouseName}",
                        )
                        Text(
                            text = "ロケーション: ${item.locationCode} ${item.locationName}",
                        )
                        Text(
                            text = "在庫数: ${item.quantity}",
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        TextButton(
                            modifier = Modifier.padding(top = 8.dp),
                            onClick = onOpenHistory,
                        ) {
                            Text("履歴を見る")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HtStockHistoryScreen(
    onBack: () -> Unit,
    viewModel: HtStockHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ZaikoScreenScaffold(
        title = "HT 在庫履歴",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::refresh,
                enabled = !uiState.isLoading,
            ) {
                Text(if (uiState.isLoading) "読込中..." else "再読込")
            }

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!uiState.isLoading && uiState.items.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "履歴がありません",
                    )
                }
            }

            uiState.items.forEach { history ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = formatHistoryDateTime(history.operatedAtEpochMillis),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = "${history.operationType}  ${signedQuantity(history.deltaQuantity)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(text = "${history.productCode} / ${history.productName}")
                        Text(text = "倉庫: ${history.warehouseCode}")
                        Text(text = "ロケーション: ${history.locationCode}")
                        Text(text = "担当: ${history.operatorName}")
                        history.note?.takeIf { it.isNotBlank() }?.let {
                            Text(text = "備考: $it")
                        }
                    }
                }
            }
        }
    }
}

private fun formatHistoryDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun signedQuantity(quantity: Long): String {
    return if (quantity > 0) "+$quantity" else quantity.toString()
}

@Composable
fun HtInboundScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: HtInboundViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.completedMessage) {
        if (uiState.completedMessage != null) {
            onComplete()
            viewModel.consumeCompleted()
        }
    }

    val canSave = uiState.productCode.isNotBlank() &&
            uiState.locationCode.isNotBlank() &&
            (uiState.quantity.toLongOrNull()?.let { it > 0L } == true) &&
            !uiState.isSaving

    ZaikoScreenScaffold(
        title = "HT 入庫登録",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.productCode,
                onValueChange = viewModel::onProductCodeChanged,
                label = { Text("商品コード") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.locationCode,
                onValueChange = viewModel::onLocationCodeChanged,
                label = { Text("入庫先ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.quantity,
                onValueChange = viewModel::onQuantityChanged,
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submit,
            ) {
                Text(
                    text = if (uiState.isSaving) "登録中..." else "登録"
                )
            }
        }
    }
}

@Composable
fun HtOutboundScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: HtOutboundViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completedMessage) {
        if (uiState.completedMessage != null) {
            onComplete()
            viewModel.consumeCompleted()
        }
    }

    val canSave = uiState.productCode.isNotBlank() &&
            uiState.locationCode.isNotBlank() &&
            (uiState.quantity.toLongOrNull()?.let { it > 0L } == true) &&
            !uiState.isSaving

    ZaikoScreenScaffold(
        title = "HT 出庫登録",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.productCode,
                onValueChange = viewModel::onProductCodeChanged,
                label = { Text("商品コード") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.locationCode,
                onValueChange = viewModel::onLocationCodeChanged,
                label = { Text("出庫元ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.quantity,
                onValueChange = viewModel::onQuantityChanged,
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submit,
            ) {
                Text(
                    text = if (uiState.isSaving) "登録中..." else "登録"
                )
            }
        }
    }
}

@Composable
fun HtMoveScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: HtMoveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completedMessage) {
        if (uiState.completedMessage != null) {
            onComplete()
            viewModel.consumeCompleted()
        }
    }

    val canSave = uiState.productCode.isNotBlank() &&
            uiState.fromLocationCode.isNotBlank() &&
            uiState.toLocationCode.isNotBlank() &&
            uiState.fromLocationCode != uiState.toLocationCode &&
            (uiState.quantity.toLongOrNull()?.let { it > 0L } == true) &&
            !uiState.isSaving

    ZaikoScreenScaffold(
        title = "HT 移動登録",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.productCode,
                onValueChange = viewModel::onProductCodeChanged,
                label = { Text("商品コード") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.fromLocationCode,
                onValueChange = viewModel::onFromLocationCodeChanged,
                label = { Text("移動元ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.toLocationCode,
                onValueChange = viewModel::onToLocationCodeChanged,
                label = { Text("移動先ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.quantity,
                onValueChange = viewModel::onQuantityChanged,
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submit,
            ) {
                Text(
                    text = if (uiState.isSaving) "登録中..." else "登録"
                )
            }
        }
    }
}

@Composable
fun HtStocktakeScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: HtStocktakeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completedMessage) {
        if (uiState.completedMessage != null) {
            onComplete()
            viewModel.consumeCompleted()
        }
    }

    val canSave = uiState.productCode.isNotBlank() &&
            uiState.locationCode.isNotBlank() &&
            (uiState.actualQuantity.toLongOrNull()?.let { it >= 0L } == true) &&
            !uiState.isSaving

    ZaikoScreenScaffold(
        title = "HT 棚卸入力",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.productCode,
                onValueChange = viewModel::onProductCodeChanged,
                label = { Text("商品コード") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.locationCode,
                onValueChange = viewModel::onLocationCodeChanged,
                label = { Text("ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.actualQuantity,
                onValueChange = viewModel::onActualQuantityChanged,
                label = { Text("実棚数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "※ この画面では棚卸を保存します。在庫へ反映されるのは確定時です。",
                style = MaterialTheme.typography.bodySmall,
            )

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submit,
            ) {
                Text(
                    text = if (uiState.isSaving) "保存中..." else "保存"
                )
            }
        }
    }
}

@Composable
fun HtAdjustmentScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: HtAdjustmentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completedMessage) {
        if (uiState.completedMessage != null) {
            onComplete()
            viewModel.consumeCompleted()
        }
    }

    val canSave = uiState.productCode.isNotBlank() &&
            uiState.locationCode.isNotBlank() &&
            uiState.reasonCode.isNotBlank() &&
            (uiState.adjustQuantity.toLongOrNull()?.let { it != 0L } == true) &&
            !(uiState.reasonCode == "OTHER" && uiState.note.isBlank()) &&
            !uiState.isSaving

    ZaikoScreenScaffold(
        title = "HT 在庫調整",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.productCode,
                onValueChange = viewModel::onProductCodeChanged,
                label = { Text("商品コード") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.locationCode,
                onValueChange = viewModel::onLocationCodeChanged,
                label = { Text("ロケーション") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.adjustQuantity,
                onValueChange = viewModel::onAdjustQuantityChanged,
                label = { Text("調整数（増:+ / 減:-）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.reasonCode,
                onValueChange = viewModel::onReasonCodeChanged,
                label = { Text("理由コード") },
                placeholder = { Text("DAMAGE / LOSS / CORRECT / DIFF / OTHER") },
                singleLine = true,
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
                enabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "理由コード例: DAMAGE=破損 / LOSS=紛失 / CORRECT=誤登録訂正 / DIFF=棚卸差異 / OTHER=その他",
                style = MaterialTheme.typography.bodySmall,
            )

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submit,
            ) {
                Text(
                    text = if (uiState.isSaving) "登録中..." else "登録"
                )
            }
        }
    }
}

@Composable
fun HtCompletedScreen(
    message: String,
    onBackHome: () -> Unit,
    onContinue: () -> Unit,
) {
    ZaikoScreenScaffold(title = "HT 完了") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBackHome,
            ) {
                Text("ホームへ戻る")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinue,
            ) {
                Text("続けて登録")
            }
        }
    }
}

@Composable
private fun BasicOperationScreen(
    title: String,
    firstLabel: String,
    secondLabel: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val first = remember { mutableStateOf("") }
    val second = remember { mutableStateOf("") }
    val quantity = remember { mutableStateOf("") }
    val note = remember { mutableStateOf("") }

    val canSave =
        first.value.isNotBlank() &&
                second.value.isNotBlank() &&
                quantity.value.toIntOrNull()?.let { it > 0 } == true

    ZaikoScreenScaffold(
        title = title,
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = first.value,
                onValueChange = { first.value = it },
                label = { Text(firstLabel) },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = second.value,
                onValueChange = { second.value = it },
                label = { Text(secondLabel) },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = quantity.value,
                onValueChange = { quantity.value = it },
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = note.value,
                onValueChange = { note.value = it },
                label = { Text("備考") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = onComplete,
            ) {
                Text("登録")
            }
        }
    }
}

@Composable
private fun ScrollForm(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        content()
    }
}
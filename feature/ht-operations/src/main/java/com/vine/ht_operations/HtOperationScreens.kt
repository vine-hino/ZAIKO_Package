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

@Composable
fun HtStockListScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val items = listOf(
        "ITEM-001 / ネジ M3 / A-01 / 120",
        "ITEM-002 / ボルト M5 / A-02 / 56",
        "ITEM-003 / ワッシャー / B-01 / 240",
    )
    val search = remember { mutableStateOf("") }

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
                value = search.value,
                onValueChange = { search.value = it },
                label = { Text("商品コード / バーコード") },
                singleLine = true,
            )

            items
                .filter { it.contains(search.value, ignoreCase = true) || search.value.isBlank() }
                .forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(text = item)
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
) {
    val histories = listOf(
        "2026-04-14 09:00 入庫 +100 担当: 山田",
        "2026-04-14 10:20 出庫 -10 担当: 佐藤",
        "2026-04-14 14:30 調整 -2 担当: 鈴木",
    )

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
            histories.forEach { history ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = history,
                    )
                }
            }
        }
    }
}

@Composable
fun HtInboundScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    BasicOperationScreen(
        title = "HT 入庫登録",
        firstLabel = "商品コード",
        secondLabel = "入庫先ロケーション",
        onBack = onBack,
        onComplete = onComplete,
    )
}

@Composable
fun HtOutboundScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    BasicOperationScreen(
        title = "HT 出庫登録",
        firstLabel = "商品コード",
        secondLabel = "出庫元ロケーション",
        onBack = onBack,
        onComplete = onComplete,
    )
}

@Composable
fun HtMoveScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val productCode = remember { mutableStateOf("") }
    val fromLocation = remember { mutableStateOf("") }
    val toLocation = remember { mutableStateOf("") }
    val quantity = remember { mutableStateOf("") }
    val note = remember { mutableStateOf("") }

    val canSave =
        productCode.value.isNotBlank() &&
                fromLocation.value.isNotBlank() &&
                toLocation.value.isNotBlank() &&
                quantity.value.toIntOrNull()?.let { it > 0 } == true

    ZaikoScreenScaffold(
        title = "HT 在庫移動",
        onBack = onBack,
    ) { padding ->
        ScrollForm(
            padding = padding,
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = productCode.value,
                onValueChange = { productCode.value = it },
                label = { Text("商品コード") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = fromLocation.value,
                onValueChange = { fromLocation.value = it },
                label = { Text("移動元ロケーション") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = toLocation.value,
                onValueChange = { toLocation.value = it },
                label = { Text("移動先ロケーション") },
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
fun HtStocktakeScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val productCode = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val systemQty = remember { mutableStateOf("100") }
    val actualQty = remember { mutableStateOf("") }

    val diff = (actualQty.value.toIntOrNull() ?: 0) - (systemQty.value.toIntOrNull() ?: 0)
    val canSave =
        productCode.value.isNotBlank() &&
                location.value.isNotBlank() &&
                actualQty.value.toIntOrNull() != null

    ZaikoScreenScaffold(
        title = "HT 棚卸入力",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = productCode.value,
                onValueChange = { productCode.value = it },
                label = { Text("商品コード") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = location.value,
                onValueChange = { location.value = it },
                label = { Text("ロケーション") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = systemQty.value,
                onValueChange = { systemQty.value = it },
                label = { Text("帳簿在庫数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = actualQty.value,
                onValueChange = { actualQty.value = it },
                label = { Text("実棚数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "差異: $diff",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = onComplete,
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
fun HtAdjustmentScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val productCode = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val currentQty = remember { mutableStateOf("100") }
    val deltaQty = remember { mutableStateOf("") }
    val reason = remember { mutableStateOf("") }

    val canSave =
        productCode.value.isNotBlank() &&
                location.value.isNotBlank() &&
                deltaQty.value.toIntOrNull() != null &&
                reason.value.isNotBlank()

    ZaikoScreenScaffold(
        title = "HT 在庫調整",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = productCode.value,
                onValueChange = { productCode.value = it },
                label = { Text("商品コード") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = location.value,
                onValueChange = { location.value = it },
                label = { Text("ロケーション") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentQty.value,
                onValueChange = { currentQty.value = it },
                label = { Text("現在在庫数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = deltaQty.value,
                onValueChange = { deltaQty.value = it },
                label = { Text("調整数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = reason.value,
                onValueChange = { reason.value = it },
                label = { Text("調整理由") },
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
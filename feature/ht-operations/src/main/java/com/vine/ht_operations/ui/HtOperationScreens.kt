package com.vine.ht_operations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vine.designsystem.component.ZaikoScreenScaffold
import com.vine.ht_operations.presentation.HtAdjustmentViewModel
import com.vine.ht_operations.presentation.HtStockListViewModel
import com.vine.ht_operations.presentation.HtStocktakeViewModel

@Composable
fun HtStockListScreen(
    onBack: () -> Unit,
    onSelectStock: (productCode: String, warehouseCode: String, locationCode: String) -> Unit,
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::search, enabled = !uiState.isLoading) {
                    Text("検索")
                }
                TextButton(onClick = viewModel::search, enabled = !uiState.isLoading) {
                    Text("再読込")
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.items.forEach { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${item.productCode} ${item.productName}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("倉庫: ${item.warehouseCode} / ロケーション: ${item.locationCode}")
                            Text("在庫数: ${item.quantity}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onSelectStock(
                                        item.productCode,
                                        item.warehouseCode,
                                        item.locationCode,
                                    )
                                },
                            ) {
                                Text("この在庫を調整")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HtStocktakeScreen(
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    viewModel: HtStocktakeViewModel = hiltViewModel(),
) {
    val completedMessage = viewModel.completedMessage
    LaunchedEffect(completedMessage) {
        completedMessage?.let {
            viewModel.consumeCompleted()
            onComplete(it)
        }
    }

    val canSave = viewModel.productLookup.selected != null &&
            viewModel.locationLookup.selected != null &&
            viewModel.countedQuantityText.toIntOrNull()?.let { it >= 0 } == true &&
            !viewModel.isSubmitting

    ZaikoScreenScaffold(
        title = "HT 棚卸入力",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            MasterLookupField(
                label = "商品",
                state = viewModel.productLookup,
                onQueryChange = viewModel::onProductQueryChanged,
                onSelect = viewModel::selectProduct,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MasterLookupField(
                label = "ロケーション",
                state = viewModel.locationLookup,
                onQueryChange = viewModel::onLocationQueryChanged,
                onSelect = viewModel::selectLocation,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.countedQuantityText,
                onValueChange = viewModel::onCountedQuantityChanged,
                label = { Text("実棚数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.noteText,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "※ この画面では棚卸を保存します。在庫へ反映されるのはPC側で確定したときです。",
                style = MaterialTheme.typography.bodySmall,
            )

            viewModel.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submitStocktake,
            ) {
                if (viewModel.isSubmitting) {
                    CircularProgressIndicator()
                } else {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
fun HtAdjustmentScreen(
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    initialProductCode: String? = null,
    initialLocationCode: String? = null,
    viewModel: HtAdjustmentViewModel = hiltViewModel(),
) {
    LaunchedEffect(initialProductCode, initialLocationCode) {
        viewModel.setInitialTarget(
            productCode = initialProductCode,
            locationCode = initialLocationCode,
        )
    }

    val completedMessage = viewModel.completedMessage
    LaunchedEffect(completedMessage) {
        completedMessage?.let {
            viewModel.consumeCompleted()
            onComplete(it)
        }
    }

    val canSave = viewModel.productLookup.selected != null &&
            viewModel.locationLookup.selected != null &&
            viewModel.reasonLookup.selected != null &&
            viewModel.quantityText.toIntOrNull()?.let { it != 0 } == true &&
            !viewModel.isSubmitting

    ZaikoScreenScaffold(
        title = "HT 在庫調整",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            MasterLookupField(
                label = "商品",
                state = viewModel.productLookup,
                onQueryChange = viewModel::onProductQueryChanged,
                onSelect = viewModel::selectProduct,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MasterLookupField(
                label = "ロケーション",
                state = viewModel.locationLookup,
                onQueryChange = viewModel::onLocationQueryChanged,
                onSelect = viewModel::selectLocation,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MasterLookupField(
                label = "理由",
                state = viewModel.reasonLookup,
                onQueryChange = viewModel::onReasonQueryChanged,
                onSelect = viewModel::selectReason,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.quantityText,
                onValueChange = viewModel::onQuantityChanged,
                label = { Text("調整数（増:+ / 減:-）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.noteText,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
            )

            viewModel.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submitAdjustment,
            ) {
                if (viewModel.isSubmitting) {
                    CircularProgressIndicator()
                } else {
                    Text("登録")
                }
            }
        }
    }
}

@Composable
fun HtPreparingScreen(
    label: String,
    onBackHome: () -> Unit,
    onBack: () -> Unit,
) {
    ZaikoScreenScaffold(
        title = "準備中",
        onBack = onBack,
    ) { padding ->
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
                    text = "$label は準備中です。",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = "このボタンを押してもアプリが落ちないようにし、準備中メッセージを表示するようにしています。",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBackHome,
            ) {
                Text("ホームへ戻る")
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
fun ScrollForm(
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

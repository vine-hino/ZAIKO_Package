package com.vine.ht_operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vine.connector_api.MasterLookupItem

@Composable
fun HtInboundRoute(
    modifier: Modifier = Modifier,
    viewModel: HtInboundViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "入庫登録",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (uiState.successMessage != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.successMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (uiState.errorMessage != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.errorMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "商品選択",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = uiState.productKeyword,
            onValueChange = viewModel::onProductKeywordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("商品コード / 商品名") },
            singleLine = true,
        )

        Button(
            onClick = viewModel::searchProducts,
            enabled = !uiState.isSearchingProducts,
        ) {
            Text(if (uiState.isSearchingProducts) "商品検索中..." else "商品検索")
        }

        uiState.selectedProduct?.let { selected ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("選択中の商品", fontWeight = FontWeight.Bold)
                    Text("${selected.code} / ${selected.name}")
                }
            }
        }

        if (uiState.productCandidates.isNotEmpty()) {
            MasterCandidateList(
                items = uiState.productCandidates,
                onSelect = viewModel::selectProduct,
            )
        }

        HorizontalDivider()

        Text(
            text = "ロケーション選択",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = uiState.locationKeyword,
            onValueChange = viewModel::onLocationKeywordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ロケーションコード / ロケーション名") },
            singleLine = true,
        )

        Button(
            onClick = viewModel::searchLocations,
            enabled = !uiState.isSearchingLocations,
        ) {
            Text(if (uiState.isSearchingLocations) "ロケーション検索中..." else "ロケーション検索")
        }

        uiState.selectedLocation?.let { selected ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("選択中のロケーション", fontWeight = FontWeight.Bold)
                    Text("${selected.code} / ${selected.name}")
                    Text("倉庫コード: ${selected.warehouseCode.orEmpty()}")
                }
            }
        }

        if (uiState.locationCandidates.isNotEmpty()) {
            MasterCandidateList(
                items = uiState.locationCandidates,
                onSelect = viewModel::selectLocation,
            )
        }

        HorizontalDivider()

        OutlinedTextField(
            value = uiState.quantity,
            onValueChange = viewModel::onQuantityChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("数量") },
            singleLine = true,
        )

        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::onNoteChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("備考") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = viewModel::submit,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSubmitting) "登録中..." else "入庫登録")
        }

        TextButton(
            onClick = viewModel::clearMessage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("メッセージを閉じる")
        }
    }
}

@Composable
private fun MasterCandidateList(
    items: List<MasterLookupItem>,
    onSelect: (MasterLookupItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.code,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = item.name)

                        if (!item.warehouseCode.isNullOrBlank()) {
                            Text(
                                text = "倉庫: ${item.warehouseCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
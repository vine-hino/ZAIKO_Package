package com.vine.pc_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import kotlinx.coroutines.launch

@Composable
fun PcStocktakeManagementScreen() {
    var summaries by remember { mutableStateOf<List<StocktakeSummary>>(emptyList()) }
    var selectedOperationUuid by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<List<StocktakeDetail>>(emptyList()) }
    var diffOnly by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val selectedSummary = summaries.firstOrNull { it.operationUuid == selectedOperationUuid }

    val filteredDetails = details.filter { row ->
        if (keyword.isBlank()) {
            true
        } else {
            listOf(
                row.productCode,
                row.productName,
                row.locationCode,
            ).any { it.contains(keyword, ignoreCase = true) }
        }
    }

    suspend fun reloadSummaries() {
        runCatching {
            PcDependencies.stocktakeServerClient.getDrafts()
        }.onSuccess { rows ->
            summaries = rows
            errorMessage = null

            if (rows.isNotEmpty() && selectedOperationUuid == null) {
                selectedOperationUuid = rows.first().operationUuid
            }

            if (selectedOperationUuid != null && rows.none { it.operationUuid == selectedOperationUuid }) {
                selectedOperationUuid = rows.firstOrNull()?.operationUuid
            }
        }.onFailure { e ->
            errorMessage = e.message ?: "棚卸一覧の取得に失敗しました"
        }
    }

    suspend fun reloadDetails() {
        val operationUuid = selectedOperationUuid ?: run {
            details = emptyList()
            return
        }

        runCatching {
            PcDependencies.stocktakeServerClient.getDetails(
                operationUuid = operationUuid,
                diffOnly = diffOnly,
            )
        }.onSuccess { rows ->
            details = rows
            errorMessage = null
        }.onFailure { e ->
            errorMessage = e.message ?: "棚卸明細の取得に失敗しました"
        }
    }

    LaunchedEffect(Unit) {
        reloadSummaries()
        reloadDetails()
    }

    LaunchedEffect(selectedOperationUuid, diffOnly) {
        reloadDetails()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "棚卸管理",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "HT から送られた棚卸記録を確認し、PC で確定します。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !diffOnly,
                onClick = { diffOnly = false },
                label = { Text("全件表示") },
            )
            FilterChip(
                selected = diffOnly,
                onClick = { diffOnly = true },
                label = { Text("差異のみ") },
            )

            TextButton(
                onClick = {
                    scope.launch {
                        reloadSummaries()
                        reloadDetails()
                    }
                }
            ) {
                Text("再読込")
            }
        }

        message?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        errorMessage?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "棚卸記録一覧",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (summaries.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "棚卸記録はありません",
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(summaries, key = { it.operationUuid }) { summary ->
                            val selected = summary.operationUuid == selectedOperationUuid

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                onClick = {
                                    selectedOperationUuid = summary.operationUuid
                                    message = null
                                },
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = summary.stocktakeNo,
                                        style = MaterialTheme.typography.titleMedium,
                                    )

                                    Text("棚卸日: ${summary.stocktakeDate}")
                                    Text("倉庫: ${summary.warehouseName ?: summary.warehouseCode.orEmpty()}")
                                    Text("状態: ${summary.status}")
                                    Text("明細件数: ${summary.lineCount}")
                                    Text(
                                        text = "差異件数: ${summary.discrepancyLineCount}",
                                        color = if (summary.discrepancyLineCount > 0) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text("入力者: ${summary.enteredByName.orEmpty()}")
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "棚卸詳細",
                    style = MaterialTheme.typography.titleMedium,
                )

                selectedSummary?.let { summary ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = summary.stocktakeNo,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("棚卸日: ${summary.stocktakeDate}")
                            Text("倉庫: ${summary.warehouseName ?: summary.warehouseCode.orEmpty()}")
                            Text("状態: ${summary.status}")
                            Text("明細件数: ${summary.lineCount}")
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("商品コード / 商品名 / ロケーション") },
                    singleLine = true,
                )

                Button(
                    enabled = selectedSummary != null,
                    onClick = {
                        val summary = selectedSummary ?: return@Button
                        scope.launch {
                            runCatching {
                                PcDependencies.stocktakeServerClient.confirm(
                                    operationUuid = summary.operationUuid,
                                    operatorCode = "OP-0001",
                                )
                            }.onSuccess { result ->
                                message = result.message
                                reloadSummaries()
                                reloadDetails()
                            }.onFailure { e ->
                                errorMessage = e.message ?: "棚卸確定に失敗しました"
                            }
                        }
                    },
                ) {
                    Text("この棚卸を確定")
                }

                if (selectedSummary == null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "左の一覧から棚卸記録を選択してください",
                        )
                    }
                } else if (filteredDetails.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "表示できる明細がありません",
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredDetails, key = { it.detailUuid }) { row ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "${row.productCode} / ${row.productName}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text("ロケーション: ${row.locationCode}")
                                    Text("帳簿数: ${row.bookQuantity}")
                                    Text("実棚数: ${row.actualQuantity}")
                                    Text("差異: ${formatSigned(row.diffQuantity)}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSigned(value: Long): String {
    return if (value > 0) "+$value" else value.toString()
}
package com.vine.pc_app

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary

@Composable
fun PcStocktakeManagementScreen() {
    var summaries by remember { mutableStateOf<List<StocktakeSummary>>(emptyList()) }
    var selectedOperationUuid by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<List<StocktakeDetail>>(emptyList()) }
    var diffOnly by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

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
        summaries = PcDependencies.stocktakeServerClient.getDrafts()
        if (selectedOperationUuid == null) {
            selectedOperationUuid = summaries.firstOrNull()?.operationUuid
        }
    }

    suspend fun reloadDetails() {
        val operationUuid = selectedOperationUuid ?: run {
            details = emptyList()
            return
        }

        details = PcDependencies.stocktakeServerClient.getDetails(
            operationUuid = operationUuid,
            diffOnly = diffOnly,
        )
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
            text = "棚卸確認",
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    message = null
                },
            ) {
                Text("表示中")
            }

            Button(
                onClick = {
                    diffOnly = !diffOnly
                },
            ) {
                Text(if (diffOnly) "差異のみ: ON" else "差異のみ: OFF")
            }
        }

        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
            )
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
                    text = "未確定棚卸一覧",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (summaries.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "未確定の棚卸はありません",
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(summaries, key = { it.operationUuid }) { summary ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedOperationUuid = summary.operationUuid
                                },
                            ) {
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
                    text = "棚卸明細",
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
                        kotlinx.coroutines.runBlocking {
                            val result = PcDependencies.stocktakeServerClient.confirm(
                                operationUuid = summary.operationUuid,
                                operatorCode = "OP-0001",
                            )
                            message = result.message
                            reloadSummaries()
                            reloadDetails()
                        }
                    },
                ) {
                    Text("この棚卸を確定")
                }

                if (selectedSummary == null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "棚卸を選択してください",
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
                                    Text("差異: ${signed(row.diffQuantity)}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun signed(value: Long): String {
    return if (value > 0) "+$value" else value.toString()
}
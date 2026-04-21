package com.vine.pc_app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.GetStocktakeDetailsQuery
import com.vine.inventory_contract.GetStocktakeSummariesQuery
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import com.vine.pc_app.data.PcDependencies
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun StocktakeConfirmScreen() {
    val repository = remember { PcDependencies.stocktakeRepository }
    val importer = remember { PcDependencies.stocktakeJsonImporter }

    var summaries by remember { mutableStateOf<List<StocktakeSummary>>(emptyList()) }
    var selectedOperationUuid by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<List<StocktakeDetail>>(emptyList()) }
    var keyword by remember { mutableStateOf("") }
    var diffOnly by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reloadSummaries() {
        val rows = repository.getSummaries(
            GetStocktakeSummariesQuery(
                status = "DRAFT",
                limit = 200,
            ),
        )
        summaries = rows

        if (rows.none { it.operationUuid == selectedOperationUuid }) {
            selectedOperationUuid = rows.firstOrNull()?.operationUuid
        }
    }

    fun reloadDetails() {
        val operationUuid = selectedOperationUuid
        if (operationUuid == null) {
            details = emptyList()
            return
        }

        details = repository.getDetails(
            GetStocktakeDetailsQuery(
                operationUuid = operationUuid,
                diffOnly = diffOnly,
            ),
        )
    }

    LaunchedEffect(Unit) {
        reloadSummaries()
    }

    LaunchedEffect(selectedOperationUuid, diffOnly) {
        reloadDetails()
    }

    val selectedSummary = summaries.firstOrNull { it.operationUuid == selectedOperationUuid }

    val filteredDetails = details.filter { row ->
        keyword.isBlank() ||
                row.productCode.contains(keyword, ignoreCase = true) ||
                row.productName.contains(keyword, ignoreCase = true) ||
                row.locationCode.contains(keyword, ignoreCase = true)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "棚卸確定",
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        try {
                            val file = chooseJsonFile() ?: return@Button
                            val result = importer.importFile(file)
                            message = result.message
                            reloadSummaries()
                        } catch (e: Exception) {
                            message = e.message ?: "JSON取込に失敗しました"
                        }
                    },
                ) {
                    Text("JSON取込")
                }

                Button(
                    onClick = {
                        try {
                            reloadSummaries()
                            reloadDetails()
                            message = "再読込しました"
                        } catch (e: Exception) {
                            message = e.message ?: "再読込に失敗しました"
                        }
                    },
                ) {
                    Text("再読込")
                }

                Button(
                    enabled = selectedSummary != null,
                    onClick = {
                        val summary = selectedSummary ?: return@Button
                        try {
                            val result = repository.confirm(
                                ConfirmStocktakeCommand(
                                    operationUuid = summary.operationUuid,
                                    operatorCode = "OP-0001",
                                ),
                            )
                            message = result.message
                            reloadSummaries()
                            reloadDetails()
                        } catch (e: Exception) {
                            message = e.message ?: "棚卸確定に失敗しました"
                        }
                    },
                ) {
                    Text("この棚卸を確定")
                }

                Button(
                    onClick = { diffOnly = !diffOnly },
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
                            items(
                                items = summaries,
                                key = { it.operationUuid },
                            ) { summary ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    border = if (summary.operationUuid == selectedOperationUuid) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
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
                                        Text("入力者: ${summary.enteredByName ?: ""}")
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
                            items(
                                items = filteredDetails,
                                key = { it.detailUuid },
                            ) { row ->
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
                                        Text(
                                            text = "差異: ${signed(row.diffQuantity)}",
                                            color = if (row.diffQuantity == 0L) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                        )
                                    }
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

private fun chooseJsonFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "棚卸JSONを選択"
        fileFilter = FileNameExtensionFilter("JSON files", "json")
        isMultiSelectionEnabled = false
    }

    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}
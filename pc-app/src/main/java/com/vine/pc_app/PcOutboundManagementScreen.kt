package com.vine.pc_app

import androidx.compose.foundation.BorderStroke
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
import com.vine.inventory_contract.GetOutboundDetailsQuery
import com.vine.inventory_contract.GetOutboundSummariesQuery
import com.vine.inventory_contract.OutboundDetail
import com.vine.inventory_contract.OutboundSummary
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun PcOutboundManagementScreen() {
    val repository = remember { PcDependencies.outboundRepository }
    val importer = remember { PcDependencies.outboundJsonImporter }

    var summaries by remember { mutableStateOf<List<OutboundSummary>>(emptyList()) }
    var selectedOperationUuid by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<List<OutboundDetail>>(emptyList()) }
    var keyword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun reloadSummaries() {
        val rows = repository.getSummaries(
            GetOutboundSummariesQuery(limit = 200),
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
            GetOutboundDetailsQuery(operationUuid = operationUuid),
        )
    }

    LaunchedEffect(Unit) {
        reloadSummaries()
    }

    LaunchedEffect(selectedOperationUuid) {
        reloadDetails()
    }

    val selectedSummary = summaries.firstOrNull { it.operationUuid == selectedOperationUuid }

    val filteredDetails = details.filter { row ->
        keyword.isBlank() ||
                row.productCode.contains(keyword, ignoreCase = true) ||
                row.fromLocationCode.contains(keyword, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "出庫管理",
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    try {
                        val file = chooseOutboundJsonFile() ?: return@Button
                        val result = importer.importFile(file)
                        message = result.message
                        reloadSummaries()
                    } catch (e: Exception) {
                        message = e.message ?: "出庫JSON取込に失敗しました"
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
                    text = "出庫一覧",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (summaries.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "出庫実績はありません",
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(summaries, key = { it.operationUuid }) { summary ->
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
                                        text = summary.outboundNo,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text("日時: ${formatOutboundDateTime(summary.operatedAtEpochMillis)}")
                                    Text("担当: ${summary.operatorCode}")
                                    Text("倉庫: ${summary.warehouseCode.orEmpty()}")
                                    Text("明細件数: ${summary.lineCount}")
                                    summary.externalDocNo?.let { Text("外部伝票: $it") }
                                    summary.outboundPlanId?.let { Text("予定ID: $it") }
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
                    text = "出庫明細",
                    style = MaterialTheme.typography.titleMedium,
                )

                selectedSummary?.let { summary ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = summary.outboundNo,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("日時: ${formatOutboundDateTime(summary.operatedAtEpochMillis)}")
                            Text("担当: ${summary.operatorCode}")
                            Text("倉庫: ${summary.warehouseCode.orEmpty()}")
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("商品コード / ロケーション") },
                    singleLine = true,
                )

                if (selectedSummary == null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "出庫を選択してください",
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
                                        text = row.productCode,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text("出庫元倉庫: ${row.fromWarehouseCode}")
                                    Text("出庫元ロケーション: ${row.fromLocationCode}")
                                    Text("数量: ${row.quantity}")
                                    row.note?.takeIf { it.isNotBlank() }?.let {
                                        Text("備考: $it")
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

private fun chooseOutboundJsonFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "出庫JSONを選択"
        fileFilter = FileNameExtensionFilter("JSON files", "json")
        isMultiSelectionEnabled = false
    }

    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun formatOutboundDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
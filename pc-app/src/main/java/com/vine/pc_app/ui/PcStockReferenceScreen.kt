package com.vine.pc_app.ui
import com.vine.pc_app.data.PcDependencies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.StockBalanceDto
import com.vine.pc_app.domain.OperationBodyCell
import com.vine.pc_app.domain.OperationEmptyState
import com.vine.pc_app.domain.OperationHeaderCard
import com.vine.pc_app.domain.OperationHeaderCell
import com.vine.pc_app.domain.OperationPageBg
import com.vine.pc_app.domain.OperationResultCard
import com.vine.pc_app.domain.OperationRowEvenBg
import com.vine.pc_app.domain.OperationRowOddBg
import com.vine.pc_app.domain.OperationSearchCard
import com.vine.pc_app.domain.OperationTableHeaderBg
import com.vine.pc_app.domain.SimpleDropdownField
import com.vine.pc_app.domain.SummaryCountBg
import com.vine.pc_app.domain.SummaryMetricCard
import com.vine.pc_app.domain.SummaryQtyBg
import com.vine.pc_app.domain.SummaryWarehouseBg
import com.vine.pc_app.domain.operationDateTimeFormatter
import com.vine.pc_app.domain.operationQuantityFormatter
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class StockReferenceRowModel(
    val productCode: String,
    val productName: String,
    val barcode: String? = null,
    val warehouseName: String,
    val locationName: String,
    val quantity: Int,
    val updatedAt: LocalDateTime,
)

data class StockReferenceSearchCondition(
    val keyword: String = "",
    val warehouseName: String = "",
    val locationName: String = "",
)

@Composable
fun PcStockReferenceScreen(
    onOpenAdjustment: (StockReferenceRowModel) -> Unit = {},
    adjustmentContent: (@Composable () -> Unit)? = null,
) {
    var allRows by remember { mutableStateOf<List<StockReferenceRowModel>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        runCatching {
            PcDependencies.stockBalanceClient
                .getBalances()
                .items
                .map { it.toRowModel() }
        }.onSuccess { rows ->
            allRows = rows
            loadError = null
        }.onFailure { e ->
            loadError = e.message ?: "在庫照会の取得に失敗しました"
        }
    }

    LaunchedEffect(Unit) {
        reload()

        launch {
            runCatching {
                PcDependencies.inventoryRealtimeClient.connect {
                    reload()
                }
            }
        }
    }

    StockReferenceScreen(
        allRows = allRows,
        loadError = loadError,
        onReload = { kotlinx.coroutines.runBlocking { reload() } },
        onRowSelected = onOpenAdjustment,
        adjustmentContent = adjustmentContent,
    )
}

@Composable
fun StockReferenceScreen(
    allRows: List<StockReferenceRowModel>,
    loadError: String?,
    onReload: () -> Unit,
    onRowSelected: (StockReferenceRowModel) -> Unit,
    adjustmentContent: (@Composable () -> Unit)? = null,
) {
    var draftCondition by remember {
        mutableStateOf(StockReferenceSearchCondition())
    }

    var appliedCondition by remember {
        mutableStateOf(StockReferenceSearchCondition())
    }

    val warehouses = remember(allRows) {
        allRows.map { it.warehouseName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val locations = remember(allRows, draftCondition.warehouseName) {
        allRows.asSequence()
            .filter {
                draftCondition.warehouseName.isBlank() || it.warehouseName == draftCondition.warehouseName
            }
            .map { it.locationName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    val filteredRows = remember(allRows, appliedCondition) {
        filterStockRows(allRows, appliedCondition)
    }

    val totalCount = filteredRows.size
    val totalQuantity = filteredRows.sumOf { it.quantity }
    val warehouseCount = filteredRows.map { it.warehouseName }.distinct().size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = OperationPageBg
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            OperationHeaderCard(
                title = "在庫照会",
                description = "商品、倉庫、ロケーション単位で現在在庫を照会する画面です。",
                totalText = "該当 $totalCount 件 / 在庫総数 ${operationQuantityFormatter.format(totalQuantity)}",
                actionText = "再読込",
                onAction = onReload,
            )

            Spacer(modifier = Modifier.height(16.dp))

            loadError?.let { message ->
                OperationSearchCard {
                    Text(
                        text = message,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OperationSearchCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = draftCondition.keyword,
                        onValueChange = { draftCondition = draftCondition.copy(keyword = it) },
                        modifier = Modifier.weight(1.6f),
                        label = { Text("商品キーワード") },
                        placeholder = { Text("商品コード / 商品名") },
                        singleLine = true
                    )

                    SimpleDropdownField(
                        modifier = Modifier.width(220.dp),
                        label = "倉庫",
                        selectedValue = draftCondition.warehouseName,
                        options = warehouses,
                        onSelected = {
                            draftCondition = draftCondition.copy(
                                warehouseName = it,
                                locationName = ""
                            )
                        }
                    )

                    SimpleDropdownField(
                        modifier = Modifier.width(220.dp),
                        label = "ロケーション",
                        selectedValue = draftCondition.locationName,
                        options = locations,
                        onSelected = {
                            draftCondition = draftCondition.copy(locationName = it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            draftCondition = StockReferenceSearchCondition()
                            appliedCondition = StockReferenceSearchCondition()
                        }
                    ) {
                        Text("クリア")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            appliedCondition = draftCondition
                        }
                    ) {
                        Text("検索")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "該当件数",
                    value = operationQuantityFormatter.format(totalCount),
                    subText = "検索結果の件数",
                    backgroundColor = SummaryCountBg
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "在庫総数",
                    value = operationQuantityFormatter.format(totalQuantity),
                    subText = "検索結果の数量合計",
                    backgroundColor = SummaryQtyBg
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "対象倉庫数",
                    value = operationQuantityFormatter.format(warehouseCount),
                    subText = "該当データに含まれる倉庫",
                    backgroundColor = SummaryWarehouseBg
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (adjustmentContent == null) {
                StockReferenceResultTable(
                    rows = filteredRows,
                    onRowSelected = onRowSelected,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1.15f)) {
                        StockReferenceResultTable(
                            rows = filteredRows,
                            onRowSelected = onRowSelected,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        adjustmentContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun StockReferenceResultTable(
    rows: List<StockReferenceRowModel>,
    onRowSelected: (StockReferenceRowModel) -> Unit,
) {
    OperationResultCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OperationTableHeaderBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OperationHeaderCell("商品コード", weight = 1.1f)
            OperationHeaderCell("商品名", weight = 1.8f)
            OperationHeaderCell("倉庫", weight = 1.0f)
            OperationHeaderCell("ロケーション", weight = 1.15f)
            OperationHeaderCell("在庫数", weight = 0.8f)
            OperationHeaderCell("更新日時", weight = 1.25f)
        }

        Divider()

        if (rows.isEmpty()) {
            OperationEmptyState(
                title = "該当する在庫データがありません",
                description = "検索条件を変更して再度お試しください。"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(rows, key = { index, row ->
                    "${row.productCode}_${row.warehouseName}_${row.locationName}_$index"
                }) { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRowSelected(row) }
                            .background(
                                if (index % 2 == 0) OperationRowEvenBg else OperationRowOddBg
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OperationBodyCell(row.productCode, weight = 1.1f)
                        OperationBodyCell(row.productName, weight = 1.8f)
                        OperationBodyCell(row.warehouseName, weight = 1.0f)
                        OperationBodyCell(row.locationName, weight = 1.15f)
                        OperationBodyCell(
                            operationQuantityFormatter.format(row.quantity),
                            weight = 0.8f
                        )
                        OperationBodyCell(
                            row.updatedAt.format(operationDateTimeFormatter),
                            weight = 1.25f
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

private fun filterStockRows(
    source: List<StockReferenceRowModel>,
    condition: StockReferenceSearchCondition,
): List<StockReferenceRowModel> {
    val keyword = condition.keyword.trim()

    return source.asSequence()
        .filter { row ->
            keyword.isBlank() ||
                    row.productCode.contains(keyword, ignoreCase = true) ||
                    row.productName.contains(keyword, ignoreCase = true) ||
                    (row.barcode?.contains(keyword, ignoreCase = true) == true)
        }
        .filter { row ->
            condition.warehouseName.isBlank() || row.warehouseName == condition.warehouseName
        }
        .filter { row ->
            condition.locationName.isBlank() || row.locationName == condition.locationName
        }
        .sortedWith(
            compareBy<StockReferenceRowModel> { it.productCode }
                .thenBy { it.warehouseName }
                .thenBy { it.locationName }
        )
        .toList()
}

private fun StockBalanceDto.toRowModel(): StockReferenceRowModel {
    return StockReferenceRowModel(
        productCode = productCode,
        productName = productName,
        barcode = null,
        warehouseName = warehouseCode,
        locationName = locationCode,
        quantity = quantity.toInt(),
        updatedAt = parseUpdatedAt(updatedAt),
    )
}

private fun parseUpdatedAt(value: String): LocalDateTime {
    return runCatching {
        OffsetDateTime.parse(value).toLocalDateTime()
    }.getOrElse {
        LocalDateTime.now()
    }
}

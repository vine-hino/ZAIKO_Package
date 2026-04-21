package com.vine.pc_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime

data class OperationListConfig(
    val screenTitle: String,
    val screenDescription: String,
    val createActionText: String,
    val operationNoLabel: String,
    val operationDateLabel: String,
    val operationDateColumnLabel: String,
    val warehouseLabel: String,
    val locationLabel: String,
    val emptyTitle: String,
    val emptyDescription: String,
)

data class OperationListRowModel(
    val operationNo: String,
    val operationAt: LocalDateTime,
    val productCode: String,
    val productName: String,
    val barcode: String? = null,
    val warehouseName: String,
    val locationName: String,
    val quantity: Int,
    val note: String = "",
    val operatorName: String? = null,
)

data class OperationListSearchCondition(
    val fromDateText: String = "",
    val toDateText: String = "",
    val keyword: String = "",
    val warehouseName: String = "",
    val locationName: String = "",
    val operationNo: String = "",
)

@Composable
fun OperationListScreen(
    config: OperationListConfig,
    allRows: List<OperationListRowModel>,
    onOpenDetail: (OperationListRowModel) -> Unit = {},
    onCreateNew: () -> Unit = {},
) {
    val todayText = remember {
        LocalDate.now().format(operationDateFormatter)
    }

    var draftCondition by remember {
        mutableStateOf(
            OperationListSearchCondition(
                toDateText = todayText
            )
        )
    }

    var appliedCondition by remember {
        mutableStateOf(
            OperationListSearchCondition(
                toDateText = todayText
            )
        )
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

    val fromDateError = draftCondition.fromDateText.isNotBlank() &&
            parseOperationDateOrNull(draftCondition.fromDateText) == null

    val toDateError = draftCondition.toDateText.isNotBlank() &&
            parseOperationDateOrNull(draftCondition.toDateText) == null

    val filteredRows = remember(allRows, appliedCondition) {
        filterOperationRows(allRows, appliedCondition)
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
                title = config.screenTitle,
                description = config.screenDescription,
                totalText = "該当 $totalCount 件 / 合計数量 ${operationQuantityFormatter.format(totalQuantity)}",
                actionText = config.createActionText,
                onAction = onCreateNew
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchPanel(
                config = config,
                draftCondition = draftCondition,
                warehouses = warehouses,
                locations = locations,
                fromDateError = fromDateError,
                toDateError = toDateError,
                onChange = { draftCondition = it },
                onSearch = {
                    if (!fromDateError && !toDateError) {
                        appliedCondition = draftCondition
                    }
                },
                onClear = {
                    draftCondition = OperationListSearchCondition(
                        toDateText = todayText
                    )
                    appliedCondition = OperationListSearchCondition(
                        toDateText = todayText
                    )
                },
                onQuickToday = {
                    val today = LocalDate.now().format(operationDateFormatter)
                    draftCondition = draftCondition.copy(
                        fromDateText = today,
                        toDateText = today
                    )
                },
                onQuick7Days = {
                    val today = LocalDate.now()
                    draftCondition = draftCondition.copy(
                        fromDateText = today.minusDays(6).format(operationDateFormatter),
                        toDateText = today.format(operationDateFormatter)
                    )
                },
                onQuick30Days = {
                    val today = LocalDate.now()
                    draftCondition = draftCondition.copy(
                        fromDateText = today.minusDays(29).format(operationDateFormatter),
                        toDateText = today.format(operationDateFormatter)
                    )
                }
            )

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
                    title = "合計数量",
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

            ResultSection(
                config = config,
                rows = filteredRows,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

@Composable
private fun SearchPanel(
    config: OperationListConfig,
    draftCondition: OperationListSearchCondition,
    warehouses: List<String>,
    locations: List<String>,
    fromDateError: Boolean,
    toDateError: Boolean,
    onChange: (OperationListSearchCondition) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onQuickToday: () -> Unit,
    onQuick7Days: () -> Unit,
    onQuick30Days: () -> Unit,
) {
    OperationSearchCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = draftCondition.keyword,
                onValueChange = { onChange(draftCondition.copy(keyword = it)) },
                modifier = Modifier.weight(1.6f),
                label = { Text("商品キーワード") },
                placeholder = { Text("商品コード / 商品名 / バーコード") },
                singleLine = true
            )

            androidx.compose.material3.OutlinedTextField(
                value = draftCondition.operationNo,
                onValueChange = { onChange(draftCondition.copy(operationNo = it)) },
                modifier = Modifier.weight(1.0f),
                label = { Text(config.operationNoLabel) },
                placeholder = { Text("番号で検索") },
                singleLine = true
            )

            DatePickerField(
                modifier = Modifier.width(200.dp),
                label = "${config.operationDateLabel} From",
                value = draftCondition.fromDateText,
                isError = fromDateError,
                onDateSelected = {
                    onChange(draftCondition.copy(fromDateText = it))
                }
            )

            DatePickerField(
                modifier = Modifier.width(200.dp),
                label = "${config.operationDateLabel} To",
                value = draftCondition.toDateText,
                isError = toDateError,
                onDateSelected = {
                    onChange(draftCondition.copy(toDateText = it))
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleDropdownField(
                modifier = Modifier.width(220.dp),
                label = config.warehouseLabel,
                selectedValue = draftCondition.warehouseName,
                options = warehouses,
                onSelected = {
                    onChange(
                        draftCondition.copy(
                            warehouseName = it,
                            locationName = ""
                        )
                    )
                }
            )

            SimpleDropdownField(
                modifier = Modifier.width(220.dp),
                label = config.locationLabel,
                selectedValue = draftCondition.locationName,
                options = locations,
                onSelected = {
                    onChange(draftCondition.copy(locationName = it))
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            CompactQuickRangeButton(
                text = "今日",
                onClick = onQuickToday
            )
            Spacer(modifier = Modifier.width(6.dp))

            CompactQuickRangeButton(
                text = "直近7日",
                onClick = onQuick7Days
            )
            Spacer(modifier = Modifier.width(6.dp))

            CompactQuickRangeButton(
                text = "直近30日",
                onClick = onQuick30Days
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClear) {
                Text("クリア")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSearch,
                enabled = !(fromDateError || toDateError)
            ) {
                Text("検索")
            }
        }
    }
}

@Composable
private fun ResultSection(
    config: OperationListConfig,
    rows: List<OperationListRowModel>,
    onOpenDetail: (OperationListRowModel) -> Unit,
) {
    OperationResultCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OperationTableHeaderBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OperationHeaderCell(config.operationDateColumnLabel, weight = 1.35f)
            OperationHeaderCell(config.operationNoLabel, weight = 1.15f)
            OperationHeaderCell("商品", weight = 2.0f)
            OperationHeaderCell("倉庫", weight = 1.05f)
            OperationHeaderCell("ロケーション", weight = 1.15f)
            OperationHeaderCell("数量", weight = 0.8f)
            OperationHeaderCell("備考", weight = 1.55f)
            OperationHeaderCell("操作", weight = 0.75f)
        }

        Divider()

        if (rows.isEmpty()) {
            OperationEmptyState(
                title = config.emptyTitle,
                description = config.emptyDescription
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(rows, key = { _, item -> item.operationNo }) { index, row ->
                    OperationTableRow(
                        row = row,
                        rowColor = if (index % 2 == 0) OperationRowEvenBg else OperationRowOddBg,
                        onOpenDetail = { onOpenDetail(row) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun OperationTableRow(
    row: OperationListRowModel,
    rowColor: androidx.compose.ui.graphics.Color,
    onOpenDetail: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OperationBodyCell(row.operationAt.format(operationDateTimeFormatter), weight = 1.35f)
        OperationBodyCell(row.operationNo, weight = 1.15f)
        OperationBodyCell("${row.productCode} / ${row.productName}", weight = 2.0f)
        OperationBodyCell(row.warehouseName, weight = 1.05f)
        OperationBodyCell(row.locationName, weight = 1.15f)
        OperationBodyCell(operationQuantityFormatter.format(row.quantity), weight = 0.8f)
        OperationBodyCell(row.note.ifBlank { "-" }, weight = 1.55f)

        Box(
            modifier = Modifier.weight(0.75f),
            contentAlignment = Alignment.CenterStart
        ) {
            TextButton(onClick = onOpenDetail) {
                Text("詳細")
            }
        }
    }
}

private fun filterOperationRows(
    source: List<OperationListRowModel>,
    condition: OperationListSearchCondition,
): List<OperationListRowModel> {
    val fromDate = parseOperationDateOrNull(condition.fromDateText)
    val toDate = parseOperationDateOrNull(condition.toDateText)
    val keyword = condition.keyword.trim()
    val operationNo = condition.operationNo.trim()

    return source.asSequence()
        .filter { row ->
            fromDate == null || !row.operationAt.toLocalDate().isBefore(fromDate)
        }
        .filter { row ->
            toDate == null || !row.operationAt.toLocalDate().isAfter(toDate)
        }
        .filter { row ->
            operationNo.isBlank() || row.operationNo.contains(operationNo, ignoreCase = true)
        }
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
        .sortedByDescending { it.operationAt }
        .toList()
}
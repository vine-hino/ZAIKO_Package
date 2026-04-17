package com.vine.pc_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

data class StocktakeListRowModel(
    val stocktakeNo: String,
    val stocktakeAt: LocalDateTime,
    val productCode: String,
    val productName: String,
    val warehouseName: String,
    val locationName: String,
    val bookQuantity: Int,
    val actualQuantity: Int,
    val note: String = "",
) {
    val difference: Int
        get() = actualQuantity - bookQuantity
}

data class StocktakeListSearchCondition(
    val fromDateText: String = "",
    val toDateText: String = "",
    val keyword: String = "",
    val warehouseName: String = "",
    val locationName: String = "",
    val stocktakeNo: String = "",
)

@Composable
fun PcStocktakeManagementScreen() {
    val allRows = remember {
        listOf(
            StocktakeListRowModel(
                stocktakeNo = "ST-20260417-001",
                stocktakeAt = LocalDateTime.now().minusHours(3),
                productCode = "P-001",
                productName = "検品用ラベル",
                warehouseName = "東京倉庫",
                locationName = "A-01-01",
                bookQuantity = 120,
                actualQuantity = 118,
                note = "2枚差異"
            ),
            StocktakeListRowModel(
                stocktakeNo = "ST-20260416-004",
                stocktakeAt = LocalDateTime.now().minusDays(1),
                productCode = "P-002",
                productName = "梱包箱M",
                warehouseName = "大阪倉庫",
                locationName = "B-02-03",
                bookQuantity = 50,
                actualQuantity = 50,
                note = ""
            ),
            StocktakeListRowModel(
                stocktakeNo = "ST-20260415-002",
                stocktakeAt = LocalDateTime.now().minusDays(2),
                productCode = "P-003",
                productName = "作業用手袋",
                warehouseName = "東京倉庫",
                locationName = "A-02-04",
                bookQuantity = 10,
                actualQuantity = 8,
                note = "不足"
            )
        )
    }

    StocktakeManagementScreen(allRows = allRows)
}

@Composable
fun StocktakeManagementScreen(
    allRows: List<StocktakeListRowModel>,
) {
    val todayText = remember {
        LocalDate.now().format(operationDateFormatter)
    }

    var draftCondition by remember {
        mutableStateOf(
            StocktakeListSearchCondition(
                toDateText = todayText
            )
        )
    }

    var appliedCondition by remember {
        mutableStateOf(
            StocktakeListSearchCondition(
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
        filterStocktakeRows(allRows, appliedCondition)
    }

    val totalCount = filteredRows.size
    val diffCount = filteredRows.count { it.difference != 0 }
    val diffSum = filteredRows.sumOf { abs(it.difference) }

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
                title = "棚卸一覧",
                description = "棚卸実績を検索し、差異や確認状況を把握する画面です。",
                totalText = "該当 $totalCount 件 / 差異あり $diffCount 件",
                actionText = "新規棚卸",
                onAction = {
                    // 後で StocktakeConfirmScreen へ遷移
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

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

                    OutlinedTextField(
                        value = draftCondition.stocktakeNo,
                        onValueChange = { draftCondition = draftCondition.copy(stocktakeNo = it) },
                        modifier = Modifier.weight(1.0f),
                        label = { Text("棚卸No") },
                        placeholder = { Text("番号で検索") },
                        singleLine = true
                    )

                    DatePickerField(
                        modifier = Modifier.width(200.dp),
                        label = "棚卸日 From",
                        value = draftCondition.fromDateText,
                        isError = fromDateError,
                        onDateSelected = {
                            draftCondition = draftCondition.copy(fromDateText = it)
                        }
                    )

                    DatePickerField(
                        modifier = Modifier.width(200.dp),
                        label = "棚卸日 To",
                        value = draftCondition.toDateText,
                        isError = toDateError,
                        onDateSelected = {
                            draftCondition = draftCondition.copy(toDateText = it)
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

                    Spacer(modifier = Modifier.weight(1f))

                    CompactQuickRangeButton(
                        text = "今日",
                        onClick = {
                            val today = LocalDate.now().format(operationDateFormatter)
                            draftCondition = draftCondition.copy(
                                fromDateText = today,
                                toDateText = today
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    CompactQuickRangeButton(
                        text = "直近7日",
                        onClick = {
                            val today = LocalDate.now()
                            draftCondition = draftCondition.copy(
                                fromDateText = today.minusDays(6).format(operationDateFormatter),
                                toDateText = today.format(operationDateFormatter)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    CompactQuickRangeButton(
                        text = "直近30日",
                        onClick = {
                            val today = LocalDate.now()
                            draftCondition = draftCondition.copy(
                                fromDateText = today.minusDays(29).format(operationDateFormatter),
                                toDateText = today.format(operationDateFormatter)
                            )
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
                            draftCondition = StocktakeListSearchCondition(
                                toDateText = todayText
                            )
                            appliedCondition = StocktakeListSearchCondition(
                                toDateText = todayText
                            )
                        }
                    ) {
                        Text("クリア")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (!fromDateError && !toDateError) {
                                appliedCondition = draftCondition
                            }
                        },
                        enabled = !(fromDateError || toDateError)
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
                    title = "差異あり件数",
                    value = operationQuantityFormatter.format(diffCount),
                    subText = "差異が出ている件数",
                    backgroundColor = SummaryQtyBg
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "差異数量合計",
                    value = operationQuantityFormatter.format(diffSum),
                    subText = "差異数量の絶対値合計",
                    backgroundColor = SummaryWarehouseBg
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OperationResultCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OperationTableHeaderBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OperationHeaderCell("棚卸日時", weight = 1.25f)
                    OperationHeaderCell("棚卸No", weight = 1.1f)
                    OperationHeaderCell("商品", weight = 1.8f)
                    OperationHeaderCell("倉庫", weight = 1.0f)
                    OperationHeaderCell("ロケーション", weight = 1.1f)
                    OperationHeaderCell("帳簿数", weight = 0.75f)
                    OperationHeaderCell("実棚数", weight = 0.75f)
                    OperationHeaderCell("差異", weight = 0.7f)
                    OperationHeaderCell("備考", weight = 1.2f)
                }

                Divider()

                if (filteredRows.isEmpty()) {
                    OperationEmptyState(
                        title = "該当する棚卸データがありません",
                        description = "検索条件を変更して再度お試しください。"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(filteredRows, key = { _, row -> row.stocktakeNo }) { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) OperationRowEvenBg else OperationRowOddBg
                                    )
                                    .clickable {
                                        // 後で詳細確認 or StocktakeConfirmScreen へ遷移
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OperationBodyCell(
                                    row.stocktakeAt.format(operationDateTimeFormatter),
                                    weight = 1.25f
                                )
                                OperationBodyCell(row.stocktakeNo, weight = 1.1f)
                                OperationBodyCell("${row.productCode} / ${row.productName}", weight = 1.8f)
                                OperationBodyCell(row.warehouseName, weight = 1.0f)
                                OperationBodyCell(row.locationName, weight = 1.1f)
                                OperationBodyCell(
                                    operationQuantityFormatter.format(row.bookQuantity),
                                    weight = 0.75f
                                )
                                OperationBodyCell(
                                    operationQuantityFormatter.format(row.actualQuantity),
                                    weight = 0.75f
                                )
                                OperationBodyCell(
                                    formatDifference(row.difference),
                                    weight = 0.7f
                                )
                                OperationBodyCell(
                                    row.note.ifBlank { "-" },
                                    weight = 1.2f
                                )
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

private fun filterStocktakeRows(
    source: List<StocktakeListRowModel>,
    condition: StocktakeListSearchCondition,
): List<StocktakeListRowModel> {
    val fromDate = parseOperationDateOrNull(condition.fromDateText)
    val toDate = parseOperationDateOrNull(condition.toDateText)
    val keyword = condition.keyword.trim()
    val stocktakeNo = condition.stocktakeNo.trim()

    return source.asSequence()
        .filter { row ->
            fromDate == null || !row.stocktakeAt.toLocalDate().isBefore(fromDate)
        }
        .filter { row ->
            toDate == null || !row.stocktakeAt.toLocalDate().isAfter(toDate)
        }
        .filter { row ->
            stocktakeNo.isBlank() || row.stocktakeNo.contains(stocktakeNo, ignoreCase = true)
        }
        .filter { row ->
            keyword.isBlank() ||
                    row.productCode.contains(keyword, ignoreCase = true) ||
                    row.productName.contains(keyword, ignoreCase = true)
        }
        .filter { row ->
            condition.warehouseName.isBlank() || row.warehouseName == condition.warehouseName
        }
        .filter { row ->
            condition.locationName.isBlank() || row.locationName == condition.locationName
        }
        .sortedByDescending { it.stocktakeAt }
        .toList()
}

private fun formatDifference(value: Int): String {
    return when {
        value > 0 -> "+${operationQuantityFormatter.format(value)}"
        value < 0 -> "-${operationQuantityFormatter.format(abs(value))}"
        else -> "0"
    }
}
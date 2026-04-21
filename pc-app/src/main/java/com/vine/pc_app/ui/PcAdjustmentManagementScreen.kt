package com.vine.pc_app.ui
import com.vine.pc_app.data.PcDependencies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.RegisterStockMovementRequest
import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
import com.vine.pc_app.data.network.PcMasterLookupItem
import com.vine.pc_app.domain.OperationEmptyState
import com.vine.pc_app.domain.OperationHeaderCard
import com.vine.pc_app.domain.OperationPageBg
import com.vine.pc_app.domain.OperationResultCard
import com.vine.pc_app.domain.OperationSearchCard
import com.vine.pc_app.domain.SimpleDropdownField
import com.vine.pc_app.domain.SummaryCountBg
import com.vine.pc_app.domain.SummaryMetricCard
import com.vine.pc_app.domain.SummaryQtyBg
import com.vine.pc_app.domain.SummaryWarehouseBg
import com.vine.pc_app.domain.operationDateTimeFormatter
import com.vine.pc_app.domain.operationQuantityFormatter
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

data class AdjustmentHistoryRowModel(
    val referenceNo: String,
    val adjustedAtText: String,
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val locationCode: String,
    val quantity: Long,
    val reasonName: String,
    val note: String,
    val operatorName: String,
)

@Composable
fun PcAdjustmentManagementScreen(
    initialProductCode: String? = null,
    initialWarehouseCode: String? = null,
    initialLocationCode: String? = null,
) {
    var products by remember { mutableStateOf<List<PcMasterLookupItem>>(emptyList()) }
    var locations by remember { mutableStateOf<List<PcMasterLookupItem>>(emptyList()) }
    var reasons by remember { mutableStateOf<List<PcMasterLookupItem>>(emptyList()) }
    var allRows by remember { mutableStateOf<List<AdjustmentHistoryRowModel>>(emptyList()) }

    var selectedProductLabel by remember { mutableStateOf("") }
    var selectedLocationLabel by remember { mutableStateOf("") }
    var selectedReasonLabel by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }

    var message by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val productOptions = remember(products) {
        products.map { it.toProductLabel() }
    }
    val locationOptions = remember(locations) {
        locations.map { it.toLocationLabel() }
    }
    val reasonOptions = remember(reasons) {
        reasons.map { it.toReasonLabel() }
    }

    val selectedProduct = remember(products, selectedProductLabel) {
        products.firstOrNull { it.toProductLabel() == selectedProductLabel }
    }
    val selectedLocation = remember(locations, selectedLocationLabel) {
        locations.firstOrNull { it.toLocationLabel() == selectedLocationLabel }
    }
    val selectedReason = remember(reasons, selectedReasonLabel) {
        reasons.firstOrNull { it.toReasonLabel() == selectedReasonLabel }
    }

    val filteredRows = remember(allRows, keyword) {
        if (keyword.isBlank()) {
            allRows
        } else {
            allRows.filter { row ->
                listOf(
                    row.referenceNo,
                    row.productCode,
                    row.productName,
                    row.warehouseCode,
                    row.locationCode,
                    row.reasonName,
                    row.note,
                    row.operatorName,
                ).any { value ->
                    value.contains(keyword, ignoreCase = true)
                }
            }
        }
    }

    val totalCount = filteredRows.size
    val positiveTotal = filteredRows.filter { it.quantity > 0 }.sumOf { it.quantity }
    val negativeTotal = filteredRows.filter { it.quantity < 0 }.sumOf { it.quantity }

    suspend fun reloadMasters() {
        runCatching {
            Triple(
                PcDependencies.masterLookupClient.search(type = "PRODUCT"),
                PcDependencies.masterLookupClient.search(type = "LOCATION"),
                PcDependencies.masterLookupClient.search(type = "REASON"),
            )
        }.onSuccess { (productRows, locationRows, reasonRows) ->
            products = productRows.sortedBy { it.code }
            locations = locationRows.sortedWith(
                compareBy<PcMasterLookupItem> { it.warehouseCode.orEmpty() }
                    .thenBy { it.code }
            )
            reasons = reasonRows.sortedBy { it.code }
            if (!initialProductCode.isNullOrBlank() && selectedProductLabel.isBlank()) {
                products.firstOrNull { it.code == initialProductCode }?.let { product ->
                    selectedProductLabel = product.toProductLabel()
                }
            }
            if (!initialLocationCode.isNullOrBlank() && selectedLocationLabel.isBlank()) {
                locations.firstOrNull { location ->
                    location.code == initialLocationCode &&
                            (initialWarehouseCode.isNullOrBlank() || location.warehouseCode == initialWarehouseCode)
                }?.let { location ->
                    selectedLocationLabel = location.toLocationLabel()
                }
            }
            errorMessage = null
        }.onFailure { e ->
            errorMessage = e.message ?: "調整用マスタの取得に失敗しました"
        }
    }

    suspend fun reloadHistory() {
        runCatching {
            PcDependencies.inventoryMovementClient
                .getMovements()
                .movements
                .filter { it.operation == StockOperation.ADJUST }
                .sortedByDescending { it.occurredAt }
                .map { it.toAdjustmentHistoryRowModel() }
        }.onSuccess { rows ->
            allRows = rows
            errorMessage = null
        }.onFailure { e ->
            errorMessage = e.message ?: "在庫調整履歴の取得に失敗しました"
        }
    }

    suspend fun submitAdjustment() {
        val product = selectedProduct ?: run {
            errorMessage = "商品を選択してください"
            return
        }
        val location = selectedLocation ?: run {
            errorMessage = "ロケーションを選択してください"
            return
        }
        val reason = selectedReason ?: run {
            errorMessage = "理由を選択してください"
            return
        }
        val quantity = quantityText.toLongOrNull()?.takeIf { it != 0L } ?: run {
            errorMessage = "調整数は 0 以外で入力してください"
            return
        }

        isSubmitting = true
        errorMessage = null
        message = null

        runCatching {
            PcDependencies.inventoryMovementClient.registerMovement(
                RegisterStockMovementRequest(
                    itemId = product.code,
                    itemName = product.name,
                    quantity = quantity,
                    operation = StockOperation.ADJUST,
                    operatorName = "PC-OP001",
                    warehouseCode = location.warehouseCode.orEmpty(),
                    locationCode = location.code,
                    note = noteText.ifBlank { null },
                    adjustmentReasonCode = reason.code,
                    adjustmentReasonName = reason.name,
                )
            )
        }.onSuccess { response ->
            message = buildString {
                append("在庫調整を登録しました")
                append("\n受付番号: ")
                append(response.referenceNo)
            }
            selectedProductLabel = ""
            selectedLocationLabel = ""
            selectedReasonLabel = ""
            quantityText = ""
            noteText = ""
            reloadHistory()
        }.onFailure { e ->
            errorMessage = e.message ?: "在庫調整の登録に失敗しました"
        }

        isSubmitting = false
    }

    LaunchedEffect(Unit) {
        reloadMasters()
        reloadHistory()

        scope.launch {
            runCatching {
                PcDependencies.inventoryRealtimeClient.connect { message ->
                    if (message.movement.operation == StockOperation.ADJUST) {
                        scope.launch {
                            reloadHistory()
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = OperationPageBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OperationHeaderCard(
                title = "在庫調整",
                description = "PC から在庫調整を登録し、HT / PC を含む全調整履歴を参照します。",
                totalText = "履歴 $totalCount 件 / 増加 ${operationQuantityFormatter.format(positiveTotal)} / 減少 ${operationQuantityFormatter.format(negativeTotal)}",
                actionText = "再読込",
                onAction = {
                    scope.launch {
                        reloadMasters()
                        reloadHistory()
                    }
                },
            )

            errorMessage?.let { text ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = text,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            message?.let { text ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = text,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "調整登録",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = "商品・ロケーション・理由を選び、調整数を入力して登録します。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        SimpleDropdownField(
                            modifier = Modifier.fillMaxWidth(),
                            label = "商品",
                            selectedValue = selectedProductLabel,
                            options = productOptions,
                            onSelected = { selectedProductLabel = it },
                        )

                        SimpleDropdownField(
                            modifier = Modifier.fillMaxWidth(),
                            label = "ロケーション",
                            selectedValue = selectedLocationLabel,
                            options = locationOptions,
                            onSelected = { selectedLocationLabel = it },
                        )

                        SimpleDropdownField(
                            modifier = Modifier.fillMaxWidth(),
                            label = "理由",
                            selectedValue = selectedReasonLabel,
                            options = reasonOptions,
                            onSelected = { selectedReasonLabel = it },
                        )

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("調整数（増:+ / 減:-）") },
                            placeholder = { Text("例: 5 / -3") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("備考") },
                            minLines = 3,
                        )

                        Button(
                            onClick = { scope.launch { submitAdjustment() } },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (isSubmitting) "登録中..." else "登録")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OperationSearchCard {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("履歴検索") },
                            placeholder = { Text("受付番号 / 商品 / ロケーション / 理由 / 備考") },
                            singleLine = true,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SummaryMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "履歴件数",
                            value = operationQuantityFormatter.format(totalCount),
                            subText = "HT / PC の調整履歴",
                            backgroundColor = SummaryCountBg,
                        )
                        SummaryMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "増加合計",
                            value = operationQuantityFormatter.format(positiveTotal),
                            subText = "プラス調整の合計",
                            backgroundColor = SummaryQtyBg,
                        )
                        SummaryMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "減少合計",
                            value = operationQuantityFormatter.format(negativeTotal),
                            subText = "マイナス調整の合計",
                            backgroundColor = SummaryWarehouseBg,
                        )
                    }

                    OperationResultCard {
                        if (filteredRows.isEmpty()) {
                            OperationEmptyState(
                                title = "在庫調整履歴はありません",
                                description = "PC または HT で在庫調整を登録すると、ここに表示されます。",
                            )
                        } else {
                            AdjustmentHistoryTable(rows = filteredRows)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentHistoryTable(
    rows: List<AdjustmentHistoryRowModel>,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdjustmentHeaderCell(text = "受付番号", weight = 1.2f)
            AdjustmentHeaderCell(text = "日時", weight = 1.2f)
            AdjustmentHeaderCell(text = "商品", weight = 1.6f)
            AdjustmentHeaderCell(text = "倉庫", weight = 0.9f)
            AdjustmentHeaderCell(text = "ロケーション", weight = 1.0f)
            AdjustmentHeaderCell(text = "調整数", weight = 0.8f)
            AdjustmentHeaderCell(text = "理由", weight = 1.1f)
            AdjustmentHeaderCell(text = "備考", weight = 1.2f)
            AdjustmentHeaderCell(text = "担当", weight = 0.8f)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows, key = { _, row -> row.referenceNo }) { index, row ->
                if (index > 0) {
                    HorizontalDivider()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdjustmentBodyCell(text = row.referenceNo, weight = 1.2f)
                    AdjustmentBodyCell(text = row.adjustedAtText, weight = 1.2f)
                    AdjustmentBodyCell(
                        text = "${row.productCode} / ${row.productName}",
                        weight = 1.6f,
                    )
                    AdjustmentBodyCell(text = row.warehouseCode, weight = 0.9f)
                    AdjustmentBodyCell(text = row.locationCode, weight = 1.0f)
                    AdjustmentBodyCell(
                        text = if (row.quantity > 0) "+${row.quantity}" else row.quantity.toString(),
                        weight = 0.8f,
                        color = if (row.quantity > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    AdjustmentBodyCell(text = row.reasonName, weight = 1.1f)
                    AdjustmentBodyCell(text = row.note.ifBlank { "-" }, weight = 1.2f)
                    AdjustmentBodyCell(text = row.operatorName, weight = 0.8f)
                }
            }
        }
    }
}

@Composable
private fun RowScope.AdjustmentHeaderCell(
    text: String,
    weight: Float,
) {
    Box(modifier = Modifier.weight(weight)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RowScope.AdjustmentBodyCell(
    text: String,
    weight: Float,
    color: androidx.compose.ui.graphics.Color? = null,
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(end = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
    }
}

private fun PcMasterLookupItem.toProductLabel(): String = "$code / $name"

private fun PcMasterLookupItem.toLocationLabel(): String {
    return if (warehouseCode.isNullOrBlank()) {
        code
    } else {
        "$code / 倉庫:${warehouseCode}"
    }
}

private fun PcMasterLookupItem.toReasonLabel(): String = "$code / $name"

private fun StockMovementDto.toAdjustmentHistoryRowModel(): AdjustmentHistoryRowModel {
    val adjustedAt = runCatching {
        OffsetDateTime.parse(occurredAt).format(operationDateTimeFormatter)
    }.getOrDefault(occurredAt)

    return AdjustmentHistoryRowModel(
        referenceNo = referenceNo,
        adjustedAtText = adjustedAt,
        productCode = itemId,
        productName = itemName,
        warehouseCode = warehouseCode,
        locationCode = locationCode,
        quantity = quantity,
        reasonName = adjustmentReasonName ?: adjustmentReasonCode.orEmpty(),
        note = note.orEmpty(),
        operatorName = operatorName,
    )
}

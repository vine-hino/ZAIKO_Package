package com.vine.pc_app.ui

import com.vine.pc_app.data.PcDependencies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
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

private enum class AdjustmentDirection {
    INCREASE,
    DECREASE,
}

private enum class AdjustmentHistoryFilter {
    ALL,
    INCREASE_ONLY,
    DECREASE_ONLY,
}

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

    var direction by remember { mutableStateOf(AdjustmentDirection.INCREASE) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    var keyword by remember { mutableStateOf("") }
    var historyFilter by remember { mutableStateOf(AdjustmentHistoryFilter.ALL) }

    var message by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingMasters by remember { mutableStateOf(false) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    var selectedHistoryRow by remember { mutableStateOf<AdjustmentHistoryRowModel?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val productOptions = remember(products) { products.map { it.toProductLabel() } }
    val locationOptions = remember(locations) { locations.map { it.toLocationLabel() } }
    val reasonOptions = remember(reasons) { reasons.map { it.toReasonLabel() } }

    val selectedProduct = remember(products, selectedProductLabel) {
        products.firstOrNull { it.toProductLabel() == selectedProductLabel }
    }
    val selectedLocation = remember(locations, selectedLocationLabel) {
        locations.firstOrNull { it.toLocationLabel() == selectedLocationLabel }
    }
    val selectedReason = remember(reasons, selectedReasonLabel) {
        reasons.firstOrNull { it.toReasonLabel() == selectedReasonLabel }
    }

    val signedQuantity = remember(direction, amountText) {
        amountText.toLongOrNull()?.takeIf { it > 0L }?.let {
            if (direction == AdjustmentDirection.INCREASE) it else -it
        }
    }

    val filteredRows = remember(allRows, keyword, historyFilter) {
        allRows.filter { row ->
            val matchesKeyword = keyword.isBlank() || listOf(
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

            val matchesFilter = when (historyFilter) {
                AdjustmentHistoryFilter.ALL -> true
                AdjustmentHistoryFilter.INCREASE_ONLY -> row.quantity > 0
                AdjustmentHistoryFilter.DECREASE_ONLY -> row.quantity < 0
            }

            matchesKeyword && matchesFilter
        }
    }

    val totalCount = filteredRows.size
    val positiveTotal = filteredRows.filter { it.quantity > 0 }.sumOf { it.quantity }
    val negativeTotal = filteredRows.filter { it.quantity < 0 }.sumOf { it.quantity }

    suspend fun reloadMasters() {
        isLoadingMasters = true
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
        isLoadingMasters = false
    }

    suspend fun reloadHistory() {
        isLoadingHistory = true
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
        isLoadingHistory = false
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
        val quantity = signedQuantity ?: run {
            errorMessage = "数量を 1 以上で入力してください"
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
                append(" / 受付番号: ")
                append(response.referenceNo)
            }

            // 連続作業しやすいように商品・ロケーションは維持
            selectedReasonLabel = ""
            amountText = ""
            noteText = ""
            direction = AdjustmentDirection.INCREASE

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
                PcDependencies.inventoryRealtimeClient.connect { realtimeMessage ->
                    if (realtimeMessage.movement.operation == StockOperation.ADJUST) {
                        scope.launch { reloadHistory() }
                    }
                }
            }
        }
    }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        message = null
    }

    LaunchedEffect(errorMessage) {
        val text = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        errorMessage = null
    }

    selectedHistoryRow?.let { row ->
        AdjustmentHistoryDetailDialog(
            row = row,
            onDismiss = { selectedHistoryRow = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = OperationPageBg,
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OperationPageBg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OperationHeaderCard(
                    title = "在庫調整",
                    description = "PC から在庫調整を登録し、HT / PC を含む全調整履歴を参照します。",
                    totalText = "履歴 $totalCount 件 / 増加 ${operationQuantityFormatter.format(positiveTotal)} / 減少 ${operationQuantityFormatter.format(negativeTotal)}",
                    actionText = if (isLoadingMasters || isLoadingHistory) "再読込中..." else "再読込",
                    onAction = {
                        if (!isLoadingMasters && !isLoadingHistory) {
                            scope.launch {
                                reloadMasters()
                                reloadHistory()
                            }
                        }
                    },
                )

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val isWide = maxWidth >= 1200.dp

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AdjustmentEntryPane(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxSize(),
                                productOptions = productOptions,
                                locationOptions = locationOptions,
                                reasonOptions = reasonOptions,
                                selectedProductLabel = selectedProductLabel,
                                onProductSelected = { selectedProductLabel = it },
                                selectedLocationLabel = selectedLocationLabel,
                                onLocationSelected = { selectedLocationLabel = it },
                                selectedReasonLabel = selectedReasonLabel,
                                onReasonSelected = { selectedReasonLabel = it },
                                direction = direction,
                                onDirectionChanged = { direction = it },
                                amountText = amountText,
                                onAmountChanged = { input ->
                                    if (input.all(Char::isDigit)) {
                                        amountText = input
                                    }
                                },
                                noteText = noteText,
                                onNoteChanged = { noteText = it },
                                signedQuantity = signedQuantity,
                                selectedProduct = selectedProduct,
                                selectedLocation = selectedLocation,
                                selectedReason = selectedReason,
                                isSubmitting = isSubmitting,
                                isLoadingMasters = isLoadingMasters,
                                onSubmit = {
                                    scope.launch { submitAdjustment() }
                                },
                            )

                            AdjustmentHistoryPane(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxSize(),
                                keyword = keyword,
                                onKeywordChanged = { keyword = it },
                                historyFilter = historyFilter,
                                onHistoryFilterChanged = { historyFilter = it },
                                totalCount = totalCount,
                                positiveTotal = positiveTotal,
                                negativeTotal = negativeTotal,
                                rows = filteredRows,
                                isLoadingHistory = isLoadingHistory,
                                onRowSelected = { selectedHistoryRow = it },
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AdjustmentEntryPane(
                                modifier = Modifier.fillMaxWidth(),
                                productOptions = productOptions,
                                locationOptions = locationOptions,
                                reasonOptions = reasonOptions,
                                selectedProductLabel = selectedProductLabel,
                                onProductSelected = { selectedProductLabel = it },
                                selectedLocationLabel = selectedLocationLabel,
                                onLocationSelected = { selectedLocationLabel = it },
                                selectedReasonLabel = selectedReasonLabel,
                                onReasonSelected = { selectedReasonLabel = it },
                                direction = direction,
                                onDirectionChanged = { direction = it },
                                amountText = amountText,
                                onAmountChanged = { input ->
                                    if (input.all(Char::isDigit)) {
                                        amountText = input
                                    }
                                },
                                noteText = noteText,
                                onNoteChanged = { noteText = it },
                                signedQuantity = signedQuantity,
                                selectedProduct = selectedProduct,
                                selectedLocation = selectedLocation,
                                selectedReason = selectedReason,
                                isSubmitting = isSubmitting,
                                isLoadingMasters = isLoadingMasters,
                                onSubmit = {
                                    scope.launch { submitAdjustment() }
                                },
                            )

                            AdjustmentHistoryPane(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                keyword = keyword,
                                onKeywordChanged = { keyword = it },
                                historyFilter = historyFilter,
                                onHistoryFilterChanged = { historyFilter = it },
                                totalCount = totalCount,
                                positiveTotal = positiveTotal,
                                negativeTotal = negativeTotal,
                                rows = filteredRows,
                                isLoadingHistory = isLoadingHistory,
                                onRowSelected = { selectedHistoryRow = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentEntryPane(
    modifier: Modifier = Modifier,
    productOptions: List<String>,
    locationOptions: List<String>,
    reasonOptions: List<String>,
    selectedProductLabel: String,
    onProductSelected: (String) -> Unit,
    selectedLocationLabel: String,
    onLocationSelected: (String) -> Unit,
    selectedReasonLabel: String,
    onReasonSelected: (String) -> Unit,
    direction: AdjustmentDirection,
    onDirectionChanged: (AdjustmentDirection) -> Unit,
    amountText: String,
    onAmountChanged: (String) -> Unit,
    noteText: String,
    onNoteChanged: (String) -> Unit,
    signedQuantity: Long?,
    selectedProduct: PcMasterLookupItem?,
    selectedLocation: PcMasterLookupItem?,
    selectedReason: PcMasterLookupItem?,
    isSubmitting: Boolean,
    isLoadingMasters: Boolean,
    onSubmit: () -> Unit,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "調整登録",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "入力を迷いにくいように、増減の切替と数量入力を分けています。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isLoadingMasters) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "マスタ読み込み中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SimpleDropdownField(
                modifier = Modifier.fillMaxWidth(),
                label = "商品",
                selectedValue = selectedProductLabel,
                options = productOptions,
                onSelected = onProductSelected,
            )

            SimpleDropdownField(
                modifier = Modifier.fillMaxWidth(),
                label = "ロケーション",
                selectedValue = selectedLocationLabel,
                options = locationOptions,
                onSelected = onLocationSelected,
            )

            SimpleDropdownField(
                modifier = Modifier.fillMaxWidth(),
                label = "理由",
                selectedValue = selectedReasonLabel,
                options = reasonOptions,
                onSelected = onReasonSelected,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "増減",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (direction == AdjustmentDirection.INCREASE) {
                        Button(
                            onClick = { onDirectionChanged(AdjustmentDirection.INCREASE) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("増やす")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onDirectionChanged(AdjustmentDirection.INCREASE) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("増やす")
                        }
                    }

                    if (direction == AdjustmentDirection.DECREASE) {
                        Button(
                            onClick = { onDirectionChanged(AdjustmentDirection.DECREASE) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("減らす")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onDirectionChanged(AdjustmentDirection.DECREASE) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("減らす")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("数量") },
                placeholder = { Text("例: 5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("備考") },
                minLines = 3,
            )

            AdjustmentPreviewCard(
                selectedProduct = selectedProduct,
                selectedLocation = selectedLocation,
                selectedReason = selectedReason,
                signedQuantity = signedQuantity,
            )

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSubmitting) "登録中..." else "登録")
            }

            Text(
                text = "登録後は商品・ロケーションを維持するため、連続入力がしやすくなります。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdjustmentPreviewCard(
    selectedProduct: PcMasterLookupItem?,
    selectedLocation: PcMasterLookupItem?,
    selectedReason: PcMasterLookupItem?,
    signedQuantity: Long?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "登録内容プレビュー",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            PreviewLine("商品", selectedProduct?.let { "${it.code} / ${it.name}" } ?: "未選択")
            PreviewLine(
                "ロケーション",
                selectedLocation?.let { "${it.code} / 倉庫:${it.warehouseCode.orEmpty()}" } ?: "未選択"
            )
            PreviewLine("理由", selectedReason?.let { "${it.code} / ${it.name}" } ?: "未選択")
            PreviewLine(
                "調整数",
                signedQuantity?.let { if (it > 0) "+$it" else it.toString() } ?: "未入力"
            )
        }
    }
}

@Composable
private fun PreviewLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdjustmentHistoryPane(
    modifier: Modifier = Modifier,
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    historyFilter: AdjustmentHistoryFilter,
    onHistoryFilterChanged: (AdjustmentHistoryFilter) -> Unit,
    totalCount: Int,
    positiveTotal: Long,
    negativeTotal: Long,
    rows: List<AdjustmentHistoryRowModel>,
    isLoadingHistory: Boolean,
    onRowSelected: (AdjustmentHistoryRowModel) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OperationSearchCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = onKeywordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("履歴検索") },
                    placeholder = { Text("受付番号 / 商品 / ロケーション / 理由 / 備考 / 担当") },
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterToggleButton(
                        modifier = Modifier.weight(1f),
                        text = "すべて",
                        selected = historyFilter == AdjustmentHistoryFilter.ALL,
                        onClick = { onHistoryFilterChanged(AdjustmentHistoryFilter.ALL) },
                    )
                    FilterToggleButton(
                        modifier = Modifier.weight(1f),
                        text = "増加のみ",
                        selected = historyFilter == AdjustmentHistoryFilter.INCREASE_ONLY,
                        onClick = { onHistoryFilterChanged(AdjustmentHistoryFilter.INCREASE_ONLY) },
                    )
                    FilterToggleButton(
                        modifier = Modifier.weight(1f),
                        text = "減少のみ",
                        selected = historyFilter == AdjustmentHistoryFilter.DECREASE_ONLY,
                        onClick = { onHistoryFilterChanged(AdjustmentHistoryFilter.DECREASE_ONLY) },
                    )
                }
            }
        }

        AdjustmentSummarySection(
            totalCount = totalCount,
            positiveTotal = positiveTotal,
            negativeTotal = negativeTotal,
        )

        Box(
            modifier = Modifier.weight(1f),
        ) {
            OperationResultCard {
                when {
                    isLoadingHistory -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator()
                                Text("履歴を読み込み中です")
                            }
                        }
                    }

                    rows.isEmpty() -> {
                        OperationEmptyState(
                            title = "対象の在庫調整履歴はありません",
                            description = "検索条件を変更するか、PC または HT で在庫調整を登録するとここに表示されます。",
                        )
                    }

                    else -> {
                        AdjustmentHistoryList(
                            rows = rows,
                            onRowSelected = onRowSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSummarySection(
    totalCount: Int,
    positiveTotal: Long,
    negativeTotal: Long,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val wide = maxWidth >= 780.dp

        if (wide) {
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
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "履歴件数",
                    value = operationQuantityFormatter.format(totalCount),
                    subText = "HT / PC の調整履歴",
                    backgroundColor = SummaryCountBg,
                )
                SummaryMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "増加合計",
                    value = operationQuantityFormatter.format(positiveTotal),
                    subText = "プラス調整の合計",
                    backgroundColor = SummaryQtyBg,
                )
                SummaryMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "減少合計",
                    value = operationQuantityFormatter.format(negativeTotal),
                    subText = "マイナス調整の合計",
                    backgroundColor = SummaryWarehouseBg,
                )
            }
        }
    }
}

@Composable
private fun FilterToggleButton(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            modifier = modifier,
            onClick = onClick,
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            onClick = onClick,
        ) {
            Text(text)
        }
    }
}

@Composable
private fun AdjustmentHistoryList(
    rows: List<AdjustmentHistoryRowModel>,
    onRowSelected: (AdjustmentHistoryRowModel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = rows,
            key = { it.referenceNo },
        ) { row ->
            AdjustmentHistoryListItem(
                row = row,
                onClick = { onRowSelected(row) },
            )
        }
    }
}

@Composable
private fun AdjustmentHistoryListItem(
    row: AdjustmentHistoryRowModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = row.referenceNo,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = row.adjustedAtText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = if (row.quantity > 0) "+${row.quantity}" else row.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (row.quantity > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            HorizontalDivider()

            Text(
                text = "${row.productCode} / ${row.productName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "倉庫: ${row.warehouseCode}   ロケーション: ${row.locationCode}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "理由: ${row.reasonName}   担当: ${row.operatorName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (row.note.isNotBlank()) {
                Text(
                    text = "備考: ${row.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AdjustmentHistoryDetailDialog(
    row: AdjustmentHistoryRowModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        title = {
            Text(
                text = "調整履歴詳細",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailLine("受付番号", row.referenceNo)
                DetailLine("日時", row.adjustedAtText)
                DetailLine("商品コード", row.productCode)
                DetailLine("商品名", row.productName)
                DetailLine("倉庫", row.warehouseCode)
                DetailLine("ロケーション", row.locationCode)
                DetailLine(
                    "調整数",
                    if (row.quantity > 0) "+${row.quantity}" else row.quantity.toString()
                )
                DetailLine("理由", row.reasonName)
                DetailLine("備考", row.note.ifBlank { "-" })
                DetailLine("担当", row.operatorName)
            }
        },
    )
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
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
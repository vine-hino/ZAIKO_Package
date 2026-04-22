package com.vine.pc_app.ui
import com.vine.pc_app.data.PcDependencies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId

private val japanZoneId: ZoneId = ZoneId.of("Asia/Tokyo")

@Composable
fun PcOutboundManagementScreen() {
    var outboundRows by remember { mutableStateOf<List<OutboundRowModel>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        runCatching {
            PcDependencies.inventoryMovementClient
                .getMovements()
                .movements
                .filter { it.operation == StockOperation.OUTBOUND }
                .sortedByDescending { it.occurredAt }
                .map { it.toOutboundRowModel() }
        }.onSuccess { rows ->
            outboundRows = rows
            errorMessage = null
        }.onFailure { e ->
            errorMessage = e.message ?: "出庫一覧の取得に失敗しました"
        }
    }

    LaunchedEffect(Unit) {
        reload()

        launch {
            PcDependencies.inventoryRealtimeClient.connect { message ->
                if (message.movement.operation == StockOperation.OUTBOUND) {
                    reload()
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        errorMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        OutboundListScreen(
            allRows = outboundRows,
            onOpenDetail = { row ->
                println("open outbound detail: ${row.outboundNo}")
            },
            onCreateNew = {
                println("open outbound create")
            }
        )
    }
}

private fun StockMovementDto.toOutboundRowModel(): OutboundRowModel {
    return OutboundRowModel(
        outboundNo = referenceNo,
        outboundAt = OffsetDateTime.parse(occurredAt).atZoneSameInstant(japanZoneId).toLocalDateTime(),
        productCode = itemId,
        productName = itemName,
        barcode = null,
        warehouseName = warehouseCode,
        locationName = locationCode,
        quantity = quantity.toInt(),
        note = note.orEmpty(),
        operatorName = operatorName,
    )
}

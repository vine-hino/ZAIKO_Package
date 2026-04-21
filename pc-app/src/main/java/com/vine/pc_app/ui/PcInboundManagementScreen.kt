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

@Composable
fun PcInboundManagementScreen() {
    var inboundRows by remember { mutableStateOf<List<InboundRowModel>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        runCatching {
            PcDependencies.inventoryMovementClient
                .getMovements()
                .movements
                .filter { it.operation == StockOperation.INBOUND }
                .sortedByDescending { it.occurredAt }
                .map { it.toInboundRowModel() }
        }.onSuccess { rows ->
            inboundRows = rows
            errorMessage = null
        }.onFailure { e ->
            errorMessage = e.message ?: "入庫一覧の取得に失敗しました"
        }
    }

    LaunchedEffect(Unit) {
        reload()

        launch {
            PcDependencies.inventoryRealtimeClient.connect { message ->
                if (message.movement.operation == StockOperation.INBOUND) {
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

        InboundListScreen(
            allRows = inboundRows,
            onOpenDetail = { row ->
                println("open inbound detail: ${row.inboundNo}")
            },
            onCreateNew = {
                println("open inbound create")
            }
        )
    }
}

private fun StockMovementDto.toInboundRowModel(): InboundRowModel {
    return InboundRowModel(
        inboundNo = referenceNo,
        receivedAt = OffsetDateTime.parse(occurredAt).toLocalDateTime(),
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
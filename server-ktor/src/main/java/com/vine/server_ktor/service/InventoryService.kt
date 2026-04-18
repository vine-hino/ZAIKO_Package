package com.vine.server_ktor.service

import com.vine.inventory_contract.RegisterStockMovementRequest
import com.vine.inventory_contract.RealtimeStockMessage
import com.vine.inventory_contract.StockBalanceDto
import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
import com.vine.inventory_contract.StockSummaryDto
import com.vine.server_ktor.realtime.InventoryBroadcaster
import com.vine.server_ktor.repository.StockMovementRepository
import io.ktor.server.websocket.DefaultWebSocketServerSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class InventoryService(
    private val repository: StockMovementRepository,
    private val broadcaster: InventoryBroadcaster,
) {
    suspend fun register(request: RegisterStockMovementRequest): StockMovementDto {
        require(request.quantity > 0) {
            "quantity must be greater than 0"
        }

        val now = Instant.now()
        val movement = StockMovementDto(
            id = UUID.randomUUID().toString(),
            referenceNo = createReferenceNo(request.operation, now),
            itemId = request.itemId,
            itemName = request.itemName,
            quantity = request.quantity,
            operation = request.operation,
            operatorName = request.operatorName,
            warehouseCode = request.warehouseCode,
            locationCode = request.locationCode,
            note = request.note,
            occurredAt = now.toString(),
        )

        repository.save(movement)

        broadcaster.broadcast(
            RealtimeStockMessage(
                type = "stock_movement_registered",
                movement = movement,
            )
        )

        return movement
    }

    suspend fun getMovements(): List<StockMovementDto> {
        return repository.findAll()
    }

    suspend fun getSummary(): List<StockSummaryDto> {
        return repository.findSummary()
    }

    suspend fun getBalances(
        keyword: String?,
        warehouseCode: String?,
        locationCode: String?,
    ): List<StockBalanceDto> {
        val normalizedKeyword = keyword?.trim().orEmpty()

        return repository.findAll()
            .groupBy { movement ->
                BalanceKey(
                    productCode = movement.itemId,
                    productName = movement.itemName,
                    warehouseCode = movement.warehouseCode,
                    locationCode = movement.locationCode,
                )
            }
            .map { (key, values) ->
                val quantity = values.sumOf { movement ->
                    when (movement.operation) {
                        StockOperation.INBOUND -> movement.quantity
                        StockOperation.OUTBOUND -> -movement.quantity
                    }
                }

                val updatedAt = values
                    .maxByOrNull { it.occurredAt }
                    ?.occurredAt
                    .orEmpty()

                StockBalanceDto(
                    productCode = key.productCode,
                    productName = key.productName,
                    warehouseCode = key.warehouseCode,
                    locationCode = key.locationCode,
                    quantity = quantity,
                    updatedAt = updatedAt,
                )
            }
            .filter { it.quantity != 0L }
            .filter { row ->
                normalizedKeyword.isBlank() ||
                        row.productCode.contains(normalizedKeyword, ignoreCase = true) ||
                        row.productName.contains(normalizedKeyword, ignoreCase = true)
            }
            .filter { row ->
                warehouseCode.isNullOrBlank() || row.warehouseCode == warehouseCode
            }
            .filter { row ->
                locationCode.isNullOrBlank() || row.locationCode == locationCode
            }
            .sortedWith(
                compareBy<StockBalanceDto> { it.productCode }
                    .thenBy { it.warehouseCode }
                    .thenBy { it.locationCode }
            )
    }

    suspend fun handleRealtimeSession(session: DefaultWebSocketServerSession) {
        broadcaster.handleSession(session)
    }

    private fun createReferenceNo(
        operation: StockOperation,
        now: Instant,
    ): String {
        val prefix = when (operation) {
            StockOperation.INBOUND -> "IN"
            StockOperation.OUTBOUND -> "OUT"
        }

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(now)

        val suffix = UUID.randomUUID().toString().take(8).uppercase()

        return "$prefix-$timestamp-$suffix"
    }

    private data class BalanceKey(
        val productCode: String,
        val productName: String,
        val warehouseCode: String,
        val locationCode: String,
    )
}
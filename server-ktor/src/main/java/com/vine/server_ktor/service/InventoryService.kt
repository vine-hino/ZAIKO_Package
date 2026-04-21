package com.vine.server_ktor.service

import com.vine.inventory_contract.RegisterStockMovementRequest
import com.vine.inventory_contract.RegisterStockMoveRequest
import com.vine.inventory_contract.CancelStockMovementRequest
import com.vine.inventory_contract.CancelStockMovementResult
import com.vine.inventory_contract.RealtimeStockMessage
import com.vine.inventory_contract.StockBalanceDto
import com.vine.inventory_contract.StockMoveResult
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
        when (request.operation) {
            StockOperation.INBOUND,
            StockOperation.OUTBOUND -> {
                require(request.quantity > 0) {
                    "quantity must be greater than 0"
                }
            }
            StockOperation.ADJUST -> {
                require(request.quantity != 0L) {
                    "quantity must not be 0"
                }
            }
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
            adjustmentReasonCode = request.adjustmentReasonCode,
            adjustmentReasonName = request.adjustmentReasonName,
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

    suspend fun registerMove(request: RegisterStockMoveRequest): StockMoveResult {
        require(request.quantity > 0) {
            "quantity must be greater than 0"
        }
        require(
            request.fromWarehouseCode != request.toWarehouseCode ||
                    request.fromLocationCode != request.toLocationCode
        ) {
            "from and to location must be different"
        }

        val now = Instant.now()
        val referenceNo = createMoveReferenceNo(now)

        val outboundMovement = StockMovementDto(
            id = UUID.randomUUID().toString(),
            referenceNo = referenceNo,
            itemId = request.itemId,
            itemName = request.itemName,
            quantity = request.quantity,
            operation = StockOperation.OUTBOUND,
            operatorName = request.operatorName,
            warehouseCode = request.fromWarehouseCode,
            locationCode = request.fromLocationCode,
            note = request.note,
            occurredAt = now.toString(),
        )

        val inboundMovement = StockMovementDto(
            id = UUID.randomUUID().toString(),
            referenceNo = referenceNo,
            itemId = request.itemId,
            itemName = request.itemName,
            quantity = request.quantity,
            operation = StockOperation.INBOUND,
            operatorName = request.operatorName,
            warehouseCode = request.toWarehouseCode,
            locationCode = request.toLocationCode,
            note = request.note,
            occurredAt = now.toString(),
        )

        repository.save(outboundMovement)
        repository.save(inboundMovement)

        broadcaster.broadcast(
            RealtimeStockMessage(
                type = "stock_move_registered",
                movement = outboundMovement,
            )
        )
        broadcaster.broadcast(
            RealtimeStockMessage(
                type = "stock_move_registered",
                movement = inboundMovement,
            )
        )

        return StockMoveResult(
            accepted = true,
            message = "移動をサーバーへ登録しました",
            referenceNo = referenceNo,
            outboundMovementId = outboundMovement.id,
            inboundMovementId = inboundMovement.id,
        )
    }

    suspend fun cancel(request: CancelStockMovementRequest): CancelStockMovementResult {
        val target = repository.findAll()
            .firstOrNull { it.id == request.operationUuid }
            ?: return CancelStockMovementResult(
                accepted = false,
                message = "取消対象が見つかりません",
                operationUuid = request.operationUuid,
            )

        val now = Instant.now()
        val reverseOperation = when (target.operation) {
            StockOperation.INBOUND -> StockOperation.OUTBOUND
            StockOperation.OUTBOUND -> StockOperation.INBOUND
            StockOperation.ADJUST -> StockOperation.ADJUST
        }
        val reverseQuantity = when (target.operation) {
            StockOperation.ADJUST -> -target.quantity
            else -> target.quantity
        }

        val reverse = StockMovementDto(
            id = UUID.randomUUID().toString(),
            referenceNo = "CAN-${target.referenceNo}",
            itemId = target.itemId,
            itemName = target.itemName,
            quantity = reverseQuantity,
            operation = reverseOperation,
            operatorName = request.operatorCode,
            warehouseCode = target.warehouseCode,
            locationCode = target.locationCode,
            note = request.note ?: "CANCEL ${target.referenceNo}",
            occurredAt = now.toString(),
        )

        repository.save(reverse)
        broadcaster.broadcast(
            RealtimeStockMessage(
                type = "stock_movement_cancelled",
                movement = reverse,
            )
        )

        return CancelStockMovementResult(
            accepted = true,
            message = "取消をサーバーへ登録しました",
            referenceNo = reverse.referenceNo,
            operationUuid = reverse.id,
        )
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
                        StockOperation.ADJUST -> movement.quantity
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
            StockOperation.ADJUST -> "ADJ"
        }

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(now)

        val suffix = UUID.randomUUID().toString().take(8).uppercase()

        return "$prefix-$timestamp-$suffix"
    }

    private fun createMoveReferenceNo(now: Instant): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(now)

        val suffix = UUID.randomUUID().toString().take(8).uppercase()
        return "MOV-$timestamp-$suffix"
    }

    private data class BalanceKey(
        val productCode: String,
        val productName: String,
        val warehouseCode: String,
        val locationCode: String,
    )
}

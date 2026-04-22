package com.vine.inventory_contract

import kotlinx.serialization.Serializable

@Serializable
enum class StockOperation {
    INBOUND,
    OUTBOUND,
    ADJUST,
}

@Serializable
data class RegisterStockMovementRequest(
    val itemId: String,
    val itemName: String,
    val quantity: Long,
    val operation: StockOperation,
    val operatorName: String,
    val warehouseCode: String,
    val locationCode: String,
    val note: String? = null,
    val adjustmentReasonCode: String? = null,
    val adjustmentReasonName: String? = null,
    val occurredAt: String? = null,
)

@Serializable
data class RegisterStockMoveRequest(
    val itemId: String,
    val itemName: String,
    val quantity: Long,
    val operatorName: String,
    val fromWarehouseCode: String,
    val fromLocationCode: String,
    val toWarehouseCode: String,
    val toLocationCode: String,
    val note: String? = null,
    val occurredAt: String? = null,
)

@Serializable
data class CancelStockMovementRequest(
    val operationUuid: String,
    val operatorCode: String,
    val note: String? = null,
)

@Serializable
data class StockMovementDto(
    val id: String,
    val referenceNo: String,
    val itemId: String,
    val itemName: String,
    val quantity: Long,
    val operation: StockOperation,
    val operatorName: String,
    val warehouseCode: String,
    val locationCode: String,
    val note: String? = null,
    val adjustmentReasonCode: String? = null,
    val adjustmentReasonName: String? = null,
    val occurredAt: String,
)

@Serializable
data class StockMovementListResponse(
    val movements: List<StockMovementDto>,
)

@Serializable
data class StockMoveResult(
    val accepted: Boolean,
    val message: String,
    val referenceNo: String,
    val outboundMovementId: String,
    val inboundMovementId: String,
)

@Serializable
data class CancelStockMovementResult(
    val accepted: Boolean,
    val message: String,
    val referenceNo: String? = null,
    val operationUuid: String,
)

@Serializable
data class StockSummaryDto(
    val itemId: String,
    val itemName: String,
    val currentQuantity: Long,
)

@Serializable
data class StockSummaryListResponse(
    val items: List<StockSummaryDto>,
)

@Serializable
data class RealtimeStockMessage(
    val type: String,
    val movement: StockMovementDto,
)

@Serializable
data class StockBalanceDto(
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val locationCode: String,
    val quantity: Long,
    val updatedAt: String,
)

@Serializable
data class StockBalanceListResponse(
    val items: List<StockBalanceDto>,
)

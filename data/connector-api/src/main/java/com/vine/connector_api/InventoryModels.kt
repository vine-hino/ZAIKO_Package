package com.vine.connector_api

enum class OperationKind {
    INBOUND,
    OUTBOUND,
    MOVE,
    STOCKTAKE,
    ADJUST,
}

data class StockQuery(
    val productCode: String? = null,
    val barcode: String? = null,
    val warehouseCode: String? = null,
    val locationCode: String? = null,
    val locationScanCode: String? = null,
    val limit: Int = 200,
)

data class StockItem(
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val warehouseName: String,
    val locationCode: String,
    val locationName: String,
    val quantity: Long,
)

data class StockHistoryQuery(
    val productCode: String? = null,
    val warehouseCode: String? = null,
    val locationCode: String? = null,
    val limit: Int = 200,
)

data class StockHistoryItem(
    val operationUuid: String,
    val operationType: String,
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val locationCode: String,
    val deltaQuantity: Long,
    val beforeQuantity: Long? = null,
    val afterQuantity: Long? = null,
    val operatedAtEpochMillis: Long,
    val operatorCode: String,
    val operatorName: String,
    val note: String? = null,
)

data class InboundCommand(
    val productCode: String,
    val productName: String,
    val toWarehouseCode: String,
    val toLocationCode: String,
    val quantity: Long,
    val operatorCode: String,
    val deviceId: String? = null,
    val note: String? = null,
    val externalDocNo: String? = null,
    val inboundPlanId: String? = null,
)

data class OutboundCommand(
    val productCode: String,
    val productName: String,
    val fromWarehouseCode: String,
    val fromLocationCode: String,
    val quantity: Long,
    val operatorCode: String,
    val deviceId: String? = null,
    val note: String? = null,
)

data class MoveCommand(
    val productCode: String,
    val fromWarehouseCode: String,
    val fromLocationCode: String,
    val toWarehouseCode: String,
    val toLocationCode: String,
    val quantity: Long,
    val operatorCode: String,
    val deviceId: String? = null,
    val note: String? = null,
)

data class StocktakeLineCommand(
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val locationCode: String,
    val actualQuantity: Long,
)

data class StocktakeCommand(
    val stocktakeDate: String, // YYYY-MM-DD
    val operatorCode: String,
    val warehouseCode: String? = null,
    val deviceId: String? = null,
    val note: String? = null,
    val lines: List<StocktakeLineCommand>,
)

data class AdjustmentCommand(
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val locationCode: String,
    val adjustQuantity: Long,
    val reasonCode: String,
    val reasonName: String,
    val operatorCode: String,
    val deviceId: String? = null,
    val note: String? = null,
)

data class CancelOperationCommand(
    val operationUuid: String,
    val operationType: OperationKind,
    val operatorCode: String,
    val note: String? = null,
)
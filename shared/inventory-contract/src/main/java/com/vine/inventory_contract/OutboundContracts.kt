package com.vine.inventory_contract

data class GetOutboundSummariesQuery(
    val warehouseCode: String? = null,
    val limit: Int = 100,
)

data class GetOutboundDetailsQuery(
    val operationUuid: String,
)

data class OutboundSummary(
    val operationUuid: String,
    val outboundNo: String,
    val operatedAtEpochMillis: Long,
    val operatorCode: String,
    val warehouseCode: String?,
    val lineCount: Int,
    val externalDocNo: String?,
    val outboundPlanId: String?,
    val note: String?,
)

data class OutboundDetail(
    val detailUuid: String,
    val operationUuid: String,
    val lineNo: Int,
    val productCode: String,
    val fromWarehouseCode: String,
    val fromLocationCode: String,
    val quantity: Long,
    val note: String?,
)
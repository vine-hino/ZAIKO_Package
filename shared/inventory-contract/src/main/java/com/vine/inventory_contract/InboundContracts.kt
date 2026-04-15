package com.vine.inventory_contract

data class GetInboundSummariesQuery(
    val warehouseCode: String? = null,
    val limit: Int = 100,
)

data class GetInboundDetailsQuery(
    val operationUuid: String,
)

data class InboundSummary(
    val operationUuid: String,
    val inboundNo: String,
    val operatedAtEpochMillis: Long,
    val operatorCode: String,
    val warehouseCode: String?,
    val lineCount: Int,
    val externalDocNo: String?,
    val inboundPlanId: String?,
    val note: String?,
)

data class InboundDetail(
    val detailUuid: String,
    val operationUuid: String,
    val lineNo: Int,
    val productCode: String,
    val toWarehouseCode: String,
    val toLocationCode: String,
    val quantity: Long,
    val note: String?,
)
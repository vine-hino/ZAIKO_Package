package com.vine.inventory_contract

data class InboundTransferPayload(
    val schemaVersion: Int = 1,
    val batchId: String,
    val exportedAtEpochMillis: Long,
    val sourceSystem: String = "HT",
    val sourceDeviceId: String? = null,
    val header: InboundTransferHeader,
    val details: List<InboundTransferDetail>,
)

data class InboundTransferHeader(
    val operationUuid: String,
    val inboundNo: String,
    val operatedAtEpochMillis: Long,
    val operatorCode: String,
    val warehouseCode: String?,
    val externalDocNo: String? = null,
    val inboundPlanId: String? = null,
    val note: String? = null,
)

data class InboundTransferDetail(
    val detailUuid: String,
    val operationUuid: String,
    val lineNo: Int,
    val productCode: String,
    val toWarehouseCode: String,
    val toLocationCode: String,
    val quantity: Long,
    val note: String? = null,
)
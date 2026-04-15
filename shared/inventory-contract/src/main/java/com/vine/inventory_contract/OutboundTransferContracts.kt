package com.vine.inventory_contract

data class OutboundTransferPayload(
    val schemaVersion: Int = 1,
    val batchId: String,
    val exportedAtEpochMillis: Long,
    val sourceSystem: String = "HT",
    val sourceDeviceId: String? = null,
    val header: OutboundTransferHeader,
    val details: List<OutboundTransferDetail>,
)

data class OutboundTransferHeader(
    val operationUuid: String,
    val outboundNo: String,
    val operatedAtEpochMillis: Long,
    val operatorCode: String,
    val warehouseCode: String?,
    val externalDocNo: String? = null,
    val outboundPlanId: String? = null,
    val note: String? = null,
)

data class OutboundTransferDetail(
    val detailUuid: String,
    val operationUuid: String,
    val lineNo: Int,
    val productCode: String,
    val fromWarehouseCode: String,
    val fromLocationCode: String,
    val quantity: Long,
    val note: String? = null,
)
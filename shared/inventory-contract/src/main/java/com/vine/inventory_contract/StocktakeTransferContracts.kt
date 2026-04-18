package com.vine.inventory_contract

import kotlinx.serialization.Serializable

@Serializable
data class StocktakeTransferPayload(
    val schemaVersion: Int = 1,
    val batchId: String,
    val exportedAtEpochMillis: Long,
    val sourceSystem: String = "HT",
    val sourceDeviceId: String? = null,
    val header: StocktakeTransferHeader,
    val details: List<StocktakeTransferDetail>,
)

@Serializable
data class StocktakeTransferHeader(
    val operationUuid: String,
    val stocktakeNo: String,
    val stocktakeDate: String,
    val warehouseCode: String?,
    val status: String,
    val enteredByCode: String,
    val note: String? = null,
)

@Serializable
data class StocktakeTransferDetail(
    val detailUuid: String,
    val operationUuid: String,
    val lineNo: Int,
    val productCode: String,
    val warehouseCode: String,
    val locationCode: String,
    val bookQuantity: Long,
    val actualQuantity: Long,
    val diffQuantity: Long,
    val countedAtEpochMillis: Long,
    val countedByCode: String,
)
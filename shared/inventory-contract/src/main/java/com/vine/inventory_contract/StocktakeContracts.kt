package com.vine.inventory_contract

import kotlinx.serialization.Serializable

@Serializable
data class SaveStocktakeDraftRequest(
    val stocktakeDate: String,
    val operatorCode: String,
    val warehouseCode: String,
    val productCode: String,
    val productName: String,
    val locationCode: String,
    val actualQuantity: Long,
    val note: String? = null,
)

@Serializable
data class GetStocktakeSummariesQuery(
    val status: String? = "DRAFT",
    val warehouseCode: String? = null,
    val limit: Int = 100,
)

@Serializable
data class GetStocktakeDetailsQuery(
    val operationUuid: String,
    val diffOnly: Boolean = false,
)

@Serializable
data class StocktakeSummary(
    val operationUuid: String,
    val stocktakeNo: String,
    val stocktakeDate: String,
    val warehouseCode: String?,
    val warehouseName: String?,
    val status: String,
    val lineCount: Int,
    val enteredByName: String?,
)

@Serializable
data class StocktakeDetail(
    val detailUuid: String,
    val operationUuid: String,
    val lineNo: Int,
    val productCode: String,
    val productName: String,
    val warehouseCode: String,
    val locationCode: String,
    val bookQuantity: Long,
    val actualQuantity: Long,
    val diffQuantity: Long,
)

@Serializable
data class ConfirmStocktakeCommand(
    val operationUuid: String,
    val operatorCode: String,
)

@Serializable
data class StocktakeActionResult(
    val accepted: Boolean,
    val message: String,
)
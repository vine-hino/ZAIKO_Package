package com.vine.inventory_contract

data class GetStocktakeSummariesQuery(
    val status: String? = "DRAFT",
    val warehouseCode: String? = null,
    val limit: Int = 100,
)

data class GetStocktakeDetailsQuery(
    val operationUuid: String,
    val diffOnly: Boolean = false,
)

data class StocktakeSummary(
    val operationUuid: String,
    val stocktakeNo: String,
    val stocktakeDate: String,   // YYYY-MM-DD
    val warehouseCode: String?,
    val warehouseName: String?,
    val status: String,          // DRAFT / CONFIRMED
    val lineCount: Int,
    val enteredByName: String?,
)

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

data class ConfirmStocktakeCommand(
    val operationUuid: String,
    val operatorCode: String,
)
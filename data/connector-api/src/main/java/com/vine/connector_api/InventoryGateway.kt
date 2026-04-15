package com.vine.connector_api

import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.GetStocktakeDetailsQuery
import com.vine.inventory_contract.GetStocktakeSummariesQuery
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary

interface InventoryGateway {
    fun currentConnectionType(): ConnectionType

    suspend fun searchStock(query: StockQuery): List<StockItem>

    suspend fun getStockHistory(query: StockHistoryQuery): List<StockHistoryItem>

    suspend fun getStocktakeSummaries(
        query: GetStocktakeSummariesQuery,
    ): List<StocktakeSummary>

    suspend fun getStocktakeDetails(
        query: GetStocktakeDetailsQuery,
    ): List<StocktakeDetail>

    suspend fun registerInbound(command: InboundCommand): SubmitResult

    suspend fun registerOutbound(command: OutboundCommand): SubmitResult

    suspend fun registerMove(command: MoveCommand): SubmitResult

    suspend fun saveStocktake(command: StocktakeCommand): SubmitResult

    suspend fun confirmStocktake(command: ConfirmStocktakeCommand): SubmitResult

    suspend fun registerAdjustment(command: AdjustmentCommand): SubmitResult

    suspend fun cancelOperation(command: CancelOperationCommand): SubmitResult

    suspend fun exportStocktakeToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String? = null,
    ): SubmitResult

    suspend fun getUnsyncedCount(): Int

    suspend fun exportInboundToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String? = null,
    ): SubmitResult

    suspend fun exportOutboundToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String? = null,
    ): SubmitResult
}
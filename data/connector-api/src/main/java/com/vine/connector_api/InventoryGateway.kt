package com.vine.connector_api

interface InventoryGateway {
    fun currentConnectionType(): ConnectionType

    suspend fun searchStock(query: StockQuery): List<StockItem>

    suspend fun getStockHistory(query: StockHistoryQuery): List<StockHistoryItem>

    suspend fun registerInbound(command: InboundCommand): SubmitResult

    suspend fun registerOutbound(command: OutboundCommand): SubmitResult

    suspend fun registerMove(command: MoveCommand): SubmitResult

    suspend fun saveStocktake(command: StocktakeCommand): SubmitResult

    suspend fun confirmStocktake(command: ConfirmStocktakeCommand): SubmitResult

    suspend fun registerAdjustment(command: AdjustmentCommand): SubmitResult

    suspend fun cancelOperation(command: CancelOperationCommand): SubmitResult

    suspend fun getUnsyncedCount(): Int
}
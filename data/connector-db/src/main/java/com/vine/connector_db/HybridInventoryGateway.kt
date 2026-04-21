package com.vine.connector_db

import com.vine.connector_api.AdjustmentCommand
import com.vine.connector_api.ConnectionType
import com.vine.connector_api.InboundCommand
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.MoveCommand
import com.vine.connector_api.OutboundCommand
import com.vine.connector_api.StockHistoryItem
import com.vine.connector_api.StockHistoryQuery
import com.vine.connector_api.StockItem
import com.vine.connector_api.StockQuery
import com.vine.connector_api.StocktakeCommand
import com.vine.connector_api.SubmitResult
import com.vine.database.ZaikoDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridInventoryGateway @Inject constructor(
    private val localGateway: DbInventoryGateway,
    private val inboundServerClient: InboundServerClient,
    private val outboundServerClient: OutboundServerClient,
    private val moveServerClient: MoveServerClient,
    private val stockBalanceServerClient: StockBalanceServerClient,
    private val stockHistoryServerClient: StockHistoryServerClient,
    private val adjustmentServerClient: AdjustmentServerClient,
    private val stocktakeServerClient: StocktakeServerClient,
    private val database: ZaikoDatabase,
) : InventoryGateway by localGateway {

    override fun currentConnectionType(): ConnectionType = ConnectionType.CLOUD

    override suspend fun registerInbound(command: InboundCommand): SubmitResult {
        return inboundServerClient.registerInbound(
            productCode = command.productCode,
            productName = command.productName,
            warehouseCode = command.toWarehouseCode,
            locationCode = command.toLocationCode,
            quantity = command.quantity,
            operatorCode = command.operatorCode,
            note = command.note,
        )
    }

    override suspend fun registerOutbound(command: OutboundCommand): SubmitResult {
        return outboundServerClient.registerOutbound(
            productCode = command.productCode,
            productName = command.productName,
            warehouseCode = command.fromWarehouseCode,
            locationCode = command.fromLocationCode,
            quantity = command.quantity,
            operatorCode = command.operatorCode,
            note = command.note,
        )
    }

    override suspend fun saveStocktake(command: StocktakeCommand): SubmitResult {
        val line = command.lines.firstOrNull()
            ?: return SubmitResult(
                accepted = false,
                message = "棚卸明細がありません",
            )

        return stocktakeServerClient.saveDraft(
            command = command,
            productName = line.productName,
        )
    }

    override suspend fun registerAdjustment(command: AdjustmentCommand): SubmitResult {
        return adjustmentServerClient.registerAdjustment(
            productCode = command.productCode,
            productName = command.productName,
            warehouseCode = command.warehouseCode,
            locationCode = command.locationCode,
            adjustQuantity = command.adjustQuantity,
            reasonCode = command.reasonCode,
            reasonName = command.reasonName,
            operatorCode = command.operatorCode,
            note = command.note,
        )
    }

    override suspend fun registerMove(command: MoveCommand): SubmitResult {
        return moveServerClient.registerMove(
            productCode = command.productCode,
            productName = command.productName,
            fromWarehouseCode = command.fromWarehouseCode,
            fromLocationCode = command.fromLocationCode,
            toWarehouseCode = command.toWarehouseCode,
            toLocationCode = command.toLocationCode,
            quantity = command.quantity,
            operatorCode = command.operatorCode,
            note = command.note,
        )
    }

    override suspend fun searchStock(query: StockQuery): List<StockItem> {
        return stockBalanceServerClient.searchStock(query)
    }

    override suspend fun getStockHistory(query: StockHistoryQuery): List<StockHistoryItem> {
        return stockHistoryServerClient.getStockHistory(query)
    }

    override suspend fun exportInboundToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String?,
    ): SubmitResult {
        return SubmitResult(
            accepted = false,
            message = "サーバー連携モードでは JSON 書き出しを使用しません",
        )
    }

    override suspend fun exportOutboundToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String?,
    ): SubmitResult {
        return SubmitResult(
            accepted = false,
            message = "サーバー連携モードでは JSON 書き出しを使用しません",
        )
    }

    override suspend fun exportStocktakeToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String?,
    ): SubmitResult {
        return SubmitResult(
            accepted = false,
            message = "サーバー連携モードでは JSON 書き出しを使用しません",
        )
    }
}

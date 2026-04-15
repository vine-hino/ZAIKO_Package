package com.vine.connector_db

import androidx.room.withTransaction
import com.vine.connector_api.AdjustmentCommand
import com.vine.connector_api.CancelOperationCommand
import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.connector_api.ConnectionType
import com.vine.connector_api.InboundCommand
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.MoveCommand
import com.vine.connector_api.OperationKind
import com.vine.connector_api.OutboundCommand
import com.vine.connector_api.StockHistoryItem
import com.vine.connector_api.StockHistoryQuery
import com.vine.connector_api.StockItem
import com.vine.connector_api.StockQuery
import com.vine.connector_api.StocktakeCommand
import com.vine.connector_api.SubmitResult
import com.vine.database.ZaikoDatabase
import com.vine.database.entity.AdjustmentDetailEntity
import com.vine.database.entity.AdjustmentHeaderEntity
import com.vine.database.entity.InboundDetailEntity
import com.vine.database.entity.InboundHeaderEntity
import com.vine.database.entity.MoveDetailEntity
import com.vine.database.entity.MoveHeaderEntity
import com.vine.database.entity.OutboundDetailEntity
import com.vine.database.entity.OutboundHeaderEntity
import com.vine.database.entity.StockHistoryEntity
import com.vine.database.entity.StocktakeDetailEntity
import com.vine.database.entity.StocktakeHeaderEntity
import com.vine.database.entity.SyncQueueEntity
import com.vine.database.model.OperationType
import com.vine.database.model.StocktakeStatus
import com.vine.database.model.SyncStatus
import com.vine.database.model.TerminalType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import com.vine.inventory_contract.GetStocktakeDetailsQuery
import com.vine.inventory_contract.GetStocktakeSummariesQuery
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import java.io.File

@Singleton
class DbInventoryGateway @Inject constructor(
    private val database: ZaikoDatabase,
    private val stocktakeJsonExporter: StocktakeJsonExporter,
    private val inboundJsonExporter: InboundJsonExporter,
    private val outboundJsonExporter: OutboundJsonExporter,
) : InventoryGateway {

    private val productDao get() = database.productDao()
    private val warehouseDao get() = database.warehouseDao()
    private val stockDao get() = database.stockDao()
    private val inboundDao get() = database.inboundDao()
    private val outboundDao get() = database.outboundDao()
    private val moveDao get() = database.moveDao()
    private val stocktakeDao get() = database.stocktakeDao()
    private val adjustmentDao get() = database.adjustmentDao()
    private val syncQueueDao get() = database.syncQueueDao()

    override fun currentConnectionType(): ConnectionType = ConnectionType.DIRECT_DB

    override suspend fun getStocktakeSummaries(
        query: GetStocktakeSummariesQuery,
    ): List<StocktakeSummary> {
        val warehouseId = query.warehouseCode
            ?.takeIf { it.isNotBlank() }
            ?.let { warehouseDao.findWarehouseByCode(it)?.id }

        val headers = stocktakeDao.findHeaders(
            status = query.status?.takeIf { it.isNotBlank() },
            warehouseId = warehouseId,
            limit = query.limit,
        )

        return headers.map { header ->
            val warehouse = header.warehouseId?.let { warehouseDao.findWarehouseById(it) }
            val operator = warehouseDao.findOperatorById(header.enteredBy)
            val lineCount = stocktakeDao.countDetailsByStocktakeId(header.id)

            StocktakeSummary(
                operationUuid = header.operationUuid,
                stocktakeNo = header.stocktakeNo,
                stocktakeDate = header.stocktakeDate,
                warehouseCode = warehouse?.warehouseCode,
                warehouseName = warehouse?.warehouseName,
                status = header.status.name,
                lineCount = lineCount,
                enteredByName = operator?.operatorName,
            )
        }
    }

    override suspend fun getStocktakeDetails(
        query: GetStocktakeDetailsQuery,
    ): List<StocktakeDetail> {
        val stocktake = stocktakeDao.findStocktakeByOperationUuid(query.operationUuid)
            ?: return emptyList()

        val details = stocktake.details.mapNotNull { detail ->
            val product = productDao.findProductById(detail.productId) ?: return@mapNotNull null
            val warehouse = warehouseDao.findWarehouseById(detail.warehouseId) ?: return@mapNotNull null
            val location = warehouseDao.findLocationById(detail.locationId) ?: return@mapNotNull null

            StocktakeDetail(
                detailUuid = detail.detailUuid,
                operationUuid = stocktake.header.operationUuid,
                lineNo = detail.lineNo,
                productCode = product.productCode,
                productName = product.productName,
                warehouseCode = warehouse.warehouseCode,
                locationCode = location.locationCode,
                bookQuantity = detail.bookQuantity,
                actualQuantity = detail.actualQuantity,
                diffQuantity = detail.diffQuantity,
            )
        }

        return if (query.diffOnly) {
            details.filter { it.diffQuantity != 0L }
        } else {
            details
        }
    }

    override suspend fun searchStock(query: StockQuery): List<StockItem> {
        val productCode = query.productCode?.takeIf { it.isNotBlank() }
        val barcode = query.barcode?.takeIf { it.isNotBlank() }
        val warehouseCode = query.warehouseCode?.takeIf { it.isNotBlank() }
        val locationCode = query.locationCode?.takeIf { it.isNotBlank() }
        val locationScanCode = query.locationScanCode?.takeIf { it.isNotBlank() }

        val productId = when {
            productCode != null -> productDao.findProductByCode(productCode)?.id
            barcode != null -> productDao.findProductByBarcode(barcode)?.id
            else -> null
        }

        val warehouseId = warehouseCode
            ?.let { warehouseDao.findWarehouseByCode(it)?.id }

        val locationId = when {
            locationScanCode != null ->
                warehouseDao.findLocationByScanCode(locationScanCode)?.id
            locationCode != null && warehouseId != null ->
                warehouseDao.findLocationByWarehouseAndCode(warehouseId, locationCode)?.id
            else -> null
        }

        return stockDao
            .observeStockBalances(productId, warehouseId, locationId)
            .first()
            .take(query.limit)
            .map {
                StockItem(
                    productCode = it.product.productCode,
                    productName = it.product.productName,
                    warehouseCode = it.warehouse.warehouseCode,
                    warehouseName = it.warehouse.warehouseName,
                    locationCode = it.location.locationCode,
                    locationName = it.location.locationName,
                    quantity = it.balance.quantity,
                )
            }
    }

    override suspend fun getStockHistory(query: StockHistoryQuery): List<StockHistoryItem> {
        val productId = query.productCode
            ?.takeIf { it.isNotBlank() }
            ?.let { productDao.findProductByCode(it)?.id }

        val warehouseId = query.warehouseCode
            ?.takeIf { it.isNotBlank() }
            ?.let { warehouseDao.findWarehouseByCode(it)?.id }

        val locationId = query.locationCode
            ?.takeIf { it.isNotBlank() && warehouseId != null }
            ?.let { warehouseDao.findLocationByWarehouseAndCode(warehouseId!!, it)?.id }

        return stockDao
            .observeStockHistories(productId, warehouseId, locationId, query.limit)
            .first()
            .mapNotNull { history ->
                val product = productDao.findProductById(history.productId) ?: return@mapNotNull null
                val warehouse = warehouseDao.findWarehouseById(history.warehouseId) ?: return@mapNotNull null
                val location = warehouseDao.findLocationById(history.locationId) ?: return@mapNotNull null
                val operator = warehouseDao.findOperatorById(history.operatorId) ?: return@mapNotNull null

                StockHistoryItem(
                    operationUuid = history.operationUuid,
                    operationType = history.operationType.name,
                    productCode = product.productCode,
                    productName = product.productName,
                    warehouseCode = warehouse.warehouseCode,
                    locationCode = location.locationCode,
                    deltaQuantity = history.deltaQuantity,
                    beforeQuantity = history.beforeQuantity,
                    afterQuantity = history.afterQuantity,
                    operatedAtEpochMillis = history.operatedAtEpochMillis,
                    operatorCode = operator.operatorCode,
                    operatorName = operator.operatorName,
                    note = history.note,
                )
            }
    }

    override suspend fun registerInbound(command: InboundCommand): SubmitResult {
        return runCatching {
            validatePositiveQuantity(command.quantity)

            val product = requireProduct(command.productCode)
            val warehouse = requireWarehouse(command.toWarehouseCode)
            val location = requireLocation(warehouse.id, command.toLocationCode)
            val operator = requireOperator(command.operatorCode)

            val now = System.currentTimeMillis()
            val operationUuid = UUID.randomUUID().toString()
            val detailUuid = UUID.randomUUID().toString()
            val inboundNo = createReferenceNo("IN", now)

            database.withTransaction {
                val headerId = inboundDao.insertHeader(
                    InboundHeaderEntity(
                        operationUuid = operationUuid,
                        inboundNo = inboundNo,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        deviceId = command.deviceId,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        externalDocNo = command.externalDocNo,
                        inboundPlanId = command.inboundPlanId,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )

                inboundDao.insertDetails(
                    listOf(
                        InboundDetailEntity(
                            detailUuid = detailUuid,
                            inboundId = headerId,
                            lineNo = 1,
                            productId = product.id,
                            toWarehouseId = warehouse.id,
                            toLocationId = location.id,
                            quantity = command.quantity,
                            note = command.note,
                        ),
                    ),
                )

                val before = stockDao.findStockBalance(product.id, warehouse.id, location.id)?.quantity ?: 0L
                val after = before + command.quantity

                stockDao.upsertStockBalance(
                    productId = product.id,
                    warehouseId = warehouse.id,
                    locationId = location.id,
                    deltaQuantity = command.quantity,
                    operationType = OperationType.INBOUND,
                    operationUuid = operationUuid,
                    operatedAtEpochMillis = now,
                )

                stockDao.insertStockHistory(
                    StockHistoryEntity(
                        historyUuid = UUID.randomUUID().toString(),
                        operationType = OperationType.INBOUND,
                        operationUuid = operationUuid,
                        operationDetailUuid = detailUuid,
                        productId = product.id,
                        warehouseId = warehouse.id,
                        locationId = location.id,
                        deltaQuantity = command.quantity,
                        beforeQuantity = before,
                        afterQuantity = after,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                    ),
                )

                insertSyncQueue(
                    operationType = OperationType.INBOUND,
                    operationUuid = operationUuid,
                    payloadJson = """{"type":"INBOUND","referenceNo":"$inboundNo"}""",
                    createdAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "入庫を登録しました",
                referenceId = inboundNo,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "入庫登録に失敗しました",
            )
        }
    }

    override suspend fun registerOutbound(command: OutboundCommand): SubmitResult {
        return runCatching {
            validatePositiveQuantity(command.quantity)

            val product = requireProduct(command.productCode)
            val warehouse = requireWarehouse(command.fromWarehouseCode)
            val location = requireLocation(warehouse.id, command.fromLocationCode)
            val operator = requireOperator(command.operatorCode)

            val current = stockDao.findStockBalance(product.id, warehouse.id, location.id)
            val before = current?.quantity ?: 0L
            if (before < command.quantity) {
                throw IllegalArgumentException("在庫不足です")
            }

            val now = System.currentTimeMillis()
            val operationUuid = UUID.randomUUID().toString()
            val detailUuid = UUID.randomUUID().toString()
            val outboundNo = createReferenceNo("OUT", now)
            val after = before - command.quantity

            database.withTransaction {
                val headerId = outboundDao.insertHeader(
                    OutboundHeaderEntity(
                        operationUuid = operationUuid,
                        outboundNo = outboundNo,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        deviceId = command.deviceId,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )

                outboundDao.insertDetails(
                    listOf(
                        OutboundDetailEntity(
                            detailUuid = detailUuid,
                            outboundId = headerId,
                            lineNo = 1,
                            productId = product.id,
                            fromWarehouseId = warehouse.id,
                            fromLocationId = location.id,
                            stockQuantityBefore = before,
                            quantity = command.quantity,
                            note = command.note,
                        ),
                    ),
                )

                stockDao.upsertStockBalance(
                    productId = product.id,
                    warehouseId = warehouse.id,
                    locationId = location.id,
                    deltaQuantity = -command.quantity,
                    operationType = OperationType.OUTBOUND,
                    operationUuid = operationUuid,
                    operatedAtEpochMillis = now,
                )

                stockDao.insertStockHistory(
                    StockHistoryEntity(
                        historyUuid = UUID.randomUUID().toString(),
                        operationType = OperationType.OUTBOUND,
                        operationUuid = operationUuid,
                        operationDetailUuid = detailUuid,
                        productId = product.id,
                        warehouseId = warehouse.id,
                        locationId = location.id,
                        deltaQuantity = -command.quantity,
                        beforeQuantity = before,
                        afterQuantity = after,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                    ),
                )

                insertSyncQueue(
                    operationType = OperationType.OUTBOUND,
                    operationUuid = operationUuid,
                    payloadJson = """{"type":"OUTBOUND","referenceNo":"$outboundNo"}""",
                    createdAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "出庫を登録しました",
                referenceId = outboundNo,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "出庫登録に失敗しました",
            )
        }
    }

    override suspend fun registerMove(command: MoveCommand): SubmitResult {
        return runCatching {
            validatePositiveQuantity(command.quantity)

            val product = requireProduct(command.productCode)
            val fromWarehouse = requireWarehouse(command.fromWarehouseCode)
            val fromLocation = requireLocation(fromWarehouse.id, command.fromLocationCode)
            val toWarehouse = requireWarehouse(command.toWarehouseCode)
            val toLocation = requireLocation(toWarehouse.id, command.toLocationCode)
            val operator = requireOperator(command.operatorCode)

            if (fromWarehouse.id == toWarehouse.id && fromLocation.id == toLocation.id) {
                throw IllegalArgumentException("移動元と移動先が同じです")
            }

            val fromCurrent = stockDao.findStockBalance(product.id, fromWarehouse.id, fromLocation.id)
            val fromBefore = fromCurrent?.quantity ?: 0L
            if (fromBefore < command.quantity) {
                throw IllegalArgumentException("移動元の在庫不足です")
            }

            val toCurrent = stockDao.findStockBalance(product.id, toWarehouse.id, toLocation.id)
            val toBefore = toCurrent?.quantity ?: 0L

            val now = System.currentTimeMillis()
            val operationUuid = UUID.randomUUID().toString()
            val detailUuid = UUID.randomUUID().toString()
            val moveNo = createReferenceNo("MOV", now)

            database.withTransaction {
                val headerId = moveDao.insertHeader(
                    MoveHeaderEntity(
                        operationUuid = operationUuid,
                        moveNo = moveNo,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        deviceId = command.deviceId,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )

                moveDao.insertDetails(
                    listOf(
                        MoveDetailEntity(
                            detailUuid = detailUuid,
                            moveId = headerId,
                            lineNo = 1,
                            productId = product.id,
                            fromWarehouseId = fromWarehouse.id,
                            fromLocationId = fromLocation.id,
                            toWarehouseId = toWarehouse.id,
                            toLocationId = toLocation.id,
                            quantity = command.quantity,
                            note = command.note,
                        ),
                    ),
                )

                stockDao.upsertStockBalance(
                    productId = product.id,
                    warehouseId = fromWarehouse.id,
                    locationId = fromLocation.id,
                    deltaQuantity = -command.quantity,
                    operationType = OperationType.MOVE_OUT,
                    operationUuid = operationUuid,
                    operatedAtEpochMillis = now,
                )

                stockDao.upsertStockBalance(
                    productId = product.id,
                    warehouseId = toWarehouse.id,
                    locationId = toLocation.id,
                    deltaQuantity = command.quantity,
                    operationType = OperationType.MOVE_IN,
                    operationUuid = operationUuid,
                    operatedAtEpochMillis = now,
                )

                stockDao.insertStockHistories(
                    listOf(
                        StockHistoryEntity(
                            historyUuid = UUID.randomUUID().toString(),
                            operationType = OperationType.MOVE_OUT,
                            operationUuid = operationUuid,
                            operationDetailUuid = detailUuid,
                            productId = product.id,
                            warehouseId = fromWarehouse.id,
                            locationId = fromLocation.id,
                            deltaQuantity = -command.quantity,
                            beforeQuantity = fromBefore,
                            afterQuantity = fromBefore - command.quantity,
                            operatedAtEpochMillis = now,
                            operatorId = operator.id,
                            terminalType = TerminalType.HT,
                            note = command.note,
                            syncStatus = SyncStatus.PENDING,
                            createdAtEpochMillis = now,
                        ),
                        StockHistoryEntity(
                            historyUuid = UUID.randomUUID().toString(),
                            operationType = OperationType.MOVE_IN,
                            operationUuid = operationUuid,
                            operationDetailUuid = detailUuid,
                            productId = product.id,
                            warehouseId = toWarehouse.id,
                            locationId = toLocation.id,
                            deltaQuantity = command.quantity,
                            beforeQuantity = toBefore,
                            afterQuantity = toBefore + command.quantity,
                            operatedAtEpochMillis = now,
                            operatorId = operator.id,
                            terminalType = TerminalType.HT,
                            note = command.note,
                            syncStatus = SyncStatus.PENDING,
                            createdAtEpochMillis = now,
                        ),
                    ),
                )

                insertSyncQueue(
                    operationType = OperationType.MOVE_OUT,
                    operationUuid = operationUuid,
                    payloadJson = """{"type":"MOVE","referenceNo":"$moveNo"}""",
                    createdAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "移動を登録しました",
                referenceId = moveNo,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "移動登録に失敗しました",
            )
        }
    }

    override suspend fun saveStocktake(command: StocktakeCommand): SubmitResult {
        return runCatching {
            if (command.lines.isEmpty()) {
                throw IllegalArgumentException("棚卸明細がありません")
            }

            val operator = requireOperator(command.operatorCode)
            val headerWarehouseId = command.warehouseCode
                ?.takeIf { it.isNotBlank() }
                ?.let { requireWarehouse(it).id }

            val now = System.currentTimeMillis()
            val operationUuid = UUID.randomUUID().toString()
            val stocktakeNo = createReferenceNo("STK", now)

            database.withTransaction {
                val headerId = stocktakeDao.insertHeader(
                    StocktakeHeaderEntity(
                        operationUuid = operationUuid,
                        stocktakeNo = stocktakeNo,
                        stocktakeDate = command.stocktakeDate,
                        warehouseId = headerWarehouseId,
                        status = StocktakeStatus.DRAFT,
                        enteredBy = operator.id,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )

                val details = command.lines.mapIndexed { index, line ->
                    val product = requireProduct(line.productCode)
                    val warehouse = requireWarehouse(line.warehouseCode)
                    val location = requireLocation(warehouse.id, line.locationCode)
                    val current = stockDao.findStockBalance(product.id, warehouse.id, location.id)

                    StocktakeDetailEntity(
                        detailUuid = UUID.randomUUID().toString(),
                        stocktakeId = headerId,
                        lineNo = index + 1,
                        productId = product.id,
                        warehouseId = warehouse.id,
                        locationId = location.id,
                        bookQuantity = current?.quantity ?: 0L,
                        actualQuantity = line.actualQuantity,
                        diffQuantity = line.actualQuantity - (current?.quantity ?: 0L),
                        countedAtEpochMillis = now,
                        countedBy = operator.id,
                        isReflected = false,
                    )
                }

                stocktakeDao.insertDetails(details)

                insertSyncQueue(
                    operationType = OperationType.STOCKTAKE,
                    operationUuid = operationUuid,
                    payloadJson = """{"type":"STOCKTAKE","referenceNo":"$stocktakeNo"}""",
                    createdAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "棚卸を保存しました",
                referenceId = stocktakeNo,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "棚卸保存に失敗しました",
            )
        }
    }

    override suspend fun confirmStocktake(command: ConfirmStocktakeCommand): SubmitResult {
        return runCatching {
            val operator = requireOperator(command.operatorCode)
            val stocktake = stocktakeDao.findStocktakeByOperationUuid(command.operationUuid)
                ?: throw IllegalArgumentException("棚卸データが見つかりません")

            if (stocktake.header.status == StocktakeStatus.CONFIRMED) {
                throw IllegalArgumentException("すでに確定済みです")
            }

            val now = System.currentTimeMillis()

            database.withTransaction {
                stocktake.details.forEach { detail ->
                    if (detail.diffQuantity != 0L) {
                        stockDao.upsertStockBalance(
                            productId = detail.productId,
                            warehouseId = detail.warehouseId,
                            locationId = detail.locationId,
                            deltaQuantity = detail.diffQuantity,
                            operationType = OperationType.STOCKTAKE,
                            operationUuid = stocktake.header.operationUuid,
                            operatedAtEpochMillis = now,
                        )

                        stockDao.insertStockHistory(
                            StockHistoryEntity(
                                historyUuid = UUID.randomUUID().toString(),
                                operationType = OperationType.STOCKTAKE,
                                operationUuid = stocktake.header.operationUuid,
                                operationDetailUuid = detail.detailUuid,
                                productId = detail.productId,
                                warehouseId = detail.warehouseId,
                                locationId = detail.locationId,
                                deltaQuantity = detail.diffQuantity,
                                beforeQuantity = detail.bookQuantity,
                                afterQuantity = detail.actualQuantity,
                                operatedAtEpochMillis = now,
                                operatorId = operator.id,
                                terminalType = TerminalType.PC,
                                note = stocktake.header.note,
                                syncStatus = SyncStatus.PENDING,
                                createdAtEpochMillis = now,
                            ),
                        )
                    }
                }

                stocktakeDao.markDetailsReflected(stocktake.header.id)
                stocktakeDao.confirmStocktake(
                    id = stocktake.header.id,
                    confirmedBy = operator.id,
                    confirmedAt = now,
                    updatedAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "棚卸を確定しました",
                referenceId = stocktake.header.stocktakeNo,
                operationUuid = stocktake.header.operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "棚卸確定に失敗しました",
            )
        }
    }

    override suspend fun registerAdjustment(command: AdjustmentCommand): SubmitResult {
        return runCatching {
            if (command.adjustQuantity == 0L) {
                throw IllegalArgumentException("調整数は0以外で入力してください")
            }

            val product = requireProduct(command.productCode)
            val warehouse = requireWarehouse(command.warehouseCode)
            val location = requireLocation(warehouse.id, command.locationCode)
            val operator = requireOperator(command.operatorCode)
            val reason = adjustmentDao.findReasonByCode(command.reasonCode)
                ?: throw IllegalArgumentException("調整理由が見つかりません")

            val current = stockDao.findStockBalance(product.id, warehouse.id, location.id)
            val before = current?.quantity ?: 0L
            val after = before + command.adjustQuantity

            val now = System.currentTimeMillis()
            val operationUuid = UUID.randomUUID().toString()
            val detailUuid = UUID.randomUUID().toString()
            val adjustmentNo = createReferenceNo("ADJ", now)

            database.withTransaction {
                val headerId = adjustmentDao.insertHeader(
                    AdjustmentHeaderEntity(
                        operationUuid = operationUuid,
                        adjustmentNo = adjustmentNo,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        deviceId = command.deviceId,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )

                adjustmentDao.insertDetails(
                    listOf(
                        AdjustmentDetailEntity(
                            detailUuid = detailUuid,
                            adjustmentId = headerId,
                            lineNo = 1,
                            productId = product.id,
                            warehouseId = warehouse.id,
                            locationId = location.id,
                            stockQuantityBefore = before,
                            adjustQuantity = command.adjustQuantity,
                            stockQuantityAfter = after,
                            adjustmentReasonId = reason.id,
                            note = command.note,
                        ),
                    ),
                )

                stockDao.upsertStockBalance(
                    productId = product.id,
                    warehouseId = warehouse.id,
                    locationId = location.id,
                    deltaQuantity = command.adjustQuantity,
                    operationType = OperationType.ADJUST,
                    operationUuid = operationUuid,
                    operatedAtEpochMillis = now,
                )

                stockDao.insertStockHistory(
                    StockHistoryEntity(
                        historyUuid = UUID.randomUUID().toString(),
                        operationType = OperationType.ADJUST,
                        operationUuid = operationUuid,
                        operationDetailUuid = detailUuid,
                        productId = product.id,
                        warehouseId = warehouse.id,
                        locationId = location.id,
                        deltaQuantity = command.adjustQuantity,
                        beforeQuantity = before,
                        afterQuantity = after,
                        operatedAtEpochMillis = now,
                        operatorId = operator.id,
                        terminalType = TerminalType.HT,
                        reasonCode = command.reasonCode,
                        note = command.note,
                        syncStatus = SyncStatus.PENDING,
                        createdAtEpochMillis = now,
                    ),
                )

                insertSyncQueue(
                    operationType = OperationType.ADJUST,
                    operationUuid = operationUuid,
                    payloadJson = """{"type":"ADJUST","referenceNo":"$adjustmentNo"}""",
                    createdAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "在庫調整を登録しました",
                referenceId = adjustmentNo,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "在庫調整に失敗しました",
            )
        }
    }

    override suspend fun cancelOperation(command: CancelOperationCommand): SubmitResult {
        return runCatching {
            val operator = requireOperator(command.operatorCode)
            val histories = stockDao.findHistoriesByOperationUuid(command.operationUuid)
            if (histories.isEmpty()) {
                throw IllegalArgumentException("取消対象が見つかりません")
            }

            val now = System.currentTimeMillis()

            database.withTransaction {
                histories.forEach { history ->
                    val reverseDelta = -history.deltaQuantity

                    stockDao.upsertStockBalance(
                        productId = history.productId,
                        warehouseId = history.warehouseId,
                        locationId = history.locationId,
                        deltaQuantity = reverseDelta,
                        operationType = OperationType.CANCEL,
                        operationUuid = command.operationUuid,
                        operatedAtEpochMillis = now,
                    )

                    val current = stockDao.findStockBalance(
                        history.productId,
                        history.warehouseId,
                        history.locationId,
                    )
                    val after = current?.quantity ?: 0L
                    val before = after - reverseDelta

                    stockDao.insertStockHistory(
                        StockHistoryEntity(
                            historyUuid = UUID.randomUUID().toString(),
                            operationType = OperationType.CANCEL,
                            operationUuid = command.operationUuid,
                            operationDetailUuid = history.operationDetailUuid,
                            productId = history.productId,
                            warehouseId = history.warehouseId,
                            locationId = history.locationId,
                            deltaQuantity = reverseDelta,
                            beforeQuantity = before,
                            afterQuantity = after,
                            operatedAtEpochMillis = now,
                            operatorId = operator.id,
                            terminalType = TerminalType.PC,
                            note = command.note,
                            reversedHistoryUuid = history.historyUuid,
                            syncStatus = SyncStatus.PENDING,
                            createdAtEpochMillis = now,
                        ),
                    )
                }

                when (command.operationType) {
                    OperationKind.INBOUND -> {
                        val inbound = inboundDao.findInboundByOperationUuid(command.operationUuid)
                            ?: throw IllegalArgumentException("入庫ヘッダが見つかりません")
                        inboundDao.markCancelled(inbound.header.id, now, operator.id, now)
                    }
                    OperationKind.OUTBOUND -> {
                        val outbound = outboundDao.findOutboundByOperationUuid(command.operationUuid)
                            ?: throw IllegalArgumentException("出庫ヘッダが見つかりません")
                        outboundDao.markCancelled(outbound.header.id, now, operator.id, now)
                    }
                    OperationKind.MOVE -> {
                        val move = moveDao.findMoveByOperationUuid(command.operationUuid)
                            ?: throw IllegalArgumentException("移動ヘッダが見つかりません")
                        moveDao.markCancelled(move.header.id, now, operator.id, now)
                    }
                    OperationKind.ADJUST -> {
                        val adjustment = adjustmentDao.findAdjustmentByOperationUuid(command.operationUuid)
                            ?: throw IllegalArgumentException("調整ヘッダが見つかりません")
                        adjustmentDao.markCancelled(adjustment.header.id, now, operator.id, now)
                    }
                    OperationKind.STOCKTAKE -> {
                        throw IllegalArgumentException("棚卸確定の取消は初版では未対応です")
                    }
                }

                insertSyncQueue(
                    operationType = OperationType.CANCEL,
                    operationUuid = command.operationUuid,
                    payloadJson = """{"type":"CANCEL","operationUuid":"${command.operationUuid}"}""",
                    createdAt = now,
                )
            }

            SubmitResult(
                accepted = true,
                message = "取消を登録しました",
                operationUuid = command.operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "取消に失敗しました",
            )
        }
    }

    override suspend fun getUnsyncedCount(): Int {
        return syncQueueDao.observeUnsyncedCount().first()
    }

    override suspend fun exportStocktakeToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String?,
    ): SubmitResult {
        return runCatching {
            val outputFile = File(outputFilePath)
            stocktakeJsonExporter.exportToFile(
                operationUuid = operationUuid,
                outputFile = outputFile,
                sourceDeviceId = sourceDeviceId,
            )

            SubmitResult(
                accepted = true,
                message = "棚卸JSONを書き出しました",
                referenceId = outputFile.absolutePath,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "棚卸JSON書き出しに失敗しました",
                operationUuid = operationUuid,
            )
        }
    }

    private suspend fun requireProduct(productCode: String) =
        productDao.findProductByCode(productCode)
            ?: throw IllegalArgumentException("商品が見つかりません: $productCode")

    private suspend fun requireWarehouse(warehouseCode: String) =
        warehouseDao.findWarehouseByCode(warehouseCode)
            ?: throw IllegalArgumentException("倉庫が見つかりません: $warehouseCode")

    private suspend fun requireLocation(warehouseId: Long, locationCode: String) =
        warehouseDao.findLocationByWarehouseAndCode(warehouseId, locationCode)
            ?: throw IllegalArgumentException("ロケーションが見つかりません: $locationCode")

    private suspend fun requireOperator(operatorCode: String) =
        warehouseDao.findOperatorByCode(operatorCode)
            ?: throw IllegalArgumentException("担当者が見つかりません: $operatorCode")

    private suspend fun insertSyncQueue(
        operationType: OperationType,
        operationUuid: String,
        payloadJson: String,
        createdAt: Long,
    ) {
        syncQueueDao.insert(
            SyncQueueEntity(
                operationType = operationType,
                operationUuid = operationUuid,
                payloadJson = payloadJson,
                syncStatus = SyncStatus.PENDING,
                retryCount = 0,
                lastErrorMessage = null,
                lastAttemptedAtEpochMillis = null,
                createdAtEpochMillis = createdAt,
            ),
        )
    }

    private fun validatePositiveQuantity(quantity: Long) {
        if (quantity <= 0L) {
            throw IllegalArgumentException("数量は1以上で入力してください")
        }
    }

    private fun createReferenceNo(prefix: String, epochMillis: Long): String {
        return "$prefix-$epochMillis"
    }

    override suspend fun exportInboundToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String?,
    ): SubmitResult {
        return runCatching {
            val outputFile = File(outputFilePath)
            inboundJsonExporter.exportToFile(
                operationUuid = operationUuid,
                outputFile = outputFile,
                sourceDeviceId = sourceDeviceId,
            )

            SubmitResult(
                accepted = true,
                message = "入庫JSONを書き出しました",
                referenceId = outputFile.absolutePath,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "入庫JSON書き出しに失敗しました",
                operationUuid = operationUuid,
            )
        }
    }

    override suspend fun exportOutboundToJson(
        operationUuid: String,
        outputFilePath: String,
        sourceDeviceId: String?,
    ): SubmitResult {
        return runCatching {
            val outputFile = java.io.File(outputFilePath)
            outboundJsonExporter.exportToFile(
                operationUuid = operationUuid,
                outputFile = outputFile,
                sourceDeviceId = sourceDeviceId,
            )

            SubmitResult(
                accepted = true,
                message = "出庫JSONを書き出しました",
                referenceId = outputFile.absolutePath,
                operationUuid = operationUuid,
            )
        }.getOrElse {
            SubmitResult(
                accepted = false,
                message = it.message ?: "出庫JSON書き出しに失敗しました",
                operationUuid = operationUuid,
            )
        }
    }
}
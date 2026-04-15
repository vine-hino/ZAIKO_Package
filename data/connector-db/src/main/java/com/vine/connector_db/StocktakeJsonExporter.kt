package com.vine.connector_db

import com.google.gson.GsonBuilder
import com.vine.database.ZaikoDatabase
import com.vine.inventory_contract.StocktakeTransferDetail
import com.vine.inventory_contract.StocktakeTransferHeader
import com.vine.inventory_contract.StocktakeTransferPayload
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StocktakeJsonExporter @Inject constructor(
    private val database: ZaikoDatabase,
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun exportToJson(
        operationUuid: String,
        sourceDeviceId: String? = null,
    ): String {
        val stocktakeDao = database.stocktakeDao()
        val productDao = database.productDao()
        val warehouseDao = database.warehouseDao()

        val stocktake = stocktakeDao.findStocktakeByOperationUuid(operationUuid)
            ?: throw IllegalArgumentException("棚卸データが見つかりません: $operationUuid")

        val headerWarehouseCode = stocktake.header.warehouseId
            ?.let { warehouseDao.findWarehouseById(it)?.warehouseCode }

        val enteredByCode = warehouseDao.findOperatorById(stocktake.header.enteredBy)?.operatorCode
            ?: throw IllegalStateException("棚卸入力者が見つかりません")

        val details = stocktake.details.map { detail ->
            val productCode = productDao.findProductById(detail.productId)?.productCode
                ?: throw IllegalStateException("商品が見つかりません: productId=${detail.productId}")

            val warehouseCode = warehouseDao.findWarehouseById(detail.warehouseId)?.warehouseCode
                ?: throw IllegalStateException("倉庫が見つかりません: warehouseId=${detail.warehouseId}")

            val locationCode = warehouseDao.findLocationById(detail.locationId)?.locationCode
                ?: throw IllegalStateException("ロケーションが見つかりません: locationId=${detail.locationId}")

            val countedByCode = warehouseDao.findOperatorById(detail.countedBy)?.operatorCode
                ?: throw IllegalStateException("棚卸明細入力者が見つかりません")

            StocktakeTransferDetail(
                detailUuid = detail.detailUuid,
                operationUuid = stocktake.header.operationUuid,
                lineNo = detail.lineNo,
                productCode = productCode,
                warehouseCode = warehouseCode,
                locationCode = locationCode,
                bookQuantity = detail.bookQuantity,
                actualQuantity = detail.actualQuantity,
                diffQuantity = detail.diffQuantity,
                countedAtEpochMillis = detail.countedAtEpochMillis,
                countedByCode = countedByCode,
            )
        }

        val payload = StocktakeTransferPayload(
            batchId = UUID.randomUUID().toString(),
            exportedAtEpochMillis = System.currentTimeMillis(),
            sourceSystem = "HT",
            sourceDeviceId = sourceDeviceId,
            header = StocktakeTransferHeader(
                operationUuid = stocktake.header.operationUuid,
                stocktakeNo = stocktake.header.stocktakeNo,
                stocktakeDate = stocktake.header.stocktakeDate,
                warehouseCode = headerWarehouseCode,
                status = stocktake.header.status.name,
                enteredByCode = enteredByCode,
                note = stocktake.header.note,
            ),
            details = details,
        )

        return gson.toJson(payload)
    }

    suspend fun exportToFile(
        operationUuid: String,
        outputFile: File,
        sourceDeviceId: String? = null,
    ) {
        val json = exportToJson(
            operationUuid = operationUuid,
            sourceDeviceId = sourceDeviceId,
        )
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(json, Charsets.UTF_8)
    }
}
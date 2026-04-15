package com.vine.connector_db

import com.google.gson.GsonBuilder
import com.vine.database.ZaikoDatabase
import com.vine.inventory_contract.OutboundTransferDetail
import com.vine.inventory_contract.OutboundTransferHeader
import com.vine.inventory_contract.OutboundTransferPayload
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboundJsonExporter @Inject constructor(
    private val database: ZaikoDatabase,
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun exportToJson(
        operationUuid: String,
        sourceDeviceId: String? = null,
    ): String {
        val outboundDao = database.outboundDao()
        val productDao = database.productDao()
        val warehouseDao = database.warehouseDao()

        val outbound = outboundDao.findOutboundByOperationUuid(operationUuid)
            ?: throw IllegalArgumentException("出庫データが見つかりません: $operationUuid")

        val operatorCode = warehouseDao.findOperatorById(outbound.header.operatorId)?.operatorCode
            ?: throw IllegalStateException("担当者が見つかりません")

        val details = outbound.details.map { detail ->
            val productCode = productDao.findProductById(detail.productId)?.productCode
                ?: throw IllegalStateException("商品が見つかりません: productId=${detail.productId}")

            val fromWarehouseCode = warehouseDao.findWarehouseById(detail.fromWarehouseId)?.warehouseCode
                ?: throw IllegalStateException("倉庫が見つかりません: warehouseId=${detail.fromWarehouseId}")

            val fromLocationCode = warehouseDao.findLocationById(detail.fromLocationId)?.locationCode
                ?: throw IllegalStateException("ロケーションが見つかりません: locationId=${detail.fromLocationId}")

            OutboundTransferDetail(
                detailUuid = detail.detailUuid,
                operationUuid = outbound.header.operationUuid,
                lineNo = detail.lineNo,
                productCode = productCode,
                fromWarehouseCode = fromWarehouseCode,
                fromLocationCode = fromLocationCode,
                quantity = detail.quantity,
                note = detail.note,
            )
        }

        val headerWarehouseCode = details.firstOrNull()?.fromWarehouseCode

        val payload = OutboundTransferPayload(
            batchId = UUID.randomUUID().toString(),
            exportedAtEpochMillis = System.currentTimeMillis(),
            sourceSystem = "HT",
            sourceDeviceId = sourceDeviceId,
            header = OutboundTransferHeader(
                operationUuid = outbound.header.operationUuid,
                outboundNo = outbound.header.outboundNo,
                operatedAtEpochMillis = outbound.header.operatedAtEpochMillis,
                operatorCode = operatorCode,
                warehouseCode = headerWarehouseCode,
                externalDocNo = null,
                outboundPlanId = null,
                note = outbound.header.note,
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
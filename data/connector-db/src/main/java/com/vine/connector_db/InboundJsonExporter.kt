package com.vine.connector_db

import com.google.gson.GsonBuilder
import com.vine.database.ZaikoDatabase
import com.vine.inventory_contract.InboundTransferDetail
import com.vine.inventory_contract.InboundTransferHeader
import com.vine.inventory_contract.InboundTransferPayload
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboundJsonExporter @Inject constructor(
    private val database: ZaikoDatabase,
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun exportToJson(
        operationUuid: String,
        sourceDeviceId: String? = null,
    ): String {
        val inboundDao = database.inboundDao()
        val productDao = database.productDao()
        val warehouseDao = database.warehouseDao()

        val inbound = inboundDao.findInboundByOperationUuid(operationUuid)
            ?: throw IllegalArgumentException("入庫データが見つかりません: $operationUuid")

        val operatorCode = warehouseDao.findOperatorById(inbound.header.operatorId)?.operatorCode
            ?: throw IllegalStateException("担当者が見つかりません")

        val details = inbound.details.map { detail ->
            val productCode = productDao.findProductById(detail.productId)?.productCode
                ?: throw IllegalStateException("商品が見つかりません: productId=${detail.productId}")

            val toWarehouseCode = warehouseDao.findWarehouseById(detail.toWarehouseId)?.warehouseCode
                ?: throw IllegalStateException("倉庫が見つかりません: warehouseId=${detail.toWarehouseId}")

            val toLocationCode = warehouseDao.findLocationById(detail.toLocationId)?.locationCode
                ?: throw IllegalStateException("ロケーションが見つかりません: locationId=${detail.toLocationId}")

            InboundTransferDetail(
                detailUuid = detail.detailUuid,
                operationUuid = inbound.header.operationUuid,
                lineNo = detail.lineNo,
                productCode = productCode,
                toWarehouseCode = toWarehouseCode,
                toLocationCode = toLocationCode,
                quantity = detail.quantity,
                note = detail.note,
            )
        }

        val headerWarehouseCode = details.firstOrNull()?.toWarehouseCode

        val payload = InboundTransferPayload(
            batchId = UUID.randomUUID().toString(),
            exportedAtEpochMillis = System.currentTimeMillis(),
            sourceSystem = "HT",
            sourceDeviceId = sourceDeviceId,
            header = InboundTransferHeader(
                operationUuid = inbound.header.operationUuid,
                inboundNo = inbound.header.inboundNo,
                operatedAtEpochMillis = inbound.header.operatedAtEpochMillis,
                operatorCode = operatorCode,
                warehouseCode = headerWarehouseCode,
                externalDocNo = inbound.header.externalDocNo,
                inboundPlanId = inbound.header.inboundPlanId,
                note = inbound.header.note,
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
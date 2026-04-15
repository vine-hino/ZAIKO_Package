package com.vine.zaiko_package.bootstrap

import androidx.room.withTransaction
import com.vine.database.ZaikoDatabase
import com.vine.database.entity.AdjustmentReasonEntity
import com.vine.database.entity.LocationEntity
import com.vine.database.entity.OperatorEntity
import com.vine.database.entity.ProductBarcodeEntity
import com.vine.database.entity.ProductEntity
import com.vine.database.entity.WarehouseEntity
import com.vine.database.model.BarcodeType
import com.vine.database.model.QuantitySignType
import com.vine.database.model.TerminalType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitialMasterSeeder @Inject constructor(
    private val database: ZaikoDatabase,
) {
    suspend fun seedIfNeeded() {
        val productDao = database.productDao()
        val warehouseDao = database.warehouseDao()
        val adjustmentDao = database.adjustmentDao()

        database.withTransaction {
            val now = System.currentTimeMillis()
            val seedUser = "system_seed"

            val wh01Id = warehouseDao.findWarehouseByCode("WH-01")?.id
                ?: warehouseDao.insertWarehouse(
                    WarehouseEntity(
                        warehouseCode = "WH-01",
                        warehouseName = "本倉庫",
                        warehouseShortName = "本倉庫",
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )

            val wh02Id = warehouseDao.findWarehouseByCode("WH-02")?.id
                ?: warehouseDao.insertWarehouse(
                    WarehouseEntity(
                        warehouseCode = "WH-02",
                        warehouseName = "予備倉庫",
                        warehouseShortName = "予備",
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )

            if (warehouseDao.findLocationByWarehouseAndCode(wh01Id, "A-01-01") == null) {
                warehouseDao.insertLocation(
                    LocationEntity(
                        warehouseId = wh01Id,
                        locationCode = "A-01-01",
                        locationName = "入庫棚 A-01-01",
                        scanCode = "LOC-WH01-A0101",
                        locationType = "NORMAL",
                        isUsable = true,
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )
            }

            if (warehouseDao.findLocationByWarehouseAndCode(wh01Id, "B-01-01") == null) {
                warehouseDao.insertLocation(
                    LocationEntity(
                        warehouseId = wh01Id,
                        locationCode = "B-01-01",
                        locationName = "移動先棚 B-01-01",
                        scanCode = "LOC-WH01-B0101",
                        locationType = "NORMAL",
                        isUsable = true,
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )
            }

            if (warehouseDao.findLocationByWarehouseAndCode(wh02Id, "A-01-01") == null) {
                warehouseDao.insertLocation(
                    LocationEntity(
                        warehouseId = wh02Id,
                        locationCode = "A-01-01",
                        locationName = "予備棚 A-01-01",
                        scanCode = "LOC-WH02-A0101",
                        locationType = "NORMAL",
                        isUsable = true,
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )
            }

            if (warehouseDao.findOperatorByCode("OP-0001") == null) {
                warehouseDao.insertOperator(
                    OperatorEntity(
                        operatorCode = "OP-0001",
                        operatorName = "テスト担当者",
                        defaultWarehouseId = wh01Id,
                        terminalType = TerminalType.HT,
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )
            }

            val productId = productDao.findProductByCode("PRD-0001")?.id
                ?: productDao.insertProduct(
                    ProductEntity(
                        productCode = "PRD-0001",
                        productName = "サンプル商品1",
                        productSpec = "標準品",
                        unitCode = "EA",
                        categoryCode = "DEFAULT",
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )

            if (productDao.findProductByBarcode("4900000000010") == null) {
                productDao.insertBarcode(
                    ProductBarcodeEntity(
                        productId = productId,
                        barcode = "4900000000010",
                        barcodeType = BarcodeType.JAN,
                        isPrimary = true,
                        isActive = true,
                        createdAtEpochMillis = now,
                        createdBy = seedUser,
                        updatedAtEpochMillis = now,
                        updatedBy = seedUser,
                    ),
                )
            }

            if (adjustmentDao.findReasonByCode("DAMAGE") == null) {
                adjustmentDao.insertReasons(
                    listOf(
                        AdjustmentReasonEntity(
                            reasonCode = "DAMAGE",
                            reasonName = "破損",
                            quantitySignType = QuantitySignType.MINUS,
                            noteRequired = false,
                            sortOrder = 10,
                            isActive = true,
                            createdAtEpochMillis = now,
                            createdBy = seedUser,
                            updatedAtEpochMillis = now,
                            updatedBy = seedUser,
                        ),
                        AdjustmentReasonEntity(
                            reasonCode = "LOSS",
                            reasonName = "紛失",
                            quantitySignType = QuantitySignType.MINUS,
                            noteRequired = false,
                            sortOrder = 20,
                            isActive = true,
                            createdAtEpochMillis = now,
                            createdBy = seedUser,
                            updatedAtEpochMillis = now,
                            updatedBy = seedUser,
                        ),
                        AdjustmentReasonEntity(
                            reasonCode = "CORRECT",
                            reasonName = "誤登録訂正",
                            quantitySignType = QuantitySignType.BOTH,
                            noteRequired = false,
                            sortOrder = 30,
                            isActive = true,
                            createdAtEpochMillis = now,
                            createdBy = seedUser,
                            updatedAtEpochMillis = now,
                            updatedBy = seedUser,
                        ),
                        AdjustmentReasonEntity(
                            reasonCode = "DIFF",
                            reasonName = "棚卸差異",
                            quantitySignType = QuantitySignType.BOTH,
                            noteRequired = false,
                            sortOrder = 40,
                            isActive = true,
                            createdAtEpochMillis = now,
                            createdBy = seedUser,
                            updatedAtEpochMillis = now,
                            updatedBy = seedUser,
                        ),
                        AdjustmentReasonEntity(
                            reasonCode = "OTHER",
                            reasonName = "その他",
                            quantitySignType = QuantitySignType.BOTH,
                            noteRequired = true,
                            sortOrder = 50,
                            isActive = true,
                            createdAtEpochMillis = now,
                            createdBy = seedUser,
                            updatedAtEpochMillis = now,
                            updatedBy = seedUser,
                        ),
                    ),
                )
            }
        }
    }
}
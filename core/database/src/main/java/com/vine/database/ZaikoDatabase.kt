package com.vine.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vine.database.converter.ZaikoTypeConverters
import com.vine.database.dao.AdjustmentDao
import com.vine.database.dao.InboundDao
import com.vine.database.dao.MoveDao
import com.vine.database.dao.OutboundDao
import com.vine.database.dao.ProductDao
import com.vine.database.dao.StockDao
import com.vine.database.dao.StocktakeDao
import com.vine.database.dao.SyncQueueDao
import com.vine.database.dao.WarehouseDao
import com.vine.database.entity.AdjustmentDetailEntity
import com.vine.database.entity.AdjustmentHeaderEntity
import com.vine.database.entity.AdjustmentReasonEntity
import com.vine.database.entity.InboundDetailEntity
import com.vine.database.entity.InboundHeaderEntity
import com.vine.database.entity.LocationEntity
import com.vine.database.entity.MoveDetailEntity
import com.vine.database.entity.MoveHeaderEntity
import com.vine.database.entity.OperatorEntity
import com.vine.database.entity.OutboundDetailEntity
import com.vine.database.entity.OutboundHeaderEntity
import com.vine.database.entity.ProductBarcodeEntity
import com.vine.database.entity.ProductEntity
import com.vine.database.entity.StockBalanceEntity
import com.vine.database.entity.StockHistoryEntity
import com.vine.database.entity.StocktakeDetailEntity
import com.vine.database.entity.StocktakeHeaderEntity
import com.vine.database.entity.SyncQueueEntity
import com.vine.database.entity.WarehouseEntity

@Database(
    entities = [
        ProductEntity::class,
        ProductBarcodeEntity::class,
        WarehouseEntity::class,
        LocationEntity::class,
        AdjustmentReasonEntity::class,
        OperatorEntity::class,
        StockBalanceEntity::class,
        StockHistoryEntity::class,
        InboundHeaderEntity::class,
        InboundDetailEntity::class,
        OutboundHeaderEntity::class,
        OutboundDetailEntity::class,
        MoveHeaderEntity::class,
        MoveDetailEntity::class,
        StocktakeHeaderEntity::class,
        StocktakeDetailEntity::class,
        AdjustmentHeaderEntity::class,
        AdjustmentDetailEntity::class,
        SyncQueueEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(ZaikoTypeConverters::class)
abstract class ZaikoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun stockDao(): StockDao
    abstract fun inboundDao(): InboundDao
    abstract fun outboundDao(): OutboundDao
    abstract fun moveDao(): MoveDao
    abstract fun stocktakeDao(): StocktakeDao
    abstract fun adjustmentDao(): AdjustmentDao
    abstract fun syncQueueDao(): SyncQueueDao
}
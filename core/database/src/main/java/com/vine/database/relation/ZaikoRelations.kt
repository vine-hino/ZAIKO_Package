package com.vine.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.vine.database.entity.AdjustmentDetailEntity
import com.vine.database.entity.AdjustmentHeaderEntity
import com.vine.database.entity.LocationEntity
import com.vine.database.entity.MoveDetailEntity
import com.vine.database.entity.MoveHeaderEntity
import com.vine.database.entity.OperatorEntity
import com.vine.database.entity.OutboundDetailEntity
import com.vine.database.entity.OutboundHeaderEntity
import com.vine.database.entity.ProductBarcodeEntity
import com.vine.database.entity.ProductEntity
import com.vine.database.entity.StockBalanceEntity
import com.vine.database.entity.StocktakeDetailEntity
import com.vine.database.entity.StocktakeHeaderEntity
import com.vine.database.entity.WarehouseEntity
import com.vine.database.entity.InboundDetailEntity
import com.vine.database.entity.InboundHeaderEntity

data class ProductWithBarcodes(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "product_id",
    )
    val barcodes: List<ProductBarcodeEntity>,
)

data class WarehouseWithLocations(
    @Embedded val warehouse: WarehouseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "warehouse_id",
    )
    val locations: List<LocationEntity>,
)

data class InboundWithDetails(
    @Embedded val header: InboundHeaderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "inbound_id",
    )
    val details: List<InboundDetailEntity>,
)

data class OutboundWithDetails(
    @Embedded val header: OutboundHeaderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "outbound_id",
    )
    val details: List<OutboundDetailEntity>,
)

data class MoveWithDetails(
    @Embedded val header: MoveHeaderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "move_id",
    )
    val details: List<MoveDetailEntity>,
)

data class StocktakeWithDetails(
    @Embedded val header: StocktakeHeaderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "stocktake_id",
    )
    val details: List<StocktakeDetailEntity>,
)

data class AdjustmentWithDetails(
    @Embedded val header: AdjustmentHeaderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "adjustment_id",
    )
    val details: List<AdjustmentDetailEntity>,
)

data class StockBalanceWithMaster(
    @Embedded val balance: StockBalanceEntity,
    @Relation(
        parentColumn = "product_id",
        entityColumn = "id",
    )
    val product: ProductEntity,
    @Relation(
        parentColumn = "warehouse_id",
        entityColumn = "id",
    )
    val warehouse: WarehouseEntity,
    @Relation(
        parentColumn = "location_id",
        entityColumn = "id",
    )
    val location: LocationEntity,
)

data class StockHistoryListItem(
    @Embedded val product: ProductEntity,
    @Embedded(prefix = "operator_")
    val operator: OperatorEntity,
)
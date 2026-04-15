package com.vine.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vine.database.model.BarcodeType
import com.vine.database.model.OperationType
import com.vine.database.model.QuantitySignType
import com.vine.database.model.StocktakeStatus
import com.vine.database.model.SyncStatus
import com.vine.database.model.TerminalType

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["product_code"], unique = true),
        Index(value = ["product_name"]),
        Index(value = ["category_code"]),
    ],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "product_code")
    val productCode: String,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "product_spec")
    val productSpec: String? = null,
    @ColumnInfo(name = "unit_code")
    val unitCode: String,
    @ColumnInfo(name = "category_code")
    val categoryCode: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String,
)

@Entity(
    tableName = "product_barcodes",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["barcode"], unique = true),
    ],
)
data class ProductBarcodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "barcode")
    val barcode: String,
    @ColumnInfo(name = "barcode_type")
    val barcodeType: BarcodeType? = null,
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String,
)

@Entity(
    tableName = "warehouses",
    indices = [
        Index(value = ["warehouse_code"], unique = true),
        Index(value = ["warehouse_name"]),
    ],
)
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "warehouse_code")
    val warehouseCode: String,
    @ColumnInfo(name = "warehouse_name")
    val warehouseName: String,
    @ColumnInfo(name = "warehouse_short_name")
    val warehouseShortName: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String,
)

@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["warehouse_id"]),
        Index(value = ["warehouse_id", "location_code"], unique = true),
        Index(value = ["scan_code"], unique = true),
    ],
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: Long,
    @ColumnInfo(name = "location_code")
    val locationCode: String,
    @ColumnInfo(name = "location_name")
    val locationName: String,
    @ColumnInfo(name = "scan_code")
    val scanCode: String,
    @ColumnInfo(name = "location_type")
    val locationType: String? = null,
    @ColumnInfo(name = "is_usable")
    val isUsable: Boolean = true,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String,
)

@Entity(
    tableName = "adjustment_reasons",
    indices = [
        Index(value = ["reason_code"], unique = true),
        Index(value = ["sort_order"]),
    ],
)
data class AdjustmentReasonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "reason_code")
    val reasonCode: String,
    @ColumnInfo(name = "reason_name")
    val reasonName: String,
    @ColumnInfo(name = "quantity_sign_type")
    val quantitySignType: QuantitySignType,
    @ColumnInfo(name = "note_required")
    val noteRequired: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String,
)

@Entity(
    tableName = "operators",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["default_warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["operator_code"], unique = true),
        Index(value = ["default_warehouse_id"]),
    ],
)
data class OperatorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operator_code")
    val operatorCode: String,
    @ColumnInfo(name = "operator_name")
    val operatorName: String,
    @ColumnInfo(name = "default_warehouse_id")
    val defaultWarehouseId: Long? = null,
    @ColumnInfo(name = "terminal_type")
    val terminalType: TerminalType? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String,
)

@Entity(
    tableName = "stock_balances",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["warehouse_id"]),
        Index(value = ["location_id"]),
        Index(value = ["product_id", "warehouse_id", "location_id"], unique = true),
    ],
)
data class StockBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: Long,
    @ColumnInfo(name = "location_id")
    val locationId: Long,
    @ColumnInfo(name = "quantity")
    val quantity: Long,
    @ColumnInfo(name = "last_operation_type")
    val lastOperationType: OperationType? = null,
    @ColumnInfo(name = "last_operation_uuid")
    val lastOperationUuid: String? = null,
    @ColumnInfo(name = "last_updated_at_epoch_millis")
    val lastUpdatedAtEpochMillis: Long,
    @ColumnInfo(name = "row_version")
    val rowVersion: Long = 0L,
)

@Entity(
    tableName = "stock_histories",
    indices = [
        Index(value = ["operation_type"]),
        Index(value = ["operation_uuid"]),
        Index(value = ["product_id"]),
        Index(value = ["warehouse_id"]),
        Index(value = ["location_id"]),
        Index(value = ["operated_at_epoch_millis"]),
        Index(value = ["sync_status"]),
    ],
)
data class StockHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "history_uuid")
    val historyUuid: String,
    @ColumnInfo(name = "operation_type")
    val operationType: OperationType,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "operation_detail_uuid")
    val operationDetailUuid: String? = null,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: Long,
    @ColumnInfo(name = "location_id")
    val locationId: Long,
    @ColumnInfo(name = "delta_quantity")
    val deltaQuantity: Long,
    @ColumnInfo(name = "before_quantity")
    val beforeQuantity: Long? = null,
    @ColumnInfo(name = "after_quantity")
    val afterQuantity: Long? = null,
    @ColumnInfo(name = "operated_at_epoch_millis")
    val operatedAtEpochMillis: Long,
    @ColumnInfo(name = "operator_id")
    val operatorId: Long,
    @ColumnInfo(name = "terminal_type")
    val terminalType: TerminalType,
    @ColumnInfo(name = "reason_code")
    val reasonCode: String? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "reversed_history_uuid")
    val reversedHistoryUuid: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "inbound_headers",
    indices = [
        Index(value = ["operation_uuid"], unique = true),
        Index(value = ["inbound_no"], unique = true),
        Index(value = ["operator_id"]),
        Index(value = ["operated_at_epoch_millis"]),
        Index(value = ["sync_status"]),
    ],
)
data class InboundHeaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "inbound_no")
    val inboundNo: String,
    @ColumnInfo(name = "operated_at_epoch_millis")
    val operatedAtEpochMillis: Long,
    @ColumnInfo(name = "operator_id")
    val operatorId: Long,
    @ColumnInfo(name = "terminal_type")
    val terminalType: TerminalType,
    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "external_doc_no")
    val externalDocNo: String? = null,
    @ColumnInfo(name = "inbound_plan_id")
    val inboundPlanId: String? = null,
    @ColumnInfo(name = "is_cancelled")
    val isCancelled: Boolean = false,
    @ColumnInfo(name = "cancelled_at_epoch_millis")
    val cancelledAtEpochMillis: Long? = null,
    @ColumnInfo(name = "cancelled_by")
    val cancelledBy: Long? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "inbound_details",
    foreignKeys = [
        ForeignKey(
            entity = InboundHeaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["inbound_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["detail_uuid"], unique = true),
        Index(value = ["inbound_id", "line_no"], unique = true),
        Index(value = ["product_id"]),
        Index(value = ["to_warehouse_id"]),
        Index(value = ["to_location_id"]),
    ],
)
data class InboundDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "detail_uuid")
    val detailUuid: String,
    @ColumnInfo(name = "inbound_id")
    val inboundId: Long,
    @ColumnInfo(name = "line_no")
    val lineNo: Int,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "to_warehouse_id")
    val toWarehouseId: Long,
    @ColumnInfo(name = "to_location_id")
    val toLocationId: Long,
    @ColumnInfo(name = "quantity")
    val quantity: Long,
    @ColumnInfo(name = "note")
    val note: String? = null,
)

@Entity(
    tableName = "outbound_headers",
    indices = [
        Index(value = ["operation_uuid"], unique = true),
        Index(value = ["outbound_no"], unique = true),
        Index(value = ["operator_id"]),
        Index(value = ["operated_at_epoch_millis"]),
        Index(value = ["sync_status"]),
    ],
)
data class OutboundHeaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "outbound_no")
    val outboundNo: String,
    @ColumnInfo(name = "operated_at_epoch_millis")
    val operatedAtEpochMillis: Long,
    @ColumnInfo(name = "operator_id")
    val operatorId: Long,
    @ColumnInfo(name = "terminal_type")
    val terminalType: TerminalType,
    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "is_cancelled")
    val isCancelled: Boolean = false,
    @ColumnInfo(name = "cancelled_at_epoch_millis")
    val cancelledAtEpochMillis: Long? = null,
    @ColumnInfo(name = "cancelled_by")
    val cancelledBy: Long? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "outbound_details",
    foreignKeys = [
        ForeignKey(
            entity = OutboundHeaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["outbound_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["detail_uuid"], unique = true),
        Index(value = ["outbound_id", "line_no"], unique = true),
        Index(value = ["product_id"]),
        Index(value = ["from_warehouse_id"]),
        Index(value = ["from_location_id"]),
    ],
)
data class OutboundDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "detail_uuid")
    val detailUuid: String,
    @ColumnInfo(name = "outbound_id")
    val outboundId: Long,
    @ColumnInfo(name = "line_no")
    val lineNo: Int,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "from_warehouse_id")
    val fromWarehouseId: Long,
    @ColumnInfo(name = "from_location_id")
    val fromLocationId: Long,
    @ColumnInfo(name = "stock_quantity_before")
    val stockQuantityBefore: Long? = null,
    @ColumnInfo(name = "quantity")
    val quantity: Long,
    @ColumnInfo(name = "note")
    val note: String? = null,
)

@Entity(
    tableName = "move_headers",
    indices = [
        Index(value = ["operation_uuid"], unique = true),
        Index(value = ["move_no"], unique = true),
        Index(value = ["operator_id"]),
        Index(value = ["operated_at_epoch_millis"]),
        Index(value = ["sync_status"]),
    ],
)
data class MoveHeaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "move_no")
    val moveNo: String,
    @ColumnInfo(name = "operated_at_epoch_millis")
    val operatedAtEpochMillis: Long,
    @ColumnInfo(name = "operator_id")
    val operatorId: Long,
    @ColumnInfo(name = "terminal_type")
    val terminalType: TerminalType,
    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "is_cancelled")
    val isCancelled: Boolean = false,
    @ColumnInfo(name = "cancelled_at_epoch_millis")
    val cancelledAtEpochMillis: Long? = null,
    @ColumnInfo(name = "cancelled_by")
    val cancelledBy: Long? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "move_details",
    foreignKeys = [
        ForeignKey(
            entity = MoveHeaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["move_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["detail_uuid"], unique = true),
        Index(value = ["move_id", "line_no"], unique = true),
        Index(value = ["product_id"]),
        Index(value = ["from_warehouse_id"]),
        Index(value = ["from_location_id"]),
        Index(value = ["to_warehouse_id"]),
        Index(value = ["to_location_id"]),
    ],
)
data class MoveDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "detail_uuid")
    val detailUuid: String,
    @ColumnInfo(name = "move_id")
    val moveId: Long,
    @ColumnInfo(name = "line_no")
    val lineNo: Int,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "from_warehouse_id")
    val fromWarehouseId: Long,
    @ColumnInfo(name = "from_location_id")
    val fromLocationId: Long,
    @ColumnInfo(name = "to_warehouse_id")
    val toWarehouseId: Long,
    @ColumnInfo(name = "to_location_id")
    val toLocationId: Long,
    @ColumnInfo(name = "quantity")
    val quantity: Long,
    @ColumnInfo(name = "note")
    val note: String? = null,
)

@Entity(
    tableName = "stocktake_headers",
    indices = [
        Index(value = ["operation_uuid"], unique = true),
        Index(value = ["stocktake_no"], unique = true),
        Index(value = ["stocktake_date"]),
        Index(value = ["warehouse_id"]),
        Index(value = ["status"]),
    ],
)
data class StocktakeHeaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "stocktake_no")
    val stocktakeNo: String,
    @ColumnInfo(name = "stocktake_date")
    val stocktakeDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: Long? = null,
    @ColumnInfo(name = "status")
    val status: StocktakeStatus = StocktakeStatus.DRAFT,
    @ColumnInfo(name = "entered_by")
    val enteredBy: Long,
    @ColumnInfo(name = "confirmed_by")
    val confirmedBy: Long? = null,
    @ColumnInfo(name = "confirmed_at_epoch_millis")
    val confirmedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "stocktake_details",
    foreignKeys = [
        ForeignKey(
            entity = StocktakeHeaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["stocktake_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["detail_uuid"], unique = true),
        Index(value = ["stocktake_id", "line_no"], unique = true),
        Index(value = ["product_id"]),
        Index(value = ["warehouse_id"]),
        Index(value = ["location_id"]),
        Index(value = ["is_reflected"]),
    ],
)
data class StocktakeDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "detail_uuid")
    val detailUuid: String,
    @ColumnInfo(name = "stocktake_id")
    val stocktakeId: Long,
    @ColumnInfo(name = "line_no")
    val lineNo: Int,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: Long,
    @ColumnInfo(name = "location_id")
    val locationId: Long,
    @ColumnInfo(name = "book_quantity")
    val bookQuantity: Long,
    @ColumnInfo(name = "actual_quantity")
    val actualQuantity: Long,
    @ColumnInfo(name = "diff_quantity")
    val diffQuantity: Long,
    @ColumnInfo(name = "counted_at_epoch_millis")
    val countedAtEpochMillis: Long,
    @ColumnInfo(name = "counted_by")
    val countedBy: Long,
    @ColumnInfo(name = "is_reflected")
    val isReflected: Boolean = false,
)

@Entity(
    tableName = "adjustment_headers",
    indices = [
        Index(value = ["operation_uuid"], unique = true),
        Index(value = ["adjustment_no"], unique = true),
        Index(value = ["operator_id"]),
        Index(value = ["operated_at_epoch_millis"]),
        Index(value = ["sync_status"]),
    ],
)
data class AdjustmentHeaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "adjustment_no")
    val adjustmentNo: String,
    @ColumnInfo(name = "operated_at_epoch_millis")
    val operatedAtEpochMillis: Long,
    @ColumnInfo(name = "operator_id")
    val operatorId: Long,
    @ColumnInfo(name = "terminal_type")
    val terminalType: TerminalType,
    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "is_cancelled")
    val isCancelled: Boolean = false,
    @ColumnInfo(name = "cancelled_at_epoch_millis")
    val cancelledAtEpochMillis: Long? = null,
    @ColumnInfo(name = "cancelled_by")
    val cancelledBy: Long? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "adjustment_details",
    foreignKeys = [
        ForeignKey(
            entity = AdjustmentHeaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["adjustment_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouse_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AdjustmentReasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["adjustment_reason_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["detail_uuid"], unique = true),
        Index(value = ["adjustment_id", "line_no"], unique = true),
        Index(value = ["product_id"]),
        Index(value = ["warehouse_id"]),
        Index(value = ["location_id"]),
        Index(value = ["adjustment_reason_id"]),
    ],
)
data class AdjustmentDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "detail_uuid")
    val detailUuid: String,
    @ColumnInfo(name = "adjustment_id")
    val adjustmentId: Long,
    @ColumnInfo(name = "line_no")
    val lineNo: Int,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: Long,
    @ColumnInfo(name = "location_id")
    val locationId: Long,
    @ColumnInfo(name = "stock_quantity_before")
    val stockQuantityBefore: Long? = null,
    @ColumnInfo(name = "adjust_quantity")
    val adjustQuantity: Long,
    @ColumnInfo(name = "stock_quantity_after")
    val stockQuantityAfter: Long? = null,
    @ColumnInfo(name = "adjustment_reason_id")
    val adjustmentReasonId: Long,
    @ColumnInfo(name = "note")
    val note: String? = null,
)

@Entity(
    tableName = "sync_queues",
    indices = [
        Index(value = ["operation_type"]),
        Index(value = ["operation_uuid"], unique = true),
        Index(value = ["sync_status"]),
        Index(value = ["created_at_epoch_millis"]),
    ],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "operation_type")
    val operationType: OperationType,
    @ColumnInfo(name = "operation_uuid")
    val operationUuid: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "last_error_message")
    val lastErrorMessage: String? = null,
    @ColumnInfo(name = "last_attempted_at_epoch_millis")
    val lastAttemptedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)
package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.StockBalanceEntity
import com.vine.database.entity.StockHistoryEntity
import com.vine.database.relation.StockBalanceWithMaster
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockBalance(balance: StockBalanceEntity): Long

    @Update
    suspend fun updateStockBalance(balance: StockBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockHistory(history: StockHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockHistories(histories: List<StockHistoryEntity>)

    @Query(
        """
        SELECT * FROM stock_balances
        WHERE product_id = :productId
          AND warehouse_id = :warehouseId
          AND location_id = :locationId
        LIMIT 1
        """
    )
    suspend fun findStockBalance(
        productId: Long,
        warehouseId: Long,
        locationId: Long,
    ): StockBalanceEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM stock_balances
        WHERE quantity <> 0
        ORDER BY product_id ASC, warehouse_id ASC, location_id ASC
        """
    )
    fun observeNonZeroStockBalances(): Flow<List<StockBalanceWithMaster>>

    @Transaction
    @Query(
        """
        SELECT * FROM stock_balances
        WHERE (:productId IS NULL OR product_id = :productId)
          AND (:warehouseId IS NULL OR warehouse_id = :warehouseId)
          AND (:locationId IS NULL OR location_id = :locationId)
        ORDER BY product_id ASC, warehouse_id ASC, location_id ASC
        """
    )
    fun observeStockBalances(
        productId: Long?,
        warehouseId: Long?,
        locationId: Long?,
    ): Flow<List<StockBalanceWithMaster>>

    @Query(
        """
        SELECT * FROM stock_histories
        WHERE (:productId IS NULL OR product_id = :productId)
          AND (:warehouseId IS NULL OR warehouse_id = :warehouseId)
          AND (:locationId IS NULL OR location_id = :locationId)
        ORDER BY operated_at_epoch_millis DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeStockHistories(
        productId: Long?,
        warehouseId: Long?,
        locationId: Long?,
        limit: Int = 200,
    ): Flow<List<StockHistoryEntity>>

    @Query(
        """
        SELECT * FROM stock_histories
        WHERE operation_uuid = :operationUuid
        ORDER BY id ASC
        """
    )
    suspend fun findHistoriesByOperationUuid(operationUuid: String): List<StockHistoryEntity>

    @Query(
        """
        SELECT COUNT(*) FROM stock_histories
        WHERE sync_status != 'SYNCED'
        """
    )
    fun observeUnsyncedHistoryCount(): Flow<Int>

    @Transaction
    suspend fun upsertStockBalance(
        productId: Long,
        warehouseId: Long,
        locationId: Long,
        deltaQuantity: Long,
        operationType: com.vine.database.model.OperationType?,
        operationUuid: String?,
        operatedAtEpochMillis: Long,
    ) {
        val current = findStockBalance(productId, warehouseId, locationId)
        if (current == null) {
            insertStockBalance(
                StockBalanceEntity(
                    productId = productId,
                    warehouseId = warehouseId,
                    locationId = locationId,
                    quantity = deltaQuantity,
                    lastOperationType = operationType,
                    lastOperationUuid = operationUuid,
                    lastUpdatedAtEpochMillis = operatedAtEpochMillis,
                    rowVersion = 1L,
                ),
            )
        } else {
            updateStockBalance(
                current.copy(
                    quantity = current.quantity + deltaQuantity,
                    lastOperationType = operationType,
                    lastOperationUuid = operationUuid,
                    lastUpdatedAtEpochMillis = operatedAtEpochMillis,
                    rowVersion = current.rowVersion + 1L,
                ),
            )
        }
    }
}
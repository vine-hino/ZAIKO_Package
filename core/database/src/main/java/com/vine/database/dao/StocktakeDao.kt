package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.StocktakeDetailEntity
import com.vine.database.entity.StocktakeHeaderEntity
import com.vine.database.model.StocktakeStatus
import com.vine.database.relation.StocktakeWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface StocktakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: StocktakeHeaderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<StocktakeDetailEntity>)

    @Update
    suspend fun updateHeader(header: StocktakeHeaderEntity)

    @Update
    suspend fun updateDetails(details: List<StocktakeDetailEntity>)

    @Transaction
    suspend fun insertStocktake(
        header: StocktakeHeaderEntity,
        details: List<StocktakeDetailEntity>,
    ): Long {
        val headerId = insertHeader(header)
        insertDetails(details.map { it.copy(stocktakeId = headerId) })
        return headerId
    }

    @Transaction
    @Query("SELECT * FROM stocktake_headers WHERE id = :id")
    suspend fun findStocktakeById(id: Long): StocktakeWithDetails?

    @Transaction
    @Query("SELECT * FROM stocktake_headers WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun findStocktakeByOperationUuid(operationUuid: String): StocktakeWithDetails?

    @Transaction
    @Query(
        """
        SELECT * FROM stocktake_headers
        ORDER BY stocktake_date DESC, id DESC
        """
    )
    fun observeStocktakes(): Flow<List<StocktakeWithDetails>>

    @Query(
        """
        SELECT * FROM stocktake_headers
        WHERE status = 'DRAFT'
        ORDER BY stocktake_date DESC, id DESC
        """
    )
    fun observeDraftStocktakes(): Flow<List<StocktakeHeaderEntity>>

    @Query(
        """
        SELECT * FROM stocktake_details
        WHERE stocktake_id = :stocktakeId
        ORDER BY line_no ASC
        """
    )
    suspend fun findDetailsByStocktakeId(stocktakeId: Long): List<StocktakeDetailEntity>

    @Query(
        """
        UPDATE stocktake_headers
        SET status = :status,
            confirmed_by = :confirmedBy,
            confirmed_at_epoch_millis = :confirmedAt,
            updated_at_epoch_millis = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun confirmStocktake(
        id: Long,
        status: StocktakeStatus = StocktakeStatus.CONFIRMED,
        confirmedBy: Long,
        confirmedAt: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE stocktake_details
        SET is_reflected = 1
        WHERE stocktake_id = :stocktakeId
        """
    )
    suspend fun markDetailsReflected(stocktakeId: Long)
}
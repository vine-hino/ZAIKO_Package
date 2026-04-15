package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.AdjustmentDetailEntity
import com.vine.database.entity.AdjustmentHeaderEntity
import com.vine.database.entity.AdjustmentReasonEntity
import com.vine.database.relation.AdjustmentWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface AdjustmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: AdjustmentHeaderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<AdjustmentDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReasons(reasons: List<AdjustmentReasonEntity>)

    @Update
    suspend fun updateHeader(header: AdjustmentHeaderEntity)

    @Transaction
    suspend fun insertAdjustment(
        header: AdjustmentHeaderEntity,
        details: List<AdjustmentDetailEntity>,
    ): Long {
        val headerId = insertHeader(header)
        insertDetails(details.map { it.copy(adjustmentId = headerId) })
        return headerId
    }

    @Transaction
    @Query("SELECT * FROM adjustment_headers WHERE id = :id")
    suspend fun findAdjustmentById(id: Long): AdjustmentWithDetails?

    @Transaction
    @Query("SELECT * FROM adjustment_headers WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun findAdjustmentByOperationUuid(operationUuid: String): AdjustmentWithDetails?

    @Transaction
    @Query(
        """
        SELECT * FROM adjustment_headers
        ORDER BY operated_at_epoch_millis DESC, id DESC
        """
    )
    fun observeAdjustments(): Flow<List<AdjustmentWithDetails>>

    @Query(
        """
        SELECT * FROM adjustment_details
        WHERE adjustment_id = :adjustmentId
        ORDER BY line_no ASC
        """
    )
    suspend fun findDetailsByAdjustmentId(adjustmentId: Long): List<AdjustmentDetailEntity>

    @Query(
        """
        SELECT * FROM adjustment_reasons
        WHERE is_active = 1
        ORDER BY sort_order ASC, reason_code ASC
        """
    )
    fun observeActiveReasons(): Flow<List<AdjustmentReasonEntity>>

    @Query(
        """
        UPDATE adjustment_headers
        SET sync_status = :syncStatus,
            updated_at_epoch_millis = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateSyncStatus(
        id: Long,
        syncStatus: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE adjustment_headers
        SET is_cancelled = 1,
            cancelled_at_epoch_millis = :cancelledAt,
            cancelled_by = :cancelledBy,
            updated_at_epoch_millis = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markCancelled(
        id: Long,
        cancelledAt: Long,
        cancelledBy: Long,
        updatedAt: Long,
    )

    @Query(
        """
    SELECT * FROM adjustment_reasons
    WHERE reason_code = :reasonCode
      AND is_active = 1
    LIMIT 1
    """
    )
    suspend fun findReasonByCode(reasonCode: String): AdjustmentReasonEntity?
}
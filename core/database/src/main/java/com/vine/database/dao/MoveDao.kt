package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.MoveDetailEntity
import com.vine.database.entity.MoveHeaderEntity
import com.vine.database.relation.MoveWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MoveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: MoveHeaderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<MoveDetailEntity>)

    @Update
    suspend fun updateHeader(header: MoveHeaderEntity)

    @Transaction
    suspend fun insertMove(
        header: MoveHeaderEntity,
        details: List<MoveDetailEntity>,
    ): Long {
        val headerId = insertHeader(header)
        insertDetails(details.map { it.copy(moveId = headerId) })
        return headerId
    }

    @Transaction
    @Query("SELECT * FROM move_headers WHERE id = :id")
    suspend fun findMoveById(id: Long): MoveWithDetails?

    @Transaction
    @Query("SELECT * FROM move_headers WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun findMoveByOperationUuid(operationUuid: String): MoveWithDetails?

    @Transaction
    @Query(
        """
        SELECT * FROM move_headers
        ORDER BY operated_at_epoch_millis DESC, id DESC
        """
    )
    fun observeMoves(): Flow<List<MoveWithDetails>>

    @Query(
        """
        SELECT * FROM move_headers
        WHERE sync_status != 'SYNCED'
        ORDER BY created_at_epoch_millis ASC
        """
    )
    suspend fun findPendingHeaders(): List<MoveHeaderEntity>

    @Query(
        """
        SELECT * FROM move_details
        WHERE move_id = :moveId
        ORDER BY line_no ASC
        """
    )
    suspend fun findDetailsByMoveId(moveId: Long): List<MoveDetailEntity>

    @Query(
        """
        UPDATE move_headers
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
        UPDATE move_headers
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
}
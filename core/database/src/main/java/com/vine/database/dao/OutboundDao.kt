package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.OutboundDetailEntity
import com.vine.database.entity.OutboundHeaderEntity
import com.vine.database.relation.OutboundWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: OutboundHeaderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<OutboundDetailEntity>)

    @Update
    suspend fun updateHeader(header: OutboundHeaderEntity)

    @Transaction
    suspend fun insertOutbound(
        header: OutboundHeaderEntity,
        details: List<OutboundDetailEntity>,
    ): Long {
        val headerId = insertHeader(header)
        insertDetails(details.map { it.copy(outboundId = headerId) })
        return headerId
    }

    @Transaction
    @Query("SELECT * FROM outbound_headers WHERE id = :id")
    suspend fun findOutboundById(id: Long): OutboundWithDetails?

    @Transaction
    @Query("SELECT * FROM outbound_headers WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun findOutboundByOperationUuid(operationUuid: String): OutboundWithDetails?

    @Transaction
    @Query(
        """
        SELECT * FROM outbound_headers
        ORDER BY operated_at_epoch_millis DESC, id DESC
        """
    )
    fun observeOutbounds(): Flow<List<OutboundWithDetails>>

    @Query(
        """
        SELECT * FROM outbound_headers
        WHERE sync_status != 'SYNCED'
        ORDER BY created_at_epoch_millis ASC
        """
    )
    suspend fun findPendingHeaders(): List<OutboundHeaderEntity>

    @Query(
        """
        SELECT * FROM outbound_details
        WHERE outbound_id = :outboundId
        ORDER BY line_no ASC
        """
    )
    suspend fun findDetailsByOutboundId(outboundId: Long): List<OutboundDetailEntity>

    @Query(
        """
        UPDATE outbound_headers
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
        UPDATE outbound_headers
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
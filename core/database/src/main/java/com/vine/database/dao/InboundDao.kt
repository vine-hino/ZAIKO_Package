package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.InboundDetailEntity
import com.vine.database.entity.InboundHeaderEntity
import com.vine.database.relation.InboundWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: InboundHeaderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<InboundDetailEntity>)

    @Update
    suspend fun updateHeader(header: InboundHeaderEntity)

    @Update
    suspend fun updateDetail(detail: InboundDetailEntity)

    @Transaction
    suspend fun insertInbound(
        header: InboundHeaderEntity,
        details: List<InboundDetailEntity>,
    ): Long {
        val headerId = insertHeader(header)
        insertDetails(details.map { it.copy(inboundId = headerId) })
        return headerId
    }

    @Transaction
    @Query("SELECT * FROM inbound_headers WHERE id = :id")
    suspend fun findInboundById(id: Long): InboundWithDetails?

    @Transaction
    @Query("SELECT * FROM inbound_headers WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun findInboundByOperationUuid(operationUuid: String): InboundWithDetails?

    @Transaction
    @Query(
        """
        SELECT * FROM inbound_headers
        ORDER BY operated_at_epoch_millis DESC, id DESC
        """
    )
    fun observeInbounds(): Flow<List<InboundWithDetails>>

    @Query(
        """
        SELECT * FROM inbound_headers
        WHERE sync_status != 'SYNCED'
        ORDER BY created_at_epoch_millis ASC
        """
    )
    suspend fun findPendingHeaders(): List<InboundHeaderEntity>

    @Query(
        """
        SELECT * FROM inbound_details
        WHERE inbound_id = :inboundId
        ORDER BY line_no ASC
        """
    )
    suspend fun findDetailsByInboundId(inboundId: Long): List<InboundDetailEntity>

    @Query(
        """
        UPDATE inbound_headers
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
        UPDATE inbound_headers
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
    SELECT COALESCE(SUM(d.quantity), 0)
    FROM inbound_headers h
    INNER JOIN inbound_details d ON d.inbound_id = h.id
    WHERE h.is_cancelled = 0
      AND date(h.operated_at_epoch_millis / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """
    )
    fun observeTodayInboundQuantity(): kotlinx.coroutines.flow.Flow<Long>
}
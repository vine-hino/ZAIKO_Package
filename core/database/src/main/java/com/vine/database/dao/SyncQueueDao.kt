package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vine.database.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(queue: SyncQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(queues: List<SyncQueueEntity>)

    @Update
    suspend fun update(queue: SyncQueueEntity)

    @Query(
        """
        SELECT * FROM sync_queues
        WHERE sync_status IN ('PENDING', 'ERROR')
        ORDER BY created_at_epoch_millis ASC, id ASC
        """
    )
    suspend fun findPendingOrErrorQueues(): List<SyncQueueEntity>

    @Query(
        """
        SELECT * FROM sync_queues
        WHERE operation_uuid = :operationUuid
        LIMIT 1
        """
    )
    suspend fun findByOperationUuid(operationUuid: String): SyncQueueEntity?

    @Query(
        """
        SELECT * FROM sync_queues
        ORDER BY created_at_epoch_millis DESC, id DESC
        """
    )
    fun observeAll(): Flow<List<SyncQueueEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM sync_queues
        WHERE sync_status != 'SYNCED'
        """
    )
    fun observeUnsyncedCount(): Flow<Int>

    @Query(
        """
        UPDATE sync_queues
        SET sync_status = :syncStatus,
            retry_count = :retryCount,
            last_error_message = :lastErrorMessage,
            last_attempted_at_epoch_millis = :lastAttemptedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: Long,
        syncStatus: String,
        retryCount: Int,
        lastErrorMessage: String?,
        lastAttemptedAt: Long?,
    )

    @Query("DELETE FROM sync_queues WHERE sync_status = 'SYNCED'")
    suspend fun deleteSynced()
}
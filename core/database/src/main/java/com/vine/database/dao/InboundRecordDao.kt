package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vine.database.entity.InboundRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InboundRecordEntity): Long

    @Query("SELECT * FROM inbound_records ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<InboundRecordEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(quantity), 0)
        FROM inbound_records
        WHERE date(createdAtEpochMillis / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
        """
    )
    fun observeTodayInboundQuantity(): Flow<Int>

    @Query("SELECT COUNT(*) FROM inbound_records WHERE syncStatus != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>
}
package com.vine.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inbound_records")
data class InboundRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productCode: String,
    val locationCode: String,
    val quantity: Int,
    val note: String?,
    val createdAtEpochMillis: Long,
    val syncStatus: String = "LOCAL_ONLY",
)
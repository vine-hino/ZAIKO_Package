package com.vine.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vine.database.dao.InboundRecordDao
import com.vine.database.entity.InboundRecordEntity

@Database(
    entities = [
        InboundRecordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ZaikoDatabase : RoomDatabase() {
    abstract fun inboundRecordDao(): InboundRecordDao
}
package com.vine.database.converter

import androidx.room.TypeConverter
import com.vine.database.model.BarcodeType
import com.vine.database.model.OperationType
import com.vine.database.model.QuantitySignType
import com.vine.database.model.StocktakeStatus
import com.vine.database.model.SyncStatus
import com.vine.database.model.TerminalType

class ZaikoTypeConverters {
    @TypeConverter
    fun fromTerminalType(value: TerminalType?): String? = value?.name

    @TypeConverter
    fun toTerminalType(value: String?): TerminalType? = value?.let(TerminalType::valueOf)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): String? = value?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? = value?.let(SyncStatus::valueOf)

    @TypeConverter
    fun fromOperationType(value: OperationType?): String? = value?.name

    @TypeConverter
    fun toOperationType(value: String?): OperationType? = value?.let(OperationType::valueOf)

    @TypeConverter
    fun fromStocktakeStatus(value: StocktakeStatus?): String? = value?.name

    @TypeConverter
    fun toStocktakeStatus(value: String?): StocktakeStatus? = value?.let(StocktakeStatus::valueOf)

    @TypeConverter
    fun fromQuantitySignType(value: QuantitySignType?): String? = value?.name

    @TypeConverter
    fun toQuantitySignType(value: String?): QuantitySignType? = value?.let(QuantitySignType::valueOf)

    @TypeConverter
    fun fromBarcodeType(value: BarcodeType?): String? = value?.name

    @TypeConverter
    fun toBarcodeType(value: String?): BarcodeType? = value?.let(BarcodeType::valueOf)
}
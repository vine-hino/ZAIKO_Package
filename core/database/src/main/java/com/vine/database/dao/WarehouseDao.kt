package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.LocationEntity
import com.vine.database.entity.OperatorEntity
import com.vine.database.entity.WarehouseEntity
import com.vine.database.relation.WarehouseWithLocations
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: WarehouseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouses(warehouses: List<WarehouseEntity>)

    @Update
    suspend fun updateWarehouse(warehouse: WarehouseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: OperatorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperators(operators: List<OperatorEntity>)

    @Update
    suspend fun updateOperator(operator: OperatorEntity)

    @Query("SELECT * FROM warehouses WHERE id = :id")
    suspend fun findWarehouseById(id: Long): WarehouseEntity?

    @Query("SELECT * FROM warehouses WHERE warehouse_code = :warehouseCode LIMIT 1")
    suspend fun findWarehouseByCode(warehouseCode: String): WarehouseEntity?

    @Query(
        """
        SELECT * FROM warehouses
        WHERE is_active = 1
        ORDER BY warehouse_code ASC
        """
    )
    fun observeActiveWarehouses(): Flow<List<WarehouseEntity>>

    @Transaction
    @Query("SELECT * FROM warehouses WHERE id = :warehouseId")
    suspend fun findWarehouseWithLocations(warehouseId: Long): WarehouseWithLocations?

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun findLocationById(id: Long): LocationEntity?

    @Query(
        """
        SELECT * FROM locations
        WHERE warehouse_id = :warehouseId
          AND location_code = :locationCode
        LIMIT 1
        """
    )
    suspend fun findLocationByWarehouseAndCode(
        warehouseId: Long,
        locationCode: String,
    ): LocationEntity?

    @Query(
        """
        SELECT * FROM locations
        WHERE scan_code = :scanCode
          AND is_active = 1
          AND is_usable = 1
        LIMIT 1
        """
    )
    suspend fun findLocationByScanCode(scanCode: String): LocationEntity?

    @Query(
        """
        SELECT * FROM locations
        WHERE warehouse_id = :warehouseId
          AND is_active = 1
        ORDER BY location_code ASC
        """
    )
    fun observeActiveLocationsByWarehouse(warehouseId: Long): Flow<List<LocationEntity>>

    @Query(
        """
        SELECT * FROM operators
        WHERE id = :operatorId
        LIMIT 1
        """
    )
    suspend fun findOperatorById(operatorId: Long): OperatorEntity?

    @Query(
        """
        SELECT * FROM operators
        WHERE operator_code = :operatorCode
          AND is_active = 1
        LIMIT 1
        """
    )
    suspend fun findOperatorByCode(operatorCode: String): OperatorEntity?

    @Query(
        """
        SELECT * FROM operators
        WHERE is_active = 1
        ORDER BY operator_code ASC
        """
    )
    fun observeActiveOperators(): Flow<List<OperatorEntity>>
}
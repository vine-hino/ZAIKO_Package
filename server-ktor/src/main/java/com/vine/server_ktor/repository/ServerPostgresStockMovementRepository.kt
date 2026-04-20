package com.vine.server_ktor.repository

import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
import com.vine.inventory_contract.StockSummaryDto
import java.time.OffsetDateTime
import javax.sql.DataSource

class ServerPostgresStockMovementRepository(
    private val dataSource: DataSource,
) : StockMovementRepository {

    fun bootstrap() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS server_stock_movements (
                        id VARCHAR(64) PRIMARY KEY,
                        reference_no VARCHAR(64) NOT NULL,
                        product_code VARCHAR(128) NOT NULL,
                        product_name VARCHAR(255) NOT NULL,
                        quantity BIGINT NOT NULL,
                        operation VARCHAR(32) NOT NULL,
                        operator_name VARCHAR(128) NOT NULL,
                        warehouse_code VARCHAR(64) NOT NULL,
                        location_code VARCHAR(64) NOT NULL,
                        note TEXT NULL,
                        adjustment_reason_code VARCHAR(64) NULL,
                        adjustment_reason_name VARCHAR(128) NULL,
                        occurred_at TIMESTAMPTZ NOT NULL
                    )
                    """.trimIndent()
                )

                statement.execute(
                    "ALTER TABLE server_stock_movements ADD COLUMN IF NOT EXISTS adjustment_reason_code VARCHAR(64) NULL"
                )
                statement.execute(
                    "ALTER TABLE server_stock_movements ADD COLUMN IF NOT EXISTS adjustment_reason_name VARCHAR(128) NULL"
                )

                statement.execute(
                    """
                    CREATE INDEX IF NOT EXISTS idx_server_stock_movements_occurred_at
                    ON server_stock_movements (occurred_at DESC)
                    """.trimIndent()
                )
            }
        }
    }

    override suspend fun save(movement: StockMovementDto) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO server_stock_movements (
                    id,
                    reference_no,
                    product_code,
                    product_name,
                    quantity,
                    operation,
                    operator_name,
                    warehouse_code,
                    location_code,
                    note,
                    adjustment_reason_code,
                    adjustment_reason_name,
                    occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, movement.id)
                statement.setString(2, movement.referenceNo)
                statement.setString(3, movement.itemId)
                statement.setString(4, movement.itemName)
                statement.setLong(5, movement.quantity)
                statement.setString(6, movement.operation.name)
                statement.setString(7, movement.operatorName)
                statement.setString(8, movement.warehouseCode)
                statement.setString(9, movement.locationCode)
                statement.setString(10, movement.note)
                statement.setString(11, movement.adjustmentReasonCode)
                statement.setString(12, movement.adjustmentReasonName)
                statement.setObject(13, OffsetDateTime.parse(movement.occurredAt))
                statement.executeUpdate()
            }
        }
    }

    override suspend fun findAll(): List<StockMovementDto> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT
                    id,
                    reference_no,
                    product_code,
                    product_name,
                    quantity,
                    operation,
                    operator_name,
                    warehouse_code,
                    location_code,
                    note,
                    adjustment_reason_code,
                    adjustment_reason_name,
                    occurred_at
                FROM server_stock_movements
                ORDER BY occurred_at DESC
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    val result = mutableListOf<StockMovementDto>()
                    while (rs.next()) {
                        result += StockMovementDto(
                            id = rs.getString("id"),
                            referenceNo = rs.getString("reference_no"),
                            itemId = rs.getString("product_code"),
                            itemName = rs.getString("product_name"),
                            quantity = rs.getLong("quantity"),
                            operation = StockOperation.valueOf(rs.getString("operation")),
                            operatorName = rs.getString("operator_name"),
                            warehouseCode = rs.getString("warehouse_code"),
                            locationCode = rs.getString("location_code"),
                            note = rs.getString("note"),
                            adjustmentReasonCode = rs.getString("adjustment_reason_code"),
                            adjustmentReasonName = rs.getString("adjustment_reason_name"),
                            occurredAt = rs.getObject("occurred_at", OffsetDateTime::class.java).toString(),
                        )
                    }
                    return result
                }
            }
        }
    }

    override suspend fun findSummary(): List<StockSummaryDto> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT
                    product_code,
                    product_name,
                    SUM(
                        CASE
                            WHEN operation = 'INBOUND' THEN quantity
                            WHEN operation = 'OUTBOUND' THEN -quantity
                            WHEN operation = 'ADJUST' THEN quantity
                            ELSE 0
                        END
                    ) AS current_quantity
                FROM server_stock_movements
                GROUP BY product_code, product_name
                ORDER BY product_code ASC
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    val result = mutableListOf<StockSummaryDto>()
                    while (rs.next()) {
                        result += StockSummaryDto(
                            itemId = rs.getString("product_code"),
                            itemName = rs.getString("product_name"),
                            currentQuantity = rs.getLong("current_quantity"),
                        )
                    }
                    return result
                }
            }
        }
    }
}
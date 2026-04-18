package com.vine.server_ktor.repository

import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import javax.sql.DataSource

class ServerPostgresStocktakeDraftRepository(
    private val dataSource: DataSource,
) : StocktakeDraftRepository {

    fun bootstrap() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS server_stocktake_drafts (
                        operation_uuid VARCHAR(64) PRIMARY KEY,
                        stocktake_no VARCHAR(64) NOT NULL,
                        stocktake_date VARCHAR(32) NOT NULL,
                        warehouse_code VARCHAR(64),
                        warehouse_name VARCHAR(255),
                        status VARCHAR(32) NOT NULL,
                        line_count INT NOT NULL,
                        entered_by_name VARCHAR(128),
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """.trimIndent()
                )

                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS server_stocktake_details (
                        detail_uuid VARCHAR(64) PRIMARY KEY,
                        operation_uuid VARCHAR(64) NOT NULL,
                        line_no INT NOT NULL,
                        product_code VARCHAR(128) NOT NULL,
                        product_name VARCHAR(255) NOT NULL,
                        warehouse_code VARCHAR(64) NOT NULL,
                        location_code VARCHAR(64) NOT NULL,
                        book_quantity BIGINT NOT NULL,
                        actual_quantity BIGINT NOT NULL,
                        diff_quantity BIGINT NOT NULL
                    )
                    """.trimIndent()
                )

                statement.execute(
                    """
                    CREATE INDEX IF NOT EXISTS idx_server_stocktake_details_operation_uuid
                    ON server_stocktake_details (operation_uuid)
                    """.trimIndent()
                )
            }
        }
    }

    override suspend fun save(
        summary: StocktakeSummary,
        details: List<StocktakeDetail>,
    ) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    """
                    INSERT INTO server_stocktake_drafts (
                        operation_uuid,
                        stocktake_no,
                        stocktake_date,
                        warehouse_code,
                        warehouse_name,
                        status,
                        line_count,
                        entered_by_name
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, summary.operationUuid)
                    statement.setString(2, summary.stocktakeNo)
                    statement.setString(3, summary.stocktakeDate)
                    statement.setString(4, summary.warehouseCode)
                    statement.setString(5, summary.warehouseName)
                    statement.setString(6, summary.status)
                    statement.setInt(7, summary.lineCount)
                    statement.setString(8, summary.enteredByName)
                    statement.executeUpdate()
                }

                connection.prepareStatement(
                    """
                    INSERT INTO server_stocktake_details (
                        detail_uuid,
                        operation_uuid,
                        line_no,
                        product_code,
                        product_name,
                        warehouse_code,
                        location_code,
                        book_quantity,
                        actual_quantity,
                        diff_quantity
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    details.forEach { detail ->
                        statement.setString(1, detail.detailUuid)
                        statement.setString(2, detail.operationUuid)
                        statement.setInt(3, detail.lineNo)
                        statement.setString(4, detail.productCode)
                        statement.setString(5, detail.productName)
                        statement.setString(6, detail.warehouseCode)
                        statement.setString(7, detail.locationCode)
                        statement.setLong(8, detail.bookQuantity)
                        statement.setLong(9, detail.actualQuantity)
                        statement.setLong(10, detail.diffQuantity)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }

                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    override suspend fun findSummaries(
        status: String?,
    ): List<StocktakeSummary> {
        dataSource.connection.use { connection ->
            val sql = buildString {
                append(
                    """
                    SELECT
                        operation_uuid,
                        stocktake_no,
                        stocktake_date,
                        warehouse_code,
                        warehouse_name,
                        status,
                        line_count,
                        entered_by_name
                    FROM server_stocktake_drafts
                    """.trimIndent()
                )
                if (!status.isNullOrBlank()) {
                    append(" WHERE status = ?")
                }
                append(" ORDER BY created_at DESC")
            }

            connection.prepareStatement(sql).use { statement ->
                if (!status.isNullOrBlank()) {
                    statement.setString(1, status)
                }

                statement.executeQuery().use { rs ->
                    val result = mutableListOf<StocktakeSummary>()
                    while (rs.next()) {
                        result += StocktakeSummary(
                            operationUuid = rs.getString("operation_uuid"),
                            stocktakeNo = rs.getString("stocktake_no"),
                            stocktakeDate = rs.getString("stocktake_date"),
                            warehouseCode = rs.getString("warehouse_code"),
                            warehouseName = rs.getString("warehouse_name"),
                            status = rs.getString("status"),
                            lineCount = rs.getInt("line_count"),
                            enteredByName = rs.getString("entered_by_name"),
                        )
                    }
                    return result
                }
            }
        }
    }

    override suspend fun findDetails(
        operationUuid: String,
        diffOnly: Boolean,
    ): List<StocktakeDetail> {
        dataSource.connection.use { connection ->
            val sql = buildString {
                append(
                    """
                    SELECT
                        detail_uuid,
                        operation_uuid,
                        line_no,
                        product_code,
                        product_name,
                        warehouse_code,
                        location_code,
                        book_quantity,
                        actual_quantity,
                        diff_quantity
                    FROM server_stocktake_details
                    WHERE operation_uuid = ?
                    """.trimIndent()
                )
                if (diffOnly) {
                    append(" AND diff_quantity <> 0")
                }
                append(" ORDER BY line_no ASC")
            }

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, operationUuid)

                statement.executeQuery().use { rs ->
                    val result = mutableListOf<StocktakeDetail>()
                    while (rs.next()) {
                        result += StocktakeDetail(
                            detailUuid = rs.getString("detail_uuid"),
                            operationUuid = rs.getString("operation_uuid"),
                            lineNo = rs.getInt("line_no"),
                            productCode = rs.getString("product_code"),
                            productName = rs.getString("product_name"),
                            warehouseCode = rs.getString("warehouse_code"),
                            locationCode = rs.getString("location_code"),
                            bookQuantity = rs.getLong("book_quantity"),
                            actualQuantity = rs.getLong("actual_quantity"),
                            diffQuantity = rs.getLong("diff_quantity"),
                        )
                    }
                    return result
                }
            }
        }
    }

    override suspend fun confirm(
        operationUuid: String,
    ): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE server_stocktake_drafts
                SET status = 'CONFIRMED'
                WHERE operation_uuid = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, operationUuid)
                return statement.executeUpdate() > 0
            }
        }
    }
}
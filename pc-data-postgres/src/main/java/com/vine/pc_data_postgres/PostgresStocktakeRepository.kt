package com.vine.pc_data_postgres

import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.GetStocktakeDetailsQuery
import com.vine.inventory_contract.GetStocktakeSummariesQuery
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import java.sql.Timestamp

class PostgresStocktakeRepository(
    private val dataSource: DataSource,
) : StocktakeRepository {

    override fun bootstrap() {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    BOOTSTRAP_SQL.forEach { sql ->
                        statement.execute(sql)
                    }
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

    override fun getSummaries(
        query: GetStocktakeSummariesQuery,
    ): List<StocktakeSummary> {
        val sql = """
            SELECT
                h.operation_uuid,
                h.stocktake_no,
                h.stocktake_date,
                h.warehouse_code,
                w.warehouse_name,
                h.status,
                COUNT(d.detail_uuid) AS line_count,
                o.operator_name AS entered_by_name
            FROM stocktake_headers h
            LEFT JOIN stocktake_details d
                ON d.operation_uuid = h.operation_uuid
            LEFT JOIN warehouses w
                ON w.warehouse_code = h.warehouse_code
            LEFT JOIN operators o
                ON o.operator_code = h.entered_by
            WHERE (? IS NULL OR h.status = ?)
              AND (? IS NULL OR h.warehouse_code = ?)
            GROUP BY
                h.operation_uuid,
                h.stocktake_no,
                h.stocktake_date,
                h.warehouse_code,
                w.warehouse_name,
                h.status,
                o.operator_name
            ORDER BY h.stocktake_date DESC, h.stocktake_no DESC
            LIMIT ?
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                val status = query.status?.takeIf { it.isNotBlank() }
                val warehouseCode = query.warehouseCode?.takeIf { it.isNotBlank() }

                ps.setString(1, status)
                ps.setString(2, status)
                ps.setString(3, warehouseCode)
                ps.setString(4, warehouseCode)
                ps.setInt(5, query.limit)

                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<StocktakeSummary>()
                    while (rs.next()) {
                        rows += StocktakeSummary(
                            operationUuid = rs.getString("operation_uuid"),
                            stocktakeNo = rs.getString("stocktake_no"),
                            stocktakeDate = rs.getString("stocktake_date"),
                            warehouseCode = rs.getString("warehouse_code"),
                            warehouseName = rs.getString("warehouse_name"),
                            status = rs.getString("status"),
                            lineCount = rs.getInt("line_count"),
                            discrepancyLineCount = 0,
                            enteredByName = rs.getString("entered_by_name"),
                        )
                    }
                    return rows
                }
            }
        }
    }

    override fun getDetails(
        query: GetStocktakeDetailsQuery,
    ): List<StocktakeDetail> {
        val sql = """
            SELECT
                d.detail_uuid,
                d.operation_uuid,
                d.line_no,
                d.product_code,
                p.product_name,
                d.warehouse_code,
                d.location_code,
                d.book_quantity,
                d.actual_quantity,
                d.diff_quantity
            FROM stocktake_details d
            LEFT JOIN products p
                ON p.product_code = d.product_code
            WHERE d.operation_uuid = ?
              AND (? = FALSE OR d.diff_quantity <> 0)
            ORDER BY d.line_no ASC
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                ps.setString(1, query.operationUuid)
                ps.setBoolean(2, query.diffOnly)

                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<StocktakeDetail>()
                    while (rs.next()) {
                        rows += StocktakeDetail(
                            detailUuid = rs.getString("detail_uuid"),
                            operationUuid = rs.getString("operation_uuid"),
                            lineNo = rs.getInt("line_no"),
                            productCode = rs.getString("product_code"),
                            productName = rs.getString("product_name") ?: "",
                            warehouseCode = rs.getString("warehouse_code"),
                            locationCode = rs.getString("location_code"),
                            bookQuantity = rs.getLong("book_quantity"),
                            actualQuantity = rs.getLong("actual_quantity"),
                            diffQuantity = rs.getLong("diff_quantity"),
                        )
                    }
                    return rows
                }
            }
        }
    }

    override fun confirm(
        command: ConfirmStocktakeCommand,
    ): ConfirmExecutionResult {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val header = findHeaderForUpdate(connection, command.operationUuid)
                    ?: return ConfirmExecutionResult(
                        success = false,
                        message = "棚卸ヘッダが見つかりません",
                    )

                if (header.status == "CONFIRMED") {
                    connection.rollback()
                    return ConfirmExecutionResult(
                        success = false,
                        message = "すでに確定済みです",
                    )
                }

                val details = findDetails(connection, command.operationUuid)
                val nowTs = Timestamp.from(Instant.now())

                details.forEach { row ->
                    if (row.diffQuantity != 0L) {
                        val before = findCurrentStock(
                            connection = connection,
                            productCode = row.productCode,
                            warehouseCode = row.warehouseCode,
                            locationCode = row.locationCode,
                        )

                        val after = before + row.diffQuantity

                        upsertStockBalance(
                            connection = connection,
                            productCode = row.productCode,
                            warehouseCode = row.warehouseCode,
                            locationCode = row.locationCode,
                            quantity = after,
                            updatedAt = nowTs,
                        )

                        insertStockHistory(
                            connection = connection,
                            operationUuid = row.operationUuid,
                            detailUuid = row.detailUuid,
                            productCode = row.productCode,
                            warehouseCode = row.warehouseCode,
                            locationCode = row.locationCode,
                            deltaQuantity = row.diffQuantity,
                            beforeQuantity = before,
                            afterQuantity = after,
                            operatorCode = command.operatorCode,
                            note = "棚卸確定",
                            operatedAt = nowTs,
                        )
                    }
                }

                markStocktakeConfirmed(
                    connection = connection,
                    operationUuid = command.operationUuid,
                    operatorCode = command.operatorCode,
                    confirmedAt = nowTs,
                )

                connection.commit()
                return ConfirmExecutionResult(
                    success = true,
                    message = "棚卸を確定しました",
                )
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun findHeaderForUpdate(
        connection: Connection,
        operationUuid: String,
    ): StocktakeHeaderRow? {
        val sql = """
            SELECT operation_uuid, stocktake_no, status
            FROM stocktake_headers
            WHERE operation_uuid = ?
            FOR UPDATE
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, operationUuid)
            ps.executeQuery().use { rs ->
                return if (rs.next()) {
                    StocktakeHeaderRow(
                        operationUuid = rs.getString("operation_uuid"),
                        stocktakeNo = rs.getString("stocktake_no"),
                        status = rs.getString("status"),
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun findDetails(
        connection: Connection,
        operationUuid: String,
    ): List<StocktakeDetail> {
        val sql = """
            SELECT
                detail_uuid,
                operation_uuid,
                line_no,
                product_code,
                warehouse_code,
                location_code,
                book_quantity,
                actual_quantity,
                diff_quantity
            FROM stocktake_details
            WHERE operation_uuid = ?
            ORDER BY line_no ASC
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, operationUuid)
            ps.executeQuery().use { rs ->
                val rows = mutableListOf<StocktakeDetail>()
                while (rs.next()) {
                    rows += StocktakeDetail(
                        detailUuid = rs.getString("detail_uuid"),
                        operationUuid = rs.getString("operation_uuid"),
                        lineNo = rs.getInt("line_no"),
                        productCode = rs.getString("product_code"),
                        productName = "",
                        warehouseCode = rs.getString("warehouse_code"),
                        locationCode = rs.getString("location_code"),
                        bookQuantity = rs.getLong("book_quantity"),
                        actualQuantity = rs.getLong("actual_quantity"),
                        diffQuantity = rs.getLong("diff_quantity"),
                    )
                }
                return rows
            }
        }
    }

    private fun findCurrentStock(
        connection: Connection,
        productCode: String,
        warehouseCode: String,
        locationCode: String,
    ): Long {
        val sql = """
            SELECT quantity
            FROM stock_balances
            WHERE product_code = ?
              AND warehouse_code = ?
              AND location_code = ?
            FOR UPDATE
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, productCode)
            ps.setString(2, warehouseCode)
            ps.setString(3, locationCode)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getLong("quantity") else 0L
            }
        }
    }

    private fun upsertStockBalance(
        connection: Connection,
        productCode: String,
        warehouseCode: String,
        locationCode: String,
        quantity: Long,
        updatedAt: Timestamp,
    ) {
        val sql = """
        INSERT INTO stock_balances (
            product_code,
            warehouse_code,
            location_code,
            quantity,
            updated_at
        )
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (product_code, warehouse_code, location_code)
        DO UPDATE SET
            quantity = EXCLUDED.quantity,
            updated_at = EXCLUDED.updated_at
    """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, productCode)
            ps.setString(2, warehouseCode)
            ps.setString(3, locationCode)
            ps.setLong(4, quantity)
            ps.setTimestamp(5, updatedAt)
            ps.executeUpdate()
        }
    }

    private fun insertStockHistory(
        connection: Connection,
        operationUuid: String,
        detailUuid: String,
        productCode: String,
        warehouseCode: String,
        locationCode: String,
        deltaQuantity: Long,
        beforeQuantity: Long,
        afterQuantity: Long,
        operatorCode: String,
        note: String?,
        operatedAt: Timestamp,
    ) {
        val sql = """
        INSERT INTO stock_histories (
            history_uuid,
            operation_type,
            operation_uuid,
            detail_uuid,
            product_code,
            warehouse_code,
            location_code,
            delta_quantity,
            before_quantity,
            after_quantity,
            operated_at,
            operator_code,
            note
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, UUID.randomUUID().toString())
            ps.setString(2, "STOCKTAKE")
            ps.setString(3, operationUuid)
            ps.setString(4, detailUuid)
            ps.setString(5, productCode)
            ps.setString(6, warehouseCode)
            ps.setString(7, locationCode)
            ps.setLong(8, deltaQuantity)
            ps.setLong(9, beforeQuantity)
            ps.setLong(10, afterQuantity)
            ps.setTimestamp(11, operatedAt)
            ps.setString(12, operatorCode)
            ps.setString(13, note)
            ps.executeUpdate()
        }
    }

    private fun markStocktakeConfirmed(
        connection: Connection,
        operationUuid: String,
        operatorCode: String,
        confirmedAt: Timestamp,
    ) {
        val sqlHeader = """
        UPDATE stocktake_headers
        SET status = 'CONFIRMED',
            confirmed_by = ?,
            confirmed_at = ?,
            updated_at = ?
        WHERE operation_uuid = ?
    """.trimIndent()

        val sqlDetails = """
        UPDATE stocktake_details
        SET is_reflected = TRUE
        WHERE operation_uuid = ?
    """.trimIndent()

        connection.prepareStatement(sqlHeader).use { ps ->
            ps.setString(1, operatorCode)
            ps.setTimestamp(2, confirmedAt)
            ps.setTimestamp(3, confirmedAt)
            ps.setString(4, operationUuid)
            ps.executeUpdate()
        }

        connection.prepareStatement(sqlDetails).use { ps ->
            ps.setString(1, operationUuid)
            ps.executeUpdate()
        }
    }

    private data class StocktakeHeaderRow(
        val operationUuid: String,
        val stocktakeNo: String,
        val status: String,
    )

    companion object {
        private val BOOTSTRAP_SQL = listOf(
            """
            CREATE TABLE IF NOT EXISTS products (
                product_code VARCHAR(30) PRIMARY KEY,
                product_name VARCHAR(100) NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS warehouses (
                warehouse_code VARCHAR(20) PRIMARY KEY,
                warehouse_name VARCHAR(100) NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS locations (
                warehouse_code VARCHAR(20) NOT NULL,
                location_code VARCHAR(30) NOT NULL,
                location_name VARCHAR(100) NOT NULL,
                PRIMARY KEY (warehouse_code, location_code)
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS operators (
                operator_code VARCHAR(30) PRIMARY KEY,
                operator_name VARCHAR(100) NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS stock_balances (
                product_code VARCHAR(30) NOT NULL,
                warehouse_code VARCHAR(20) NOT NULL,
                location_code VARCHAR(30) NOT NULL,
                quantity BIGINT NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                PRIMARY KEY (product_code, warehouse_code, location_code)
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS stock_histories (
                id BIGSERIAL PRIMARY KEY,
                history_uuid VARCHAR(36) NOT NULL UNIQUE,
                operation_type VARCHAR(20) NOT NULL,
                operation_uuid VARCHAR(36) NOT NULL,
                detail_uuid VARCHAR(36),
                product_code VARCHAR(30) NOT NULL,
                warehouse_code VARCHAR(20) NOT NULL,
                location_code VARCHAR(30) NOT NULL,
                delta_quantity BIGINT NOT NULL,
                before_quantity BIGINT,
                after_quantity BIGINT,
                operated_at TIMESTAMP NOT NULL,
                operator_code VARCHAR(30) NOT NULL,
                note VARCHAR(200)
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS stocktake_headers (
                operation_uuid VARCHAR(36) PRIMARY KEY,
                stocktake_no VARCHAR(30) NOT NULL UNIQUE,
                stocktake_date DATE NOT NULL,
                warehouse_code VARCHAR(20),
                status VARCHAR(20) NOT NULL,
                entered_by VARCHAR(30) NOT NULL,
                confirmed_by VARCHAR(30),
                confirmed_at TIMESTAMP,
                note VARCHAR(200),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS stocktake_details (
                detail_uuid VARCHAR(36) PRIMARY KEY,
                operation_uuid VARCHAR(36) NOT NULL,
                line_no INT NOT NULL,
                product_code VARCHAR(30) NOT NULL,
                warehouse_code VARCHAR(20) NOT NULL,
                location_code VARCHAR(30) NOT NULL,
                book_quantity BIGINT NOT NULL,
                actual_quantity BIGINT NOT NULL,
                diff_quantity BIGINT NOT NULL,
                counted_at TIMESTAMP NOT NULL,
                counted_by VARCHAR(30) NOT NULL,
                is_reflected BOOLEAN NOT NULL DEFAULT FALSE
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS sync_import_batches (
                batch_id VARCHAR(36) PRIMARY KEY,
                source_file_name VARCHAR(255),
                source_system VARCHAR(20) NOT NULL,
                source_device_id VARCHAR(50),
                imported_at TIMESTAMP NOT NULL,
                detail_count INT NOT NULL
            )
            """.trimIndent(),
        )
    }
}
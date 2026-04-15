package com.vine.pc_data_postgres

import com.google.gson.Gson
import com.vine.inventory_contract.StocktakeTransferPayload
import java.io.File
import java.sql.Connection
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.sql.DataSource
import java.sql.Timestamp
class StocktakeJsonImporter(
    private val dataSource: DataSource,
) {
    private val gson = Gson()

    fun importFile(file: File): ImportResult {
        val json = file.readText(Charsets.UTF_8)
        return importJson(
            json = json,
            sourceFileName = file.name,
        )
    }

    fun importJson(
        json: String,
        sourceFileName: String? = null,
    ): ImportResult {
        val payload = gson.fromJson(json, StocktakeTransferPayload::class.java)

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                ensureImportTables(connection)

                if (isBatchAlreadyImported(connection, payload.batchId)) {
                    connection.rollback()
                    return ImportResult(
                        success = true,
                        message = "このファイルはすでに取り込み済みです",
                        batchId = payload.batchId,
                        detailCount = payload.details.size,
                    )
                }

                val currentStatus = findHeaderStatusForUpdate(
                    connection = connection,
                    operationUuid = payload.header.operationUuid,
                )

                if (currentStatus == "CONFIRMED") {
                    connection.rollback()
                    return ImportResult(
                        success = false,
                        message = "確定済み棚卸には上書きできません",
                        batchId = payload.batchId,
                    )
                }

                upsertHeader(connection, payload)
                deleteDetailsByOperationUuid(connection, payload.header.operationUuid)
                insertDetails(connection, payload)
                insertImportBatch(connection, payload, sourceFileName)

                connection.commit()
                return ImportResult(
                    success = true,
                    message = "棚卸データを取り込みました",
                    batchId = payload.batchId,
                    detailCount = payload.details.size,
                )
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun ensureImportTables(connection: Connection) {
        val sql = """
            CREATE TABLE IF NOT EXISTS sync_import_batches (
                batch_id VARCHAR(36) PRIMARY KEY,
                source_file_name VARCHAR(255),
                source_system VARCHAR(20) NOT NULL,
                source_device_id VARCHAR(50),
                imported_at TIMESTAMP NOT NULL,
                detail_count INT NOT NULL
            )
        """.trimIndent()

        connection.createStatement().use { st ->
            st.execute(sql)
        }
    }

    private fun isBatchAlreadyImported(
        connection: Connection,
        batchId: String,
    ): Boolean {
        val sql = """
            SELECT 1
            FROM sync_import_batches
            WHERE batch_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, batchId)
            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    private fun findHeaderStatusForUpdate(
        connection: Connection,
        operationUuid: String,
    ): String? {
        val sql = """
            SELECT status
            FROM stocktake_headers
            WHERE operation_uuid = ?
            FOR UPDATE
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, operationUuid)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getString("status") else null
            }
        }
    }

    private fun upsertHeader(
        connection: Connection,
        payload: StocktakeTransferPayload,
    ) {
        val nowTs = Timestamp.from(Instant.now())

        val sql = """
        INSERT INTO stocktake_headers (
            operation_uuid,
            stocktake_no,
            stocktake_date,
            warehouse_code,
            status,
            entered_by,
            confirmed_by,
            confirmed_at,
            note,
            created_at,
            updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?)
        ON CONFLICT (operation_uuid)
        DO UPDATE SET
            stocktake_no = EXCLUDED.stocktake_no,
            stocktake_date = EXCLUDED.stocktake_date,
            warehouse_code = EXCLUDED.warehouse_code,
            status = EXCLUDED.status,
            entered_by = EXCLUDED.entered_by,
            note = EXCLUDED.note,
            updated_at = EXCLUDED.updated_at
    """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, payload.header.operationUuid)
            ps.setString(2, payload.header.stocktakeNo)
            ps.setDate(3, java.sql.Date.valueOf(payload.header.stocktakeDate))
            ps.setString(4, payload.header.warehouseCode)
            ps.setString(5, payload.header.status)
            ps.setString(6, payload.header.enteredByCode)
            ps.setString(7, payload.header.note)
            ps.setTimestamp(8, nowTs)
            ps.setTimestamp(9, nowTs)
            ps.executeUpdate()
        }
    }

    private fun deleteDetailsByOperationUuid(
        connection: Connection,
        operationUuid: String,
    ) {
        val sql = """
            DELETE FROM stocktake_details
            WHERE operation_uuid = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, operationUuid)
            ps.executeUpdate()
        }
    }

    private fun insertDetails(
        connection: Connection,
        payload: StocktakeTransferPayload,
    ) {
        val sql = """
        INSERT INTO stocktake_details (
            detail_uuid,
            operation_uuid,
            line_no,
            product_code,
            warehouse_code,
            location_code,
            book_quantity,
            actual_quantity,
            diff_quantity,
            counted_at,
            counted_by,
            is_reflected
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)
    """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            payload.details.forEach { row ->
                ps.setString(1, row.detailUuid)
                ps.setString(2, row.operationUuid)
                ps.setInt(3, row.lineNo)
                ps.setString(4, row.productCode)
                ps.setString(5, row.warehouseCode)
                ps.setString(6, row.locationCode)
                ps.setLong(7, row.bookQuantity)
                ps.setLong(8, row.actualQuantity)
                ps.setLong(9, row.diffQuantity)
                ps.setTimestamp(10, Timestamp.from(Instant.ofEpochMilli(row.countedAtEpochMillis)))
                ps.setString(11, row.countedByCode)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertImportBatch(
        connection: Connection,
        payload: StocktakeTransferPayload,
        sourceFileName: String?,
    ) {
        val sql = """
        INSERT INTO sync_import_batches (
            batch_id,
            source_file_name,
            source_system,
            source_device_id,
            imported_at,
            detail_count
        ) VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, payload.batchId)
            ps.setString(2, sourceFileName)
            ps.setString(3, payload.sourceSystem)
            ps.setString(4, payload.sourceDeviceId)
            ps.setTimestamp(5, Timestamp.from(Instant.now()))
            ps.setInt(6, payload.details.size)
            ps.executeUpdate()
        }
    }
}
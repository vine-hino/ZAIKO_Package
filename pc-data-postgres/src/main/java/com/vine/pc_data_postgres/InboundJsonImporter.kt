package com.vine.pc_data_postgres

import com.google.gson.Gson
import com.vine.inventory_contract.InboundTransferPayload
import java.io.File
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class InboundJsonImporter(
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
        val payload = gson.fromJson(json, InboundTransferPayload::class.java)

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                ensureTables(connection)

                if (isBatchAlreadyImported(connection, payload.batchId)) {
                    connection.rollback()
                    return ImportResult(
                        success = true,
                        message = "このファイルはすでに取り込み済みです",
                        batchId = payload.batchId,
                        detailCount = payload.details.size,
                    )
                }

                if (isOperationAlreadyImported(connection, payload.header.operationUuid)) {
                    connection.rollback()
                    return ImportResult(
                        success = true,
                        message = "この入庫実績はすでに取り込み済みです",
                        batchId = payload.batchId,
                        detailCount = payload.details.size,
                    )
                }

                insertHeader(connection, payload)
                insertDetails(connection, payload)
                applyInboundToStock(connection, payload)
                insertImportBatch(connection, payload, sourceFileName)

                connection.commit()
                return ImportResult(
                    success = true,
                    message = "入庫実績を取り込みました",
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

    private fun ensureTables(connection: Connection) {
        PostgresInboundRepository(dataSource).bootstrap()
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

    private fun isOperationAlreadyImported(
        connection: Connection,
        operationUuid: String,
    ): Boolean {
        val sql = """
            SELECT 1
            FROM inbound_headers
            WHERE operation_uuid = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, operationUuid)
            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    private fun insertHeader(
        connection: Connection,
        payload: InboundTransferPayload,
    ) {
        val nowTs = Timestamp.from(Instant.now())
        val operatedAt = Timestamp.from(Instant.ofEpochMilli(payload.header.operatedAtEpochMillis))

        val sql = """
            INSERT INTO inbound_headers (
                operation_uuid,
                inbound_no,
                operated_at,
                operator_code,
                warehouse_code,
                source_device_id,
                external_doc_no,
                inbound_plan_id,
                note,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, payload.header.operationUuid)
            ps.setString(2, payload.header.inboundNo)
            ps.setTimestamp(3, operatedAt)
            ps.setString(4, payload.header.operatorCode)
            ps.setString(5, payload.header.warehouseCode)
            ps.setString(6, payload.sourceDeviceId)
            ps.setString(7, payload.header.externalDocNo)
            ps.setString(8, payload.header.inboundPlanId)
            ps.setString(9, payload.header.note)
            ps.setTimestamp(10, nowTs)
            ps.setTimestamp(11, nowTs)
            ps.executeUpdate()
        }
    }

    private fun insertDetails(
        connection: Connection,
        payload: InboundTransferPayload,
    ) {
        val sql = """
            INSERT INTO inbound_details (
                detail_uuid,
                operation_uuid,
                line_no,
                product_code,
                to_warehouse_code,
                to_location_code,
                quantity,
                note
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            payload.details.forEach { row ->
                ps.setString(1, row.detailUuid)
                ps.setString(2, row.operationUuid)
                ps.setInt(3, row.lineNo)
                ps.setString(4, row.productCode)
                ps.setString(5, row.toWarehouseCode)
                ps.setString(6, row.toLocationCode)
                ps.setLong(7, row.quantity)
                ps.setString(8, row.note)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun applyInboundToStock(
        connection: Connection,
        payload: InboundTransferPayload,
    ) {
        val operatedAt = Timestamp.from(Instant.ofEpochMilli(payload.header.operatedAtEpochMillis))

        payload.details.forEach { row ->
            val before = findCurrentStock(
                connection = connection,
                productCode = row.productCode,
                warehouseCode = row.toWarehouseCode,
                locationCode = row.toLocationCode,
            )
            val after = before + row.quantity

            upsertStockBalance(
                connection = connection,
                productCode = row.productCode,
                warehouseCode = row.toWarehouseCode,
                locationCode = row.toLocationCode,
                quantity = after,
                updatedAt = operatedAt,
            )

            insertStockHistory(
                connection = connection,
                operationUuid = row.operationUuid,
                detailUuid = row.detailUuid,
                productCode = row.productCode,
                warehouseCode = row.toWarehouseCode,
                locationCode = row.toLocationCode,
                deltaQuantity = row.quantity,
                beforeQuantity = before,
                afterQuantity = after,
                operatorCode = payload.header.operatorCode,
                note = row.note ?: payload.header.note,
                operatedAt = operatedAt,
            )
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
            ps.setString(2, "INBOUND")
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

    private fun insertImportBatch(
        connection: Connection,
        payload: InboundTransferPayload,
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
package com.vine.pc_data_postgres

import com.google.gson.Gson
import com.vine.inventory_contract.OutboundTransferPayload
import java.io.File
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class OutboundJsonImporter(
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
        val payload = gson.fromJson(json, OutboundTransferPayload::class.java)

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                ensureTables()

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
                        message = "この出庫実績はすでに取り込み済みです",
                        batchId = payload.batchId,
                        detailCount = payload.details.size,
                    )
                }

                insertHeader(connection, payload)
                insertDetails(connection, payload)
                applyOutboundToStock(connection, payload)
                insertImportBatch(connection, payload, sourceFileName)

                connection.commit()
                return ImportResult(
                    success = true,
                    message = "出庫実績を取り込みました",
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

    private fun ensureTables() {
        PostgresOutboundRepository(dataSource).bootstrap()
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
            FROM outbound_headers
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
        payload: OutboundTransferPayload,
    ) {
        val nowTs = Timestamp.from(Instant.now())
        val operatedAt = Timestamp.from(Instant.ofEpochMilli(payload.header.operatedAtEpochMillis))

        val sql = """
            INSERT INTO outbound_headers (
                operation_uuid,
                outbound_no,
                operated_at,
                operator_code,
                warehouse_code,
                source_device_id,
                external_doc_no,
                outbound_plan_id,
                note,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, payload.header.operationUuid)
            ps.setString(2, payload.header.outboundNo)
            ps.setTimestamp(3, operatedAt)
            ps.setString(4, payload.header.operatorCode)
            ps.setString(5, payload.header.warehouseCode)
            ps.setString(6, payload.sourceDeviceId)
            ps.setString(7, payload.header.externalDocNo)
            ps.setString(8, payload.header.outboundPlanId)
            ps.setString(9, payload.header.note)
            ps.setTimestamp(10, nowTs)
            ps.setTimestamp(11, nowTs)
            ps.executeUpdate()
        }
    }

    private fun insertDetails(
        connection: Connection,
        payload: OutboundTransferPayload,
    ) {
        val sql = """
            INSERT INTO outbound_details (
                detail_uuid,
                operation_uuid,
                line_no,
                product_code,
                from_warehouse_code,
                from_location_code,
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
                ps.setString(5, row.fromWarehouseCode)
                ps.setString(6, row.fromLocationCode)
                ps.setLong(7, row.quantity)
                ps.setString(8, row.note)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun applyOutboundToStock(
        connection: Connection,
        payload: OutboundTransferPayload,
    ) {
        val operatedAt = Timestamp.from(Instant.ofEpochMilli(payload.header.operatedAtEpochMillis))

        payload.details.forEach { row ->
            val before = findCurrentStock(
                connection = connection,
                productCode = row.productCode,
                warehouseCode = row.fromWarehouseCode,
                locationCode = row.fromLocationCode,
            )

            if (before < row.quantity) {
                throw IllegalStateException(
                    "在庫不足です: ${row.productCode} ${row.fromWarehouseCode}/${row.fromLocationCode} 現在=$before 出庫=${row.quantity}"
                )
            }

            val after = before - row.quantity

            upsertStockBalance(
                connection = connection,
                productCode = row.productCode,
                warehouseCode = row.fromWarehouseCode,
                locationCode = row.fromLocationCode,
                quantity = after,
                updatedAt = operatedAt,
            )

            insertStockHistory(
                connection = connection,
                operationUuid = row.operationUuid,
                detailUuid = row.detailUuid,
                productCode = row.productCode,
                warehouseCode = row.fromWarehouseCode,
                locationCode = row.fromLocationCode,
                deltaQuantity = -row.quantity,
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
            ps.setString(2, "OUTBOUND")
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
        payload: OutboundTransferPayload,
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
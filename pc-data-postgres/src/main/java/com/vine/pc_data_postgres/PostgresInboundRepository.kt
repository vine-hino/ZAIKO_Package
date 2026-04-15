package com.vine.pc_data_postgres

import com.vine.inventory_contract.GetInboundDetailsQuery
import com.vine.inventory_contract.GetInboundSummariesQuery
import com.vine.inventory_contract.InboundDetail
import com.vine.inventory_contract.InboundSummary
import java.sql.Timestamp
import javax.sql.DataSource

class PostgresInboundRepository(
    private val dataSource: DataSource,
) : InboundRepository {

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
        query: GetInboundSummariesQuery,
    ): List<InboundSummary> {
        val sql = """
            SELECT
                h.operation_uuid,
                h.inbound_no,
                h.operated_at,
                h.operator_code,
                h.warehouse_code,
                COUNT(d.detail_uuid) AS line_count,
                h.external_doc_no,
                h.inbound_plan_id,
                h.note
            FROM inbound_headers h
            LEFT JOIN inbound_details d
                ON d.operation_uuid = h.operation_uuid
            WHERE (? IS NULL OR h.warehouse_code = ?)
            GROUP BY
                h.operation_uuid,
                h.inbound_no,
                h.operated_at,
                h.operator_code,
                h.warehouse_code,
                h.external_doc_no,
                h.inbound_plan_id,
                h.note
            ORDER BY h.operated_at DESC, h.inbound_no DESC
            LIMIT ?
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                val warehouseCode = query.warehouseCode?.takeIf { it.isNotBlank() }
                ps.setString(1, warehouseCode)
                ps.setString(2, warehouseCode)
                ps.setInt(3, query.limit)

                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<InboundSummary>()
                    while (rs.next()) {
                        rows += InboundSummary(
                            operationUuid = rs.getString("operation_uuid"),
                            inboundNo = rs.getString("inbound_no"),
                            operatedAtEpochMillis = rs.getTimestamp("operated_at").time,
                            operatorCode = rs.getString("operator_code"),
                            warehouseCode = rs.getString("warehouse_code"),
                            lineCount = rs.getInt("line_count"),
                            externalDocNo = rs.getString("external_doc_no"),
                            inboundPlanId = rs.getString("inbound_plan_id"),
                            note = rs.getString("note"),
                        )
                    }
                    return rows
                }
            }
        }
    }

    override fun getDetails(
        query: GetInboundDetailsQuery,
    ): List<InboundDetail> {
        val sql = """
            SELECT
                detail_uuid,
                operation_uuid,
                line_no,
                product_code,
                to_warehouse_code,
                to_location_code,
                quantity,
                note
            FROM inbound_details
            WHERE operation_uuid = ?
            ORDER BY line_no ASC
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                ps.setString(1, query.operationUuid)

                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<InboundDetail>()
                    while (rs.next()) {
                        rows += InboundDetail(
                            detailUuid = rs.getString("detail_uuid"),
                            operationUuid = rs.getString("operation_uuid"),
                            lineNo = rs.getInt("line_no"),
                            productCode = rs.getString("product_code"),
                            toWarehouseCode = rs.getString("to_warehouse_code"),
                            toLocationCode = rs.getString("to_location_code"),
                            quantity = rs.getLong("quantity"),
                            note = rs.getString("note"),
                        )
                    }
                    return rows
                }
            }
        }
    }

    companion object {
        private val BOOTSTRAP_SQL = listOf(
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
            CREATE TABLE IF NOT EXISTS inbound_headers (
                operation_uuid VARCHAR(36) PRIMARY KEY,
                inbound_no VARCHAR(30) NOT NULL UNIQUE,
                operated_at TIMESTAMP NOT NULL,
                operator_code VARCHAR(30) NOT NULL,
                warehouse_code VARCHAR(20),
                source_device_id VARCHAR(50),
                external_doc_no VARCHAR(50),
                inbound_plan_id VARCHAR(50),
                note VARCHAR(200),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS inbound_details (
                detail_uuid VARCHAR(36) PRIMARY KEY,
                operation_uuid VARCHAR(36) NOT NULL,
                line_no INT NOT NULL,
                product_code VARCHAR(30) NOT NULL,
                to_warehouse_code VARCHAR(20) NOT NULL,
                to_location_code VARCHAR(30) NOT NULL,
                quantity BIGINT NOT NULL,
                note VARCHAR(200)
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
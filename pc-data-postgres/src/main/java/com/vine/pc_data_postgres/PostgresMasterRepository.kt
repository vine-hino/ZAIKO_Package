package com.vine.pc_data_postgres

import com.vine.inventory_contract.DeleteMasterRecordCommand
import com.vine.inventory_contract.GetMasterRecordsQuery
import com.vine.inventory_contract.MasterRecordDetail
import com.vine.inventory_contract.MasterRecordSummary
import com.vine.inventory_contract.MasterType
import com.vine.inventory_contract.SaveMasterRecordCommand
import javax.sql.DataSource

class PostgresMasterRepository(
    private val dataSource: DataSource,
) : MasterRepository {

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
        query: GetMasterRecordsQuery,
    ): List<MasterRecordSummary> {
        val sql = """
            SELECT
                master_type,
                code,
                name,
                warehouse_code,
                parent_code,
                sort_order,
                is_active
            FROM master_records
            WHERE master_type = ?
              AND (
                    ? IS NULL
                    OR LOWER(code) LIKE LOWER(?)
                    OR LOWER(name) LIKE LOWER(?)
                  )
              AND (? = TRUE OR is_active = TRUE)
            ORDER BY sort_order ASC, code ASC
            LIMIT ?
        """.trimIndent()

        val keyword = query.keyword?.trim()?.takeIf { it.isNotEmpty() }
        val likeKeyword = keyword?.let { "%$it%" }

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                ps.setString(1, query.type.name)
                ps.setString(2, keyword)
                ps.setString(3, likeKeyword)
                ps.setString(4, likeKeyword)
                ps.setBoolean(5, query.includeInactive)
                ps.setInt(6, query.limit)

                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<MasterRecordSummary>()
                    while (rs.next()) {
                        rows += MasterRecordSummary(
                            type = MasterType.valueOf(rs.getString("master_type")),
                            code = rs.getString("code"),
                            name = rs.getString("name"),
                            warehouseCode = rs.getString("warehouse_code"),
                            parentCode = rs.getString("parent_code"),
                            sortOrder = rs.getInt("sort_order"),
                            isActive = rs.getBoolean("is_active"),
                        )
                    }
                    return rows
                }
            }
        }
    }

    override fun getDetail(
        type: MasterType,
        code: String,
    ): MasterRecordDetail? {
        val sql = """
            SELECT
                master_type,
                code,
                name,
                warehouse_code,
                parent_code,
                sort_order,
                is_active,
                note
            FROM master_records
            WHERE master_type = ?
              AND code = ?
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                ps.setString(1, type.name)
                ps.setString(2, code.trim())

                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null

                    return MasterRecordDetail(
                        type = MasterType.valueOf(rs.getString("master_type")),
                        code = rs.getString("code"),
                        name = rs.getString("name"),
                        warehouseCode = rs.getString("warehouse_code"),
                        parentCode = rs.getString("parent_code"),
                        sortOrder = rs.getInt("sort_order"),
                        isActive = rs.getBoolean("is_active"),
                        note = rs.getString("note"),
                    )
                }
            }
        }
    }

    override fun save(
        command: SaveMasterRecordCommand,
    ): MasterRecordDetail {
        val code = command.code.trim()
        val name = command.name.trim()

        require(code.isNotEmpty()) { "コードは必須です。" }
        require(name.isNotEmpty()) { "名称は必須です。" }

        val sql = """
            INSERT INTO master_records (
                master_type,
                code,
                name,
                warehouse_code,
                parent_code,
                sort_order,
                is_active,
                note,
                created_at,
                updated_at
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (master_type, code)
            DO UPDATE SET
                name = EXCLUDED.name,
                warehouse_code = EXCLUDED.warehouse_code,
                parent_code = EXCLUDED.parent_code,
                sort_order = EXCLUDED.sort_order,
                is_active = EXCLUDED.is_active,
                note = EXCLUDED.note,
                updated_at = CURRENT_TIMESTAMP
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(sql).use { ps ->
                    ps.setString(1, command.type.name)
                    ps.setString(2, code)
                    ps.setString(3, name)
                    ps.setString(4, command.warehouseCode.trimToNull())
                    ps.setString(5, command.parentCode.trimToNull())
                    ps.setInt(6, command.sortOrder)
                    ps.setBoolean(7, command.isActive)
                    ps.setString(8, command.note.trimToNull())
                    ps.executeUpdate()
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }

        return getDetail(command.type, code)
            ?: error("保存後のマスタ取得に失敗しました。")
    }

    override fun delete(
        command: DeleteMasterRecordCommand,
    ) {
        val sql = """
        UPDATE master_records
        SET is_active = FALSE,
            updated_at = CURRENT_TIMESTAMP
        WHERE master_type = ?
          AND code = ?
    """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                ps.setString(1, command.type.name)
                ps.setString(2, command.code.trim())
                ps.executeUpdate()
            }
        }
    }

    private fun String?.trimToNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private val BOOTSTRAP_SQL = listOf(
            """
            CREATE TABLE IF NOT EXISTS master_records (
                id BIGSERIAL PRIMARY KEY,
                master_type VARCHAR(30) NOT NULL,
                code VARCHAR(50) NOT NULL,
                name VARCHAR(100) NOT NULL,
                warehouse_code VARCHAR(50),
                parent_code VARCHAR(50),
                sort_order INT NOT NULL DEFAULT 0,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                note VARCHAR(200),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                UNIQUE (master_type, code)
            )
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS idx_master_records_type_code
            ON master_records(master_type, code)
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS idx_master_records_type_name
            ON master_records(master_type, name)
            """.trimIndent(),
        )
    }
}
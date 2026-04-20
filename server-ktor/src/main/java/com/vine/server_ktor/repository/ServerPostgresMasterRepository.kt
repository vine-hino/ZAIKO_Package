package com.vine.server_ktor.repository

import javax.sql.DataSource

data class ServerMasterSummary(
    val type: String,
    val code: String,
    val name: String,
    val warehouseCode: String?,
    val parentCode: String?,
    val isActive: Boolean,
)

class ServerPostgresMasterRepository(
    private val dataSource: DataSource,
) {
    fun bootstrap() {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.execute(
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
                        """.trimIndent()
                    )

                    statement.execute(
                        """
                        CREATE INDEX IF NOT EXISTS idx_master_records_type_code
                        ON master_records(master_type, code)
                        """.trimIndent()
                    )

                    statement.execute(
                        """
                        CREATE INDEX IF NOT EXISTS idx_master_records_type_name
                        ON master_records(master_type, name)
                        """.trimIndent()
                    )
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

    fun search(
        type: String,
        keyword: String?,
        includeInactive: Boolean,
        limit: Int,
    ): List<ServerMasterSummary> {
        val sql = """
            SELECT
                master_type,
                code,
                name,
                warehouse_code,
                parent_code,
                is_active
            FROM master_records
            WHERE master_type = ?
              AND (
                    ? IS NULL
                    OR LOWER(code) LIKE LOWER(?)
                    OR LOWER(name) LIKE LOWER(?)
                  )
              AND (? = TRUE OR is_active = TRUE)
            ORDER BY code ASC
            LIMIT ?
        """.trimIndent()

        val normalizedType = type.trim().uppercase()
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val likeKeyword = normalizedKeyword?.let { "%$it%" }

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { ps ->
                ps.setString(1, normalizedType)
                ps.setString(2, normalizedKeyword)
                ps.setString(3, likeKeyword)
                ps.setString(4, likeKeyword)
                ps.setBoolean(5, includeInactive)
                ps.setInt(6, limit)

                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<ServerMasterSummary>()
                    while (rs.next()) {
                        rows += ServerMasterSummary(
                            type = rs.getString("master_type"),
                            code = rs.getString("code"),
                            name = rs.getString("name"),
                            warehouseCode = rs.getString("warehouse_code"),
                            parentCode = rs.getString("parent_code"),
                            isActive = rs.getBoolean("is_active"),
                        )
                    }
                    return rows
                }
            }
        }
    }
}
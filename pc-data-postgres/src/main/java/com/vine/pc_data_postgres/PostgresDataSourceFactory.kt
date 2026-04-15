package com.vine.pc_data_postgres

import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

object PostgresDataSourceFactory {
    fun create(config: PgConfig): DataSource {
        return PGSimpleDataSource().apply {
            setURL(config.jdbcUrl)
            user = config.user
            password = config.password
        }
    }
}
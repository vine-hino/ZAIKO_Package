package com.vine.server_ktor.persistence

import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

object ServerPostgresDataSourceFactory {
    fun create(config: ServerPgConfig): DataSource {
        return PGSimpleDataSource().apply {
            setURL(config.jdbcUrl)
            user = config.user
            password = config.password
        }
    }
}
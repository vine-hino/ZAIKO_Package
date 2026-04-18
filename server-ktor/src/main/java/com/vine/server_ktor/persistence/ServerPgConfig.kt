package com.vine.server_ktor.persistence

import io.ktor.server.config.ApplicationConfig

data class ServerPgConfig(
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$database"

    companion object {
        fun from(config: ApplicationConfig): ServerPgConfig {
            return ServerPgConfig(
                host = config.property("postgres.host").getString(),
                port = config.property("postgres.port").getString().toInt(),
                database = config.property("postgres.database").getString(),
                user = config.property("postgres.user").getString(),
                password = config.property("postgres.password").getString(),
            )
        }
    }
}
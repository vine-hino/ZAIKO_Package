package com.vine.server_ktor

import com.vine.server_ktor.persistence.ServerPgConfig
import com.vine.server_ktor.persistence.ServerPostgresDataSourceFactory
import com.vine.server_ktor.realtime.InventoryBroadcaster
import com.vine.server_ktor.repository.ServerPostgresStockMovementRepository
import com.vine.server_ktor.repository.ServerPostgresStocktakeDraftRepository
import com.vine.server_ktor.routes.inventoryRoutes
import com.vine.server_ktor.routes.stocktakeRoutes
import com.vine.server_ktor.service.InventoryService
import com.vine.server_ktor.service.StocktakeService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val jsonConfig = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    install(ContentNegotiation) {
        json(jsonConfig)
    }

    install(WebSockets)

    val pgConfig = ServerPgConfig.from(environment.config)
    val dataSource = ServerPostgresDataSourceFactory.create(pgConfig)

    val movementRepository = ServerPostgresStockMovementRepository(dataSource).apply {
        bootstrap()
    }

    val stocktakeRepository = ServerPostgresStocktakeDraftRepository(dataSource).apply {
        bootstrap()
    }

    val broadcaster = InventoryBroadcaster(jsonConfig)

    val inventoryService = InventoryService(
        repository = movementRepository,
        broadcaster = broadcaster,
    )

    val stocktakeService = StocktakeService(
        stocktakeRepository = stocktakeRepository,
        movementRepository = movementRepository,
    )

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        inventoryRoutes(inventoryService)
        stocktakeRoutes(stocktakeService)
    }
}
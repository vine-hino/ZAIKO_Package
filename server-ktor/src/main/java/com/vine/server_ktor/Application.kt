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
import com.vine.connector_api.MasterLookupItem
import io.ktor.http.HttpStatusCode
import com.vine.server_ktor.repository.ServerPostgresMasterRepository


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

    val masterRepository = ServerPostgresMasterRepository(dataSource).also { it.bootstrap() }

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

        get("/masters") {
            val typeParam = call.request.queryParameters["type"]
                ?.trim()
                ?.uppercase()
                .orEmpty()

            val allowedTypes = setOf(
                "PRODUCT",
                "WAREHOUSE",
                "LOCATION",
                "OPERATOR",
                "REASON",
            )

            if (typeParam !in allowedTypes) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "type must be one of PRODUCT, WAREHOUSE, LOCATION, OPERATOR, REASON")
                )
                return@get
            }

            val keyword = call.request.queryParameters["q"]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            val includeInactive = call.request.queryParameters["includeInactive"]
                ?.toBooleanStrictOrNull()
                ?: false

            val limit = call.request.queryParameters["limit"]
                ?.toIntOrNull()
                ?.coerceIn(1, 500)
                ?: 50

            val rows = masterRepository.search(
                type = typeParam,
                keyword = keyword,
                includeInactive = includeInactive,
                limit = limit,
            )

            call.respond(
                rows.map {
                    MasterLookupItem(
                        type = it.type,
                        code = it.code,
                        name = it.name,
                        warehouseCode = it.warehouseCode,
                        parentCode = it.parentCode,
                        isActive = it.isActive,
                    )
                }
            )
        }

        inventoryRoutes(inventoryService)
        stocktakeRoutes(stocktakeService)
    }
}
package com.vine.server_ktor.routes

import com.vine.inventory_contract.RegisterStockMovementRequest
import com.vine.inventory_contract.StockBalanceListResponse
import com.vine.inventory_contract.StockMovementListResponse
import com.vine.inventory_contract.StockSummaryListResponse
import com.vine.server_ktor.service.InventoryService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket

fun Route.inventoryRoutes(service: InventoryService) {
    route("/inventory") {
        get("/movements") {
            call.respond(
                StockMovementListResponse(
                    movements = service.getMovements(),
                )
            )
        }

        get("/summary") {
            call.respond(
                StockSummaryListResponse(
                    items = service.getSummary(),
                )
            )
        }

        post("/movements") {
            val request = call.receive<RegisterStockMovementRequest>()
            val created = service.register(request)
            call.respond(HttpStatusCode.Created, created)
        }
    }

    route("/stock") {
        get("/balances") {
            val keyword = call.request.queryParameters["keyword"]
            val warehouseCode = call.request.queryParameters["warehouseCode"]
            val locationCode = call.request.queryParameters["locationCode"]

            call.respond(
                StockBalanceListResponse(
                    items = service.getBalances(
                        keyword = keyword,
                        warehouseCode = warehouseCode,
                        locationCode = locationCode,
                    )
                )
            )
        }
    }

    webSocket("/ws/inventory") {
        service.handleRealtimeSession(this)
    }
}
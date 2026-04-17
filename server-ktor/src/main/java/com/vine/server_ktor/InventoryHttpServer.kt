package com.vine.server_ktor

import com.vine.inventory_contract.InboundTransferContracts
import com.vine.usecase_export_inventory.ExportInventoryUseCase
import com.vine.usecase_import_inventory.ImportInventoryUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

class InventoryHttpServer(
    private val exportUseCase: ExportInventoryUseCase,
    private val importUseCase: ImportInventoryUseCase
) {
    fun start(
        host: String = "0.0.0.0",
        port: Int = 8080
    ): ApplicationEngine {
        return embeddedServer(
            factory = Netty,
            host = host,
            port = port
        ) {
            module(exportUseCase, importUseCase)
        }.start(wait = false)
    }
}

fun Application.module(
    exportUseCase: ExportInventoryUseCase,
    importUseCase: ImportInventoryUseCase
) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        get("/inventory/export") {
            call.respond(exportUseCase.execute())
        }

        post("/inventory/import") {
            val body = call.receive<InboundTransferContracts>()
            importUseCase.execute(body)
            call.respond(HttpStatusCode.OK)
        }
    }
}
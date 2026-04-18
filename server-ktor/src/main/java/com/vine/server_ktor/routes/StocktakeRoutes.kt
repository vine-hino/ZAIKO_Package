package com.vine.server_ktor.routes

import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.SaveStocktakeDraftRequest
import com.vine.server_ktor.service.StocktakeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.stocktakeRoutes(service: StocktakeService) {
    route("/stocktake") {
        post("/drafts") {
            val request = call.receive<SaveStocktakeDraftRequest>()
            val created = service.saveDraft(request)
            call.respond(HttpStatusCode.Created, created)
        }

        get("/drafts") {
            call.respond(service.getDrafts())
        }

        get("/drafts/{operationUuid}/details") {
            val operationUuid = call.parameters["operationUuid"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val diffOnly = call.request.queryParameters["diffOnly"] == "true"

            call.respond(
                service.getDetails(
                    operationUuid = operationUuid,
                    diffOnly = diffOnly,
                )
            )
        }

        post("/drafts/{operationUuid}/confirm") {
            val operationUuid = call.parameters["operationUuid"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)

            val body = call.receive<ConfirmStocktakeCommand>()
            call.respond(
                service.confirm(
                    body.copy(operationUuid = operationUuid)
                )
            )
        }
    }
}
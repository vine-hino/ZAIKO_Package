package com.vine.pc_app.network

import com.vine.inventory_contract.RegisterStockMovementRequest
import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockMovementListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class InventoryMovementClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getMovements(): StockMovementListResponse {
        return client.get("$baseUrl/inventory/movements").body()
    }

    suspend fun registerMovement(request: RegisterStockMovementRequest): StockMovementDto {
        return client.post("$baseUrl/inventory/movements") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
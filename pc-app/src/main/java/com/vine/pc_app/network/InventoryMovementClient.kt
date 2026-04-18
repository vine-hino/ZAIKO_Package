package com.vine.pc_app.network

import com.vine.inventory_contract.StockMovementListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class InventoryMovementClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getMovements(): StockMovementListResponse {
        return client.get("$baseUrl/inventory/movements").body()
    }
}
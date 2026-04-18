package com.vine.pc_app.network

import com.vine.inventory_contract.StockSummaryListResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class InventoryQueryClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getSummary(): StockSummaryListResponse {
        return client.get("$baseUrl/inventory/summary").body()
    }
}
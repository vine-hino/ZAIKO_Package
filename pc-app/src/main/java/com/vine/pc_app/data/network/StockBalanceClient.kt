package com.vine.pc_app.data.network

import com.vine.inventory_contract.StockBalanceListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class StockBalanceClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getBalances(): StockBalanceListResponse {
        return client.get("$baseUrl/stock/balances").body()
    }
}
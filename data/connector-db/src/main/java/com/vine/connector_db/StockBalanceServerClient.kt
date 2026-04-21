package com.vine.connector_db

import com.vine.connector_api.StockItem
import com.vine.connector_api.StockQuery
import com.vine.inventory_contract.StockBalanceListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StockBalanceServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun searchStock(query: StockQuery): List<StockItem> {
        val keyword = query.productCode
            ?.takeIf { it.isNotBlank() }
            ?: query.barcode?.takeIf { it.isNotBlank() }

        val response = client.get("$baseUrl/stock/balances") {
            keyword?.let { parameter("keyword", it) }
            query.warehouseCode?.takeIf { it.isNotBlank() }?.let { parameter("warehouseCode", it) }
            query.locationCode?.takeIf { it.isNotBlank() }?.let { parameter("locationCode", it) }
        }.body<StockBalanceListResponse>()

        return response.items
            .take(query.limit)
            .map {
                StockItem(
                    productCode = it.productCode,
                    productName = it.productName,
                    warehouseCode = it.warehouseCode,
                    warehouseName = it.warehouseCode,
                    locationCode = it.locationCode,
                    locationName = it.locationCode,
                    quantity = it.quantity,
                )
            }
    }
}

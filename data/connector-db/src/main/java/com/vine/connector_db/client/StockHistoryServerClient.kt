package com.vine.connector_db.client

import com.vine.connector_api.StockHistoryItem
import com.vine.connector_api.StockHistoryQuery
import com.vine.inventory_contract.StockMovementListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import java.time.OffsetDateTime

@Singleton
class StockHistoryServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun getStockHistory(query: StockHistoryQuery): List<StockHistoryItem> {
        val normalizedProductCode = query.productCode?.takeIf { it.isNotBlank() }
        val normalizedWarehouseCode = query.warehouseCode?.takeIf { it.isNotBlank() }
        val normalizedLocationCode = query.locationCode?.takeIf { it.isNotBlank() }

        val response = client.get("$baseUrl/inventory/movements").body<StockMovementListResponse>()

        return response.movements
            .asSequence()
            .filter { normalizedProductCode == null || it.itemId == normalizedProductCode }
            .filter { normalizedWarehouseCode == null || it.warehouseCode == normalizedWarehouseCode }
            .filter { normalizedLocationCode == null || it.locationCode == normalizedLocationCode }
            .take(query.limit)
            .map { movement ->
                val operatedAtEpochMillis = runCatching {
                    OffsetDateTime.parse(movement.occurredAt).toInstant().toEpochMilli()
                }.getOrDefault(0L)

                StockHistoryItem(
                    operationUuid = movement.id,
                    operationType = movement.operation.name,
                    productCode = movement.itemId,
                    productName = movement.itemName,
                    warehouseCode = movement.warehouseCode,
                    locationCode = movement.locationCode,
                    deltaQuantity = when (movement.operation.name) {
                        "OUTBOUND" -> -movement.quantity
                        else -> movement.quantity
                    },
                    beforeQuantity = null,
                    afterQuantity = null,
                    operatedAtEpochMillis = operatedAtEpochMillis,
                    operatorCode = movement.operatorName,
                    operatorName = movement.operatorName,
                    note = movement.note,
                )
            }
            .toList()
    }
}

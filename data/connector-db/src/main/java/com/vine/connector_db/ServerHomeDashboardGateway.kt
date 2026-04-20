package com.vine.connector_db

import com.vine.connector_api.HomeDashboardGateway
import com.vine.connector_api.HomeDashboardSummary
import com.vine.inventory_contract.StockMovementListResponse
import com.vine.inventory_contract.StockOperation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ServerHomeDashboardGateway @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) : HomeDashboardGateway {

    override suspend fun getSummary(): HomeDashboardSummary {
        val today = LocalDate.now()

        val movements = client.get("$baseUrl/inventory/movements")
            .body<StockMovementListResponse>()
            .movements

        val todayRows = movements.filter { movement ->
            runCatching {
                OffsetDateTime.parse(movement.occurredAt).toLocalDate() == today
            }.getOrDefault(false)
        }

        return HomeDashboardSummary(
            todayInboundQuantity = todayRows
                .filter { it.operation == StockOperation.INBOUND }
                .sumOf { it.quantity },
            todayOutboundQuantity = todayRows
                .filter { it.operation == StockOperation.OUTBOUND }
                .sumOf { it.quantity },
        )
    }
}
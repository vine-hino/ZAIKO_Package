package com.vine.connector_db

import com.vine.connector_api.SubmitResult
import com.vine.inventory_contract.RegisterStockMoveRequest
import com.vine.inventory_contract.StockMoveResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MoveServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun registerMove(
        productCode: String,
        productName: String,
        fromWarehouseCode: String,
        fromLocationCode: String,
        toWarehouseCode: String,
        toLocationCode: String,
        quantity: Long,
        operatorCode: String,
        note: String?,
    ): SubmitResult {
        return runCatching {
            val response = client.post("$baseUrl/inventory/moves") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterStockMoveRequest(
                        itemId = productCode,
                        itemName = productName,
                        quantity = quantity,
                        operatorName = operatorCode,
                        fromWarehouseCode = fromWarehouseCode,
                        fromLocationCode = fromLocationCode,
                        toWarehouseCode = toWarehouseCode,
                        toLocationCode = toLocationCode,
                        note = note,
                    )
                )
            }.body<StockMoveResult>()

            SubmitResult(
                accepted = response.accepted,
                message = response.message,
                referenceId = response.referenceNo,
                operationUuid = response.outboundMovementId,
            )
        }.getOrElse { error ->
            SubmitResult(
                accepted = false,
                message = error.message ?: "サーバー通信に失敗しました",
            )
        }
    }
}

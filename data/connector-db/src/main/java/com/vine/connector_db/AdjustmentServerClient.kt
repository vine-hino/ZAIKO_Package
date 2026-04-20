package com.vine.connector_db

import com.vine.connector_api.SubmitResult
import com.vine.inventory_contract.RegisterStockMovementRequest
import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
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
class AdjustmentServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun registerAdjustment(
        productCode: String,
        productName: String,
        warehouseCode: String,
        locationCode: String,
        adjustQuantity: Long,
        reasonCode: String,
        reasonName: String,
        operatorCode: String,
        note: String?,
    ): SubmitResult {
        return runCatching {
            val response = client.post("$baseUrl/inventory/movements") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterStockMovementRequest(
                        itemId = productCode,
                        itemName = productName,
                        quantity = adjustQuantity,
                        operation = StockOperation.ADJUST,
                        operatorName = operatorCode,
                        warehouseCode = warehouseCode,
                        locationCode = locationCode,
                        note = note,
                        adjustmentReasonCode = reasonCode,
                        adjustmentReasonName = reasonName,
                    )
                )
            }.body<StockMovementDto>()

            SubmitResult(
                accepted = true,
                message = "在庫調整をサーバーへ登録しました",
                referenceId = response.referenceNo,
                operationUuid = response.id,
            )
        }.getOrElse { error ->
            SubmitResult(
                accepted = false,
                message = error.message ?: "サーバー通信に失敗しました",
            )
        }
    }
}
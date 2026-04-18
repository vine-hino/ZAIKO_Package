package com.vine.connector_db

import com.vine.connector_api.StocktakeCommand
import com.vine.connector_api.SubmitResult
import com.vine.inventory_contract.SaveStocktakeDraftRequest
import com.vine.inventory_contract.StocktakeSummary
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
class StocktakeServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun saveDraft(
        command: StocktakeCommand,
        productName: String,
    ): SubmitResult {
        val line = command.lines.firstOrNull()
            ?: return SubmitResult(
                accepted = false,
                message = "棚卸明細がありません",
            )

        return runCatching {
            val response = client.post("$baseUrl/stocktake/drafts") {
                contentType(ContentType.Application.Json)
                setBody(
                    SaveStocktakeDraftRequest(
                        stocktakeDate = command.stocktakeDate,
                        operatorCode = command.operatorCode,
                        warehouseCode = line.warehouseCode,
                        productCode = line.productCode,
                        productName = productName,
                        locationCode = line.locationCode,
                        actualQuantity = line.actualQuantity,
                        note = command.note,
                    )
                )
            }.body<StocktakeSummary>()

            SubmitResult(
                accepted = true,
                message = "棚卸をサーバーへ保存しました",
                referenceId = response.stocktakeNo,
                operationUuid = response.operationUuid,
            )
        }.getOrElse { error ->
            SubmitResult(
                accepted = false,
                message = error.message ?: "サーバー通信に失敗しました",
            )
        }
    }
}
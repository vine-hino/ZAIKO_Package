package com.vine.connector_db

import com.vine.connector_api.CancelOperationCommand
import com.vine.connector_api.SubmitResult
import com.vine.inventory_contract.CancelStockMovementRequest
import com.vine.inventory_contract.CancelStockMovementResult
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
class CancelServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun cancel(command: CancelOperationCommand): SubmitResult {
        return runCatching {
            val response = client.post("$baseUrl/inventory/cancel") {
                contentType(ContentType.Application.Json)
                setBody(
                    CancelStockMovementRequest(
                        operationUuid = command.operationUuid,
                        operatorCode = command.operatorCode,
                        note = command.note,
                    )
                )
            }.body<CancelStockMovementResult>()

            SubmitResult(
                accepted = response.accepted,
                message = response.message,
                referenceId = response.referenceNo,
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

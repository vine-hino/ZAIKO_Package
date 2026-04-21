package com.vine.pc_app.data.network

import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.StocktakeActionResult
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class StocktakeServerClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getDrafts(): List<StocktakeSummary> {
        return client.get("$baseUrl/stocktake/drafts").body()
    }

    suspend fun getDetails(
        operationUuid: String,
        diffOnly: Boolean,
    ): List<StocktakeDetail> {
        return client.get(
            "$baseUrl/stocktake/drafts/$operationUuid/details?diffOnly=$diffOnly"
        ).body()
    }

    suspend fun confirm(
        operationUuid: String,
        operatorCode: String,
    ): StocktakeActionResult {
        return client.post("$baseUrl/stocktake/drafts/$operationUuid/confirm") {
            contentType(ContentType.Application.Json)
            setBody(
                ConfirmStocktakeCommand(
                    operationUuid = operationUuid,
                    operatorCode = operatorCode,
                )
            )
        }.body()
    }
}
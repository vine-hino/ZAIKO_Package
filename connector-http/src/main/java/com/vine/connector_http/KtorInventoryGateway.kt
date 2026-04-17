package com.vine.connector_http

import com.vine.connector_api.InventoryGateway
import com.vine.inventory_contract.InboundTransferContracts
import com.vine.inventory_contract.OutboundTransferContracts
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.client.request.contentType

class KtorInventoryGateway(
    private val baseUrl: String,
    private val client: HttpClient
) : InventoryGateway {

    override suspend fun exportInventory(): OutboundTransferContracts {
        return client.get("$baseUrl/inventory/export").body()
    }

    override suspend fun importInventory(contracts: InboundTransferContracts) {
        client.post("$baseUrl/inventory/import") {
            contentType(ContentType.Application.Json)
            setBody(contracts)
        }
    }
}
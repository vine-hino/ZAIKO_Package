package com.vine.connector_api

interface InventoryConnector {
    val spec: ConnectorSpec

    suspend fun registerInbound(request: InboundRequest): SubmitResult
}
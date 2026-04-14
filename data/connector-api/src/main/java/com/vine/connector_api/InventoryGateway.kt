package com.vine.connector_api

interface InventoryGateway {
    fun currentConnectionType(): ConnectionType

    suspend fun registerInbound(request: InboundRequest): SubmitResult
}
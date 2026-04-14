package com.vine.connector_db

import com.vine.connector_api.ConnectionType
import com.vine.connector_api.ConnectorSpec
import com.vine.connector_api.InboundRequest
import com.vine.connector_api.InventoryConnector
import com.vine.connector_api.SubmitResult

class DirectDbInventoryConnector : InventoryConnector {
    override val spec: ConnectorSpec =
        ConnectorSpec(
            type = ConnectionType.DIRECT_DB,
            displayName = "Direct DB",
            onlineLike = true,
        )

    override suspend fun registerInbound(request: InboundRequest): SubmitResult {
        return SubmitResult(
            accepted = false,
            message = "Direct DBコネクタは未実装です。実運用ではVPNや中継API経由を推奨します。",
        )
    }
}
package com.vine.connector_cloud

import com.vine.connector_api.ConnectionType
import com.vine.connector_api.ConnectorSpec
import com.vine.connector_api.InboundRequest
import com.vine.connector_api.InventoryConnector
import com.vine.connector_api.SubmitResult

class CloudInventoryConnector : InventoryConnector {
    override val spec: ConnectorSpec =
        ConnectorSpec(
            type = ConnectionType.CLOUD,
            displayName = "Cloud",
            onlineLike = true,
        )

    override suspend fun registerInbound(request: InboundRequest): SubmitResult {
        return SubmitResult(
            accepted = false,
            message = "Cloudコネクタは未実装です。将来的にはRetrofit/API連携をここに実装します。",
        )
    }
}
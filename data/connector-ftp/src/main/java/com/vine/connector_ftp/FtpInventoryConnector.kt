package com.vine.connector_ftp

import com.vine.connector_api.ConnectionType
import com.vine.connector_api.ConnectorSpec
import com.vine.connector_api.InboundRequest
import com.vine.connector_api.InventoryConnector
import com.vine.connector_api.SubmitResult

class FtpInventoryConnector : InventoryConnector {
    override val spec: ConnectorSpec =
        ConnectorSpec(
            type = ConnectionType.FTP,
            displayName = "FTP",
            onlineLike = false,
        )

    override suspend fun registerInbound(request: InboundRequest): SubmitResult {
        return SubmitResult(
            accepted = false,
            message = "FTPコネクタは未実装です。将来的にはCSV出力→FTP送信のバッチ連携に向いています。",
        )
    }
}
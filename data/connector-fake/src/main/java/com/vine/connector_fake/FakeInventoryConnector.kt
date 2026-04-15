package com.vine.connector_fake

import com.vine.connector_api.ConnectionType
import com.vine.connector_api.ConnectorSpec
import com.vine.connector_api.InboundRequest
import com.vine.connector_api.InventoryConnector
import com.vine.connector_api.SubmitResult
import javax.inject.Inject

class FakeInventoryConnector @Inject constructor(
    private val inboundRecordDao: InboundRecordDao,
) : InventoryConnector {

    override val spec: ConnectorSpec =
        ConnectorSpec(
            type = ConnectionType.FAKE,
            displayName = "Fake(Local Room)",
            onlineLike = true,
        )

    override suspend fun registerInbound(request: InboundRequest): SubmitResult {
        val id = inboundRecordDao.insert(
            InboundRecordEntity(
                productCode = request.productCode,
                locationCode = request.locationCode,
                quantity = request.quantity,
                note = request.note,
                createdAtEpochMillis = System.currentTimeMillis(),
                syncStatus = "LOCAL_ONLY",
            ),
        )

        return SubmitResult(
            accepted = true,
            message = "ローカル保存しました",
            referenceId = "LOCAL-$id",
        )
    }
}
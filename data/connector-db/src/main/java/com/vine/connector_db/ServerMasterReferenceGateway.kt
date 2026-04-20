package com.vine.connector_db

import com.vine.connector_api.MasterLookupItem
import com.vine.connector_api.MasterReferenceGateway
import com.vine.connector_api.MasterType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ServerMasterReferenceGateway(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : MasterReferenceGateway {

    override suspend fun search(
        type: MasterType,
        query: String,
        limit: Int,
        includeInactive: Boolean,
    ): List<MasterLookupItem> {
        if (query.isBlank()) return emptyList()

        return httpClient.get("$baseUrl/masters") {
            parameter("type", type.name)
            parameter("q", query)
            parameter("limit", limit)
            parameter("includeInactive", includeInactive)
        }.body()
    }
}

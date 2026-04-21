package com.vine.connector_db.client

import com.vine.connector_api.MasterLookupItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MasterServerClient @Inject constructor(
    private val client: HttpClient,
    @Named("serverBaseUrl") private val baseUrl: String,
) {
    suspend fun searchMasters(
        type: String,
        keyword: String?,
        includeInactive: Boolean,
        limit: Int,
    ): List<MasterLookupItem> {
        return client.get("$baseUrl/masters") {
            parameter("type", type.trim().uppercase())
            keyword?.trim()?.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
            parameter("includeInactive", includeInactive)
            parameter("limit", limit)
        }.body()
    }
}

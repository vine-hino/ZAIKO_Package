package com.vine.pc_app.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class PcMasterLookupItem(
    val type: String,
    val code: String,
    val name: String,
    val warehouseCode: String? = null,
    val parentCode: String? = null,
    val isActive: Boolean = true,
)

class MasterLookupClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun search(
        type: String,
        keyword: String? = null,
        includeInactive: Boolean = false,
        limit: Int = 200,
    ): List<PcMasterLookupItem> {
        return client.get("$baseUrl/masters") {
            parameter("type", type)
            keyword?.takeIf { it.isNotBlank() }?.let {
                parameter("q", it)
            }
            parameter("includeInactive", includeInactive)
            parameter("limit", limit)
        }.body()
    }
}
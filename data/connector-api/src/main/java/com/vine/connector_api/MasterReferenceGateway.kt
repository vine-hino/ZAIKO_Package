package com.vine.connector_api

interface MasterReferenceGateway {
    suspend fun search(
        type: MasterType,
        query: String,
        limit: Int = 20,
        includeInactive: Boolean = false,
    ): List<MasterLookupItem>
}

enum class MasterType {
    PRODUCT,
    WAREHOUSE,
    LOCATION,
    OPERATOR,
    REASON,
}

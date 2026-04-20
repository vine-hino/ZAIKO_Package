package com.vine.connector_api

import kotlinx.serialization.Serializable

@Serializable
data class MasterLookupItem(
    val type: String,
    val code: String,
    val name: String,
    val warehouseCode: String? = null,
    val parentCode: String? = null,
    val isActive: Boolean = true,
)
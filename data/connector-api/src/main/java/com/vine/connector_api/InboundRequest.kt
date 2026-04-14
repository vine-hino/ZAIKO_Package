package com.vine.connector_api

data class InboundRequest(
    val productCode: String,
    val locationCode: String,
    val quantity: Int,
    val note: String? = null,
)
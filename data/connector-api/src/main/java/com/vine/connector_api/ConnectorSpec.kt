package com.vine.connector_api

data class ConnectorSpec(
    val type: ConnectionType,
    val displayName: String,
    val onlineLike: Boolean,
)
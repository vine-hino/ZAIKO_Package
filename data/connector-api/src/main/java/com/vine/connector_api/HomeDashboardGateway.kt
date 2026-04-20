package com.vine.connector_api

data class HomeDashboardSummary(
    val todayInboundQuantity: Long = 0L,
    val todayOutboundQuantity: Long = 0L,
)

interface HomeDashboardGateway {
    suspend fun getSummary(): HomeDashboardSummary
}
package com.vine.ht_home

data class HtHomeUiState(
    val todayInbound: Long = 0L,
    val todayOutbound: Long = 0L,
    val unsyncedCount: Int = 0,
    val operatorName: String = "demo_user",
    val warehouseName: String = "TOKYO-01",
    val isLoading: Boolean = false,
)
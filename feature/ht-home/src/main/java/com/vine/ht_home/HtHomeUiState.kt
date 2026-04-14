package com.vine.ht_home

data class HtHomeUiState(
    val todayInbound: Int = 0,
    val todayOutbound: Int = 0,
    val unsyncedCount: Int = 0,
    val operatorName: String = "demo_user",
    val warehouseName: String = "TOKYO-01",
)
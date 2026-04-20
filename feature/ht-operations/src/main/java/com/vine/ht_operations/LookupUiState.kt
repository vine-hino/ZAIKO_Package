package com.vine.ht_operations

import com.vine.connector_api.MasterLookupItem

data class LookupUiState(
    val query: String = "",
    val candidates: List<MasterLookupItem> = emptyList(),
    val selected: MasterLookupItem? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

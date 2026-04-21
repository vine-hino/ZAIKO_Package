package com.vine.ht_operations.ui

import com.vine.connector_api.MasterLookupItem

data class HtInboundUiState(
    val productKeyword: String = "",
    val locationKeyword: String = "",
    val productCandidates: List<MasterLookupItem> = emptyList(),
    val locationCandidates: List<MasterLookupItem> = emptyList(),
    val selectedProduct: MasterLookupItem? = null,
    val selectedLocation: MasterLookupItem? = null,
    val quantity: String = "",
    val note: String = "",
    val isSearchingProducts: Boolean = false,
    val isSearchingLocations: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = selectedProduct != null &&
                selectedLocation != null &&
                quantity.toLongOrNull()?.let { it > 0L } == true &&
                !isSubmitting
}
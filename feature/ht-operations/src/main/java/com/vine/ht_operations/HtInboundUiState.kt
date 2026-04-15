package com.vine.ht_operations

data class HtInboundUiState(
    val productCode: String = "",
    val locationCode: String = "",
    val quantity: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val completedMessage: String? = null,
    val exportMessage: String? = null,
    val savedOperationUuid: String? = null,
) {
    val canSubmit: Boolean
        get() = productCode.isNotBlank() &&
                locationCode.isNotBlank() &&
                (quantity.toLongOrNull()?.let { it > 0L } == true) &&
                !isSaving &&
                !isExporting
}
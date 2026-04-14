package com.vine.ht_operations

data class HtInboundUiState(
    val productCode: String = "",
    val locationCode: String = "",
    val quantity: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completedMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = productCode.isNotBlank() &&
                locationCode.isNotBlank() &&
                quantity.toIntOrNull()?.let { it > 0 } == true &&
                !isSaving
}
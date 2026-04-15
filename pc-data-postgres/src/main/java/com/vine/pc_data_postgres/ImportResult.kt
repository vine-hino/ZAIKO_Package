package com.vine.pc_data_postgres

data class ImportResult(
    val success: Boolean,
    val message: String,
    val batchId: String? = null,
    val detailCount: Int = 0,
)
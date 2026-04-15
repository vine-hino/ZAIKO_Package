package com.vine.connector_api

data class SubmitResult(
    val accepted: Boolean,
    val message: String,
    val referenceId: String? = null,
    val operationUuid: String? = null,
)
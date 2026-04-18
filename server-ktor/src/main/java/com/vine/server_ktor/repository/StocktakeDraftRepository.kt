package com.vine.server_ktor.repository

import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary

interface StocktakeDraftRepository {
    suspend fun save(
        summary: StocktakeSummary,
        details: List<StocktakeDetail>,
    )

    suspend fun findSummaries(
        status: String? = "DRAFT",
    ): List<StocktakeSummary>

    suspend fun findDetails(
        operationUuid: String,
        diffOnly: Boolean = false,
    ): List<StocktakeDetail>

    suspend fun confirm(
        operationUuid: String,
    ): Boolean
}
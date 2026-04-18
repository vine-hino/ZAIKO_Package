package com.vine.server_ktor.repository

import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryStocktakeDraftRepository : StocktakeDraftRepository {

    private val mutex = Mutex()

    private data class StocktakeDraftRecord(
        val summary: StocktakeSummary,
        val details: List<StocktakeDetail>,
    )

    private val records = linkedMapOf<String, StocktakeDraftRecord>()

    override suspend fun save(
        summary: StocktakeSummary,
        details: List<StocktakeDetail>,
    ) {
        mutex.withLock {
            records[summary.operationUuid] = StocktakeDraftRecord(summary, details)
        }
    }

    override suspend fun findSummaries(
        status: String?,
    ): List<StocktakeSummary> {
        return mutex.withLock {
            records.values
                .map { it.summary }
                .filter { status == null || it.status == status }
                .sortedByDescending { it.stocktakeNo }
        }
    }

    override suspend fun findDetails(
        operationUuid: String,
        diffOnly: Boolean,
    ): List<StocktakeDetail> {
        return mutex.withLock {
            val details = records[operationUuid]?.details.orEmpty()
            if (diffOnly) {
                details.filter { it.diffQuantity != 0L }
            } else {
                details
            }
        }
    }

    override suspend fun confirm(
        operationUuid: String,
    ): Boolean {
        return mutex.withLock {
            val current = records[operationUuid] ?: return@withLock false
            records[operationUuid] = current.copy(
                summary = current.summary.copy(status = "CONFIRMED")
            )
            true
        }
    }
}
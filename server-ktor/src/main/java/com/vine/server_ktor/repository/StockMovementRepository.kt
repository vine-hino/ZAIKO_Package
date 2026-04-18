package com.vine.server_ktor.repository

import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockSummaryDto

interface StockMovementRepository {
    suspend fun save(movement: StockMovementDto)
    suspend fun findAll(): List<StockMovementDto>
    suspend fun findSummary(): List<StockSummaryDto>
}
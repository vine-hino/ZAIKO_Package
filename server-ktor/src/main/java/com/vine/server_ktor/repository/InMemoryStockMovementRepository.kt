package com.vine.server_ktor.repository

import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
import com.vine.inventory_contract.StockSummaryDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryStockMovementRepository : StockMovementRepository {

    private val mutex = Mutex()
    private val movements = mutableListOf<StockMovementDto>()

    override suspend fun save(movement: StockMovementDto) {
        mutex.withLock {
            movements.add(0, movement)
        }
    }

    override suspend fun findAll(): List<StockMovementDto> {
        return mutex.withLock {
            movements.toList()
        }
    }

    override suspend fun findSummary(): List<StockSummaryDto> {
        return mutex.withLock {
            movements
                .groupBy { it.itemId to it.itemName }
                .map { (key, values) ->
                    val currentQuantity = values.sumOf { movement ->
                        when (movement.operation) {
                            StockOperation.INBOUND -> movement.quantity
                            StockOperation.OUTBOUND -> -movement.quantity
                            StockOperation.ADJUST -> movement.quantity
                        }
                    }

                    StockSummaryDto(
                        itemId = key.first,
                        itemName = key.second,
                        currentQuantity = currentQuantity,
                    )
                }
                .sortedBy { it.itemName }
        }
    }
}
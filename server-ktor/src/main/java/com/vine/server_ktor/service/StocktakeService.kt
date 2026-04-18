package com.vine.server_ktor.service

import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.SaveStocktakeDraftRequest
import com.vine.inventory_contract.StockOperation
import com.vine.inventory_contract.StocktakeActionResult
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary
import com.vine.server_ktor.repository.StockMovementRepository
import com.vine.server_ktor.repository.StocktakeDraftRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class StocktakeService(
    private val stocktakeRepository: StocktakeDraftRepository,
    private val movementRepository: StockMovementRepository,
) {
    suspend fun saveDraft(
        request: SaveStocktakeDraftRequest,
    ): StocktakeSummary {
        val operationUuid = UUID.randomUUID().toString()
        val detailUuid = UUID.randomUUID().toString()
        val stocktakeNo = createStocktakeNo()

        val bookQuantity = movementRepository.findAll()
            .filter {
                it.itemId == request.productCode &&
                        it.warehouseCode == request.warehouseCode &&
                        it.locationCode == request.locationCode
            }
            .sumOf { movement ->
                when (movement.operation) {
                    StockOperation.INBOUND -> movement.quantity
                    StockOperation.OUTBOUND -> -movement.quantity
                }
            }

        val detail = StocktakeDetail(
            detailUuid = detailUuid,
            operationUuid = operationUuid,
            lineNo = 1,
            productCode = request.productCode,
            productName = request.productName,
            warehouseCode = request.warehouseCode,
            locationCode = request.locationCode,
            bookQuantity = bookQuantity,
            actualQuantity = request.actualQuantity,
            diffQuantity = request.actualQuantity - bookQuantity,
        )

        val summary = StocktakeSummary(
            operationUuid = operationUuid,
            stocktakeNo = stocktakeNo,
            stocktakeDate = request.stocktakeDate,
            warehouseCode = request.warehouseCode,
            warehouseName = request.warehouseCode,
            status = "DRAFT",
            lineCount = 1,
            enteredByName = request.operatorCode,
        )

        stocktakeRepository.save(
            summary = summary,
            details = listOf(detail),
        )

        return summary
    }

    suspend fun getDrafts(): List<StocktakeSummary> {
        return stocktakeRepository.findSummaries(status = "DRAFT")
    }

    suspend fun getDetails(
        operationUuid: String,
        diffOnly: Boolean,
    ): List<StocktakeDetail> {
        return stocktakeRepository.findDetails(
            operationUuid = operationUuid,
            diffOnly = diffOnly,
        )
    }

    suspend fun confirm(
        command: ConfirmStocktakeCommand,
    ): StocktakeActionResult {
        val updated = stocktakeRepository.confirm(command.operationUuid)

        return if (updated) {
            StocktakeActionResult(
                accepted = true,
                message = "棚卸を確定しました",
            )
        } else {
            StocktakeActionResult(
                accepted = false,
                message = "対象の棚卸が見つかりません",
            )
        }
    }

    private fun createStocktakeNo(): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        return "ST-$timestamp-${UUID.randomUUID().toString().take(8).uppercase()}"
    }
}
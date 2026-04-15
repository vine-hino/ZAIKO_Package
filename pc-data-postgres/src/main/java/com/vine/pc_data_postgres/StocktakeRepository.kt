package com.vine.pc_data_postgres

import com.vine.inventory_contract.ConfirmStocktakeCommand
import com.vine.inventory_contract.GetStocktakeDetailsQuery
import com.vine.inventory_contract.GetStocktakeSummariesQuery
import com.vine.inventory_contract.StocktakeDetail
import com.vine.inventory_contract.StocktakeSummary

data class ConfirmExecutionResult(
    val success: Boolean,
    val message: String,
)

interface StocktakeRepository {
    fun bootstrap()

    fun getSummaries(
        query: GetStocktakeSummariesQuery,
    ): List<StocktakeSummary>

    fun getDetails(
        query: GetStocktakeDetailsQuery,
    ): List<StocktakeDetail>

    fun confirm(
        command: ConfirmStocktakeCommand,
    ): ConfirmExecutionResult
}
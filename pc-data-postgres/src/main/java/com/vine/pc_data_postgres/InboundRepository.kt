package com.vine.pc_data_postgres

import com.vine.inventory_contract.GetInboundDetailsQuery
import com.vine.inventory_contract.GetInboundSummariesQuery
import com.vine.inventory_contract.InboundDetail
import com.vine.inventory_contract.InboundSummary

interface InboundRepository {
    fun bootstrap()

    fun getSummaries(
        query: GetInboundSummariesQuery,
    ): List<InboundSummary>

    fun getDetails(
        query: GetInboundDetailsQuery,
    ): List<InboundDetail>
}
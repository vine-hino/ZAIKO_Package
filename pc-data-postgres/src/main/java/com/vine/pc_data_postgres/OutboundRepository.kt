package com.vine.pc_data_postgres

import com.vine.inventory_contract.GetOutboundDetailsQuery
import com.vine.inventory_contract.GetOutboundSummariesQuery
import com.vine.inventory_contract.OutboundDetail
import com.vine.inventory_contract.OutboundSummary

interface OutboundRepository {
    fun bootstrap()

    fun getSummaries(
        query: GetOutboundSummariesQuery,
    ): List<OutboundSummary>

    fun getDetails(
        query: GetOutboundDetailsQuery,
    ): List<OutboundDetail>
}
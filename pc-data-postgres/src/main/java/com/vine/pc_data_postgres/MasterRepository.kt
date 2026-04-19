package com.vine.pc_data_postgres

import com.vine.inventory_contract.DeleteMasterRecordCommand
import com.vine.inventory_contract.GetMasterRecordsQuery
import com.vine.inventory_contract.MasterRecordDetail
import com.vine.inventory_contract.MasterRecordSummary
import com.vine.inventory_contract.MasterType
import com.vine.inventory_contract.SaveMasterRecordCommand

interface MasterRepository {
    fun bootstrap()

    fun getSummaries(
        query: GetMasterRecordsQuery,
    ): List<MasterRecordSummary>

    fun getDetail(
        type: MasterType,
        code: String,
    ): MasterRecordDetail?

    fun save(
        command: SaveMasterRecordCommand,
    ): MasterRecordDetail

    fun delete(
        command: DeleteMasterRecordCommand,
    )
}
package com.vine.inventory_contract

enum class MasterType(
    val label: String,
) {
    PRODUCT("商品"),
    WAREHOUSE("倉庫"),
    LOCATION("ロケーション"),
    OPERATOR("担当者"),
    REASON("理由"),
}

data class GetMasterRecordsQuery(
    val type: MasterType,
    val keyword: String? = null,
    val includeInactive: Boolean = true,
    val limit: Int = 200,
)

data class MasterRecordSummary(
    val type: MasterType,
    val code: String,
    val name: String,
    val warehouseCode: String?,
    val parentCode: String?,
    val sortOrder: Int,
    val isActive: Boolean,
)

data class MasterRecordDetail(
    val type: MasterType,
    val code: String,
    val name: String,
    val warehouseCode: String?,
    val parentCode: String?,
    val sortOrder: Int,
    val isActive: Boolean,
    val note: String?,
)

data class SaveMasterRecordCommand(
    val type: MasterType,
    val code: String,
    val name: String,
    val warehouseCode: String? = null,
    val parentCode: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val note: String? = null,
)

data class DeleteMasterRecordCommand(
    val type: MasterType,
    val code: String,
)
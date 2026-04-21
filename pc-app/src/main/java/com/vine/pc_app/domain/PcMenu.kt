package com.vine.pc_app.domain

enum class PcMenu(
    val title: String,
    val section: String,
) {
    DASHBOARD("ホーム", "HOME"),
    STOCKTAKE("棚卸管理", "OPERATIONS"),
    INBOUND("入庫管理", "OPERATIONS"),
    OUTBOUND("出庫管理", "OPERATIONS"),
    STOCK("在庫照会", "INVENTORY"),
    ADJUSTMENT("在庫調整", "INVENTORY"),
    MASTER("マスタ管理", "MASTER"),
    SYNC("同期管理", "SYSTEM"),
    SETTINGS("設定", "SYSTEM"),
}

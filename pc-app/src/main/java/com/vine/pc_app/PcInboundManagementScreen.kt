package com.vine.pc_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDateTime

@Composable
fun PcInboundManagementScreen() {
    // まずは UI 統合を優先して仮データで表示
    // repository 接続はこの listOf(...) を置き換えるだけで対応できます
    val inboundRows = remember {
//        loadInboundRows()


        listOf(
            InboundRowModel(
                inboundNo = "IN-20260417-001",
                receivedAt = LocalDateTime.now().minusHours(2),
                productCode = "P-001",
                productName = "検品用ラベル",
                barcode = "4901234567890",
                warehouseName = "東京倉庫",
                locationName = "A-01-01",
                quantity = 120,
                note = "午前便"
            ),
            InboundRowModel(
                inboundNo = "IN-20260416-004",
                receivedAt = LocalDateTime.now().minusDays(1),
                productCode = "P-002",
                productName = "梱包箱M",
                barcode = "4909999999999",
                warehouseName = "大阪倉庫",
                locationName = "B-02-03",
                quantity = 50,
                note = ""
            ),
            InboundRowModel(
                inboundNo = "IN-20260415-003",
                receivedAt = LocalDateTime.now().minusDays(2),
                productCode = "P-003",
                productName = "作業用手袋",
                barcode = "4901111111111",
                warehouseName = "東京倉庫",
                locationName = "A-02-04",
                quantity = 80,
                note = "午後便"
            )
        )
    }

    InboundListScreen(
        allRows = inboundRows,
        onOpenDetail = { row ->
            println("open inbound detail: ${row.inboundNo}")
        },
        onCreateNew = {
            println("open inbound create")
        }
    )
}

private fun loadInboundRows(): List<InboundRowModel> {
    // ここはあなたの InboundRepository の実際のメソッド名と
    // レコード型のプロパティ名に合わせて調整する場所です
    return emptyList()
}
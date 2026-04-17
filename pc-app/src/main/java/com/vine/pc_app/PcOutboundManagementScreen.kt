package com.vine.pc_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDateTime

@Composable
fun PcOutboundManagementScreen() {
    val outboundRows = remember {
        listOf(
            OutboundRowModel(
                outboundNo = "OUT-20260417-001",
                outboundAt = LocalDateTime.now().minusHours(1),
                productCode = "P-001",
                productName = "検品用ラベル",
                barcode = "4901234567890",
                warehouseName = "東京倉庫",
                locationName = "A-01-01",
                quantity = 30,
                note = "午後出荷"
            ),
            OutboundRowModel(
                outboundNo = "OUT-20260416-004",
                outboundAt = LocalDateTime.now().minusDays(1),
                productCode = "P-002",
                productName = "梱包箱M",
                barcode = "4909999999999",
                warehouseName = "大阪倉庫",
                locationName = "B-02-03",
                quantity = 12,
                note = ""
            )
        )
    }

    OutboundListScreen(
        allRows = outboundRows,
        onOpenDetail = { row ->
            println("open outbound detail: ${row.outboundNo}")
        },
        onCreateNew = {
            println("open outbound create")
        }
    )
}
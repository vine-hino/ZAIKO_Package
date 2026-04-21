package com.vine.pc_app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDateTime

data class OutboundRowModel(
    val outboundNo: String,
    val outboundAt: LocalDateTime,
    val productCode: String,
    val productName: String,
    val barcode: String? = null,
    val warehouseName: String,
    val locationName: String,
    val quantity: Int,
    val note: String = "",
    val operatorName: String? = null,
)

@Composable
fun OutboundListScreen(
    allRows: List<OutboundRowModel>,
    onOpenDetail: (OutboundRowModel) -> Unit = {},
    onCreateNew: () -> Unit = {},
) {
    val config = remember {
        OperationListConfig(
            screenTitle = "出庫一覧",
            screenDescription = "出庫実績を検索し、内容確認や詳細確認を行う画面です。",
            createActionText = "新規出庫",
            operationNoLabel = "出庫No",
            operationDateLabel = "出庫日",
            operationDateColumnLabel = "出庫日時",
            warehouseLabel = "出庫元倉庫",
            locationLabel = "出庫元ロケーション",
            emptyTitle = "該当する出庫データがありません",
            emptyDescription = "検索条件を変更して再度お試しください。"
        )
    }

    val mappedRows = remember(allRows) {
        allRows.map {
            OperationListRowModel(
                operationNo = it.outboundNo,
                operationAt = it.outboundAt,
                productCode = it.productCode,
                productName = it.productName,
                barcode = it.barcode,
                warehouseName = it.warehouseName,
                locationName = it.locationName,
                quantity = it.quantity,
                note = it.note,
                operatorName = it.operatorName
            )
        }
    }

    OperationListScreen(
        config = config,
        allRows = mappedRows,
        onOpenDetail = { clicked ->
            allRows.firstOrNull { it.outboundNo == clicked.operationNo }
                ?.let(onOpenDetail)
        },
        onCreateNew = onCreateNew
    )
}
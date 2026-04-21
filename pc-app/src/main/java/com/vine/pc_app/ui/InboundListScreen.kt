package com.vine.pc_app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDateTime

data class InboundRowModel(
    val inboundNo: String,
    val receivedAt: LocalDateTime,
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
fun InboundListScreen(
    allRows: List<InboundRowModel>,
    onOpenDetail: (InboundRowModel) -> Unit = {},
    onCreateNew: () -> Unit = {},
) {
    val config = remember {
        OperationListConfig(
            screenTitle = "入庫一覧",
            screenDescription = "入庫実績を検索し、内容確認や詳細確認を行う画面です。",
            createActionText = "新規入庫",
            operationNoLabel = "入庫No",
            operationDateLabel = "入庫日",
            operationDateColumnLabel = "入庫日時",
            warehouseLabel = "倉庫",
            locationLabel = "ロケーション",
            emptyTitle = "該当する入庫データがありません",
            emptyDescription = "検索条件を変更して再度お試しください。"
        )
    }

    val mappedRows = remember(allRows) {
        allRows.map {
            OperationListRowModel(
                operationNo = it.inboundNo,
                operationAt = it.receivedAt,
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
            allRows.firstOrNull { it.inboundNo == clicked.operationNo }
                ?.let(onOpenDetail)
        },
        onCreateNew = onCreateNew
    )
}
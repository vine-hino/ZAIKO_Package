package com.vine.pc_app.ui

import com.vine.pc_app.data.PcDependencies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.StockMovementDto
import com.vine.inventory_contract.StockOperation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val BizBg = Color(0xFFF3F5F7)
private val BizSurface = Color(0xFFFFFFFF)
private val BizSubtle = Color(0xFFF7F8FA)
private val BizBorder = Color(0xFFE3E8EF)
private val BizAccent = Color(0xFF1F4E79)
private val BizAccentSoft = Color(0xFFEAF2F8)
private val BizSuccess = Color(0xFF2E7D32)
private val BizSuccessSoft = Color(0xFFEAF6EC)
private val BizWarning = Color(0xFF9A6700)
private val BizWarningSoft = Color(0xFFFFF4DB)
private val BizDanger = Color(0xFFC62828)
private val BizDangerSoft = Color(0xFFFDECEC)
private val BizTextMuted = Color(0xFF667085)

private data class DashboardUiState(
    val inboundTodayCount: Int = 0,
    val outboundTodayCount: Int = 0,
    val pendingStocktakeCount: Int = 0,
    val serverStatus: String = "確認中",
    val serverSubText: String = "データ取得待ち",
    val recentMovements: List<StockMovementDto> = emptyList(),
    val errorMessage: String? = null,
)

@Composable
fun PcDashboardScreen(
    onOpenInbound: () -> Unit,
    onOpenOutbound: () -> Unit,
    onOpenStocktake: () -> Unit,
    onOpenMove: () -> Unit,
    onOpenAdjustment: () -> Unit,
    onOpenStock: () -> Unit,
    onOpenMaster: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var uiState by remember { mutableStateOf(DashboardUiState()) }

    suspend fun reload() {
        runCatching {
            val movements = PcDependencies.inventoryMovementClient
                .getMovements()
                .movements

            val stocktakeDrafts = PcDependencies.stocktakeServerClient
                .getDrafts()

            val today = LocalDate.now()

            val todaysMovements = movements.filter { movement ->
                runCatching {
                    OffsetDateTime.parse(movement.occurredAt).toLocalDate() == today
                }.getOrDefault(false)
            }

            uiState = DashboardUiState(
                inboundTodayCount = todaysMovements.count { it.operation == StockOperation.INBOUND },
                outboundTodayCount = todaysMovements.count { it.operation == StockOperation.OUTBOUND },
                pendingStocktakeCount = stocktakeDrafts.size,
                serverStatus = "ONLINE",
                serverSubText = "最終更新 ${java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}",
                recentMovements = movements
                    .sortedByDescending { it.occurredAt }
                    .take(8),
                errorMessage = null,
            )
        }.onFailure { error ->
            uiState = uiState.copy(
                serverStatus = "OFFLINE",
                serverSubText = "サーバー接続エラー",
                errorMessage = error.message ?: "ダッシュボードの取得に失敗しました",
            )
        }
    }

    LaunchedEffect(Unit) {
        reload()

        launch {
            runCatching {
                PcDependencies.inventoryRealtimeClient.connect {
                    reload()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BizBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardHeader(
                serverStatus = uiState.serverStatus,
                serverSubText = uiState.serverSubText,
            )

            if (uiState.errorMessage != null || uiState.pendingStocktakeCount > 0) {
                DashboardAttentionPanel(
                    errorMessage = uiState.errorMessage,
                    pendingStocktakeCount = uiState.pendingStocktakeCount,
                    onOpenStocktake = onOpenStocktake,
                    onOpenSync = onOpenSync,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "本日入庫",
                    value = "${uiState.inboundTodayCount}件",
                    subText = "本日登録済みの入庫件数",
                    highlightColor = BizAccent,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "本日出庫",
                    value = "${uiState.outboundTodayCount}件",
                    subText = "本日登録済みの出庫件数",
                    highlightColor = BizSuccess,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "未確定棚卸",
                    value = "${uiState.pendingStocktakeCount}件",
                    subText = "確認が必要な棚卸ドラフト",
                    highlightColor = if (uiState.pendingStocktakeCount > 0) BizWarning else BizAccent,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "直近実績",
                    value = "${uiState.recentMovements.size}件",
                    subText = "画面表示中の最新処理件数",
                    highlightColor = BizAccent,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardMenuSection(
                        title = "主要業務",
                        description = "日常業務で使用する画面",
                        actions = listOf(
                            DashboardActionItem("入庫", "入庫一覧・検索・確認", onOpenInbound),
                            DashboardActionItem("出庫", "出庫一覧・検索・確認", onOpenOutbound),
                            DashboardActionItem("棚卸", "棚卸ドラフト・差異確認", onOpenStocktake),
                            DashboardActionItem("移動", "ロケーション・倉庫間移動", onOpenMove),
                            DashboardActionItem("在庫調整", "差異・補正登録", onOpenAdjustment),
                            DashboardActionItem("在庫照会", "商品・倉庫・履歴照会", onOpenStock),
                        )
                    )

                    DashboardMenuSection(
                        title = "管理・設定",
                        description = "マスタ・同期・システム関連",
                        actions = listOf(
                            DashboardActionItem("マスタ管理", "商品・倉庫・ロケーション管理", onOpenMaster),
                            DashboardActionItem("接続/同期管理", "サーバー状態・再接続確認", onOpenSync),
                            DashboardActionItem("システム設定", "接続先・端末設定", onOpenSettings),
                        )
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardStatusPanel(
                        serverStatus = uiState.serverStatus,
                        serverSubText = uiState.serverSubText,
                        pendingStocktakeCount = uiState.pendingStocktakeCount,
                        onOpenSync = onOpenSync,
                    )

                    DashboardRecentMovementTable(
                        movements = uiState.recentMovements
                    )
                }
            }
        }
    }
}

private data class DashboardActionItem(
    val title: String,
    val description: String,
    val onClick: () -> Unit,
)

@Composable
private fun DashboardHeader(
    serverStatus: String,
    serverSubText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "在庫業務ダッシュボード",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "本日の処理状況と主要業務へのショートカット",
                style = MaterialTheme.typography.bodyMedium,
                color = BizTextMuted,
            )
        }

        StatusBadge(
            status = serverStatus,
            subText = serverSubText,
        )
    }
}

@Composable
private fun StatusBadge(
    status: String,
    subText: String,
) {
    val isOnline = status == "ONLINE"

    Row(
        modifier = Modifier
            .background(
                color = if (isOnline) BizSuccessSoft else BizDangerSoft,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .background(
                    color = if (isOnline) BizSuccess else BizDanger,
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Column {
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) BizSuccess else BizDanger,
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = BizTextMuted,
            )
        }
    }
}

@Composable
private fun DashboardAttentionPanel(
    errorMessage: String?,
    pendingStocktakeCount: Int,
    onOpenStocktake: () -> Unit,
    onOpenSync: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BizSurface),
        border = BorderStroke(1.dp, BizBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "要確認",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            errorMessage?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BizDangerSoft, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "接続異常",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BizDanger,
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = BizTextMuted,
                        )
                    }
                    Text(
                        text = "同期管理へ",
                        modifier = Modifier.clickable { onOpenSync() },
                        style = MaterialTheme.typography.labelLarge,
                        color = BizAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (pendingStocktakeCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BizWarningSoft, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "棚卸確認待ち",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BizWarning,
                        )
                        Text(
                            text = "未確定の棚卸ドラフトが ${pendingStocktakeCount} 件あります",
                            style = MaterialTheme.typography.bodySmall,
                            color = BizTextMuted,
                        )
                    }
                    Text(
                        text = "棚卸へ",
                        modifier = Modifier.clickable { onOpenStocktake() },
                        style = MaterialTheme.typography.labelLarge,
                        color = BizAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    highlightColor: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BizSurface),
        border = BorderStroke(1.dp, BizBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = BizSubtle,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = BizTextMuted,
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = highlightColor,
            )

            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = BizTextMuted,
            )
        }
    }
}

@Composable
private fun DashboardMenuSection(
    title: String,
    description: String,
    actions: List<DashboardActionItem>,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BizSurface),
        border = BorderStroke(1.dp, BizBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = BizTextMuted,
            )

            actions.forEachIndexed { index, action ->
                DashboardActionCard(
                    title = action.title,
                    description = action.description,
                    onClick = action.onClick,
                )

                if (index != actions.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BizSurface),
        border = BorderStroke(1.dp, BizBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BizTextMuted,
                )
            }

            Text(
                text = "開く",
                style = MaterialTheme.typography.labelLarge,
                color = BizAccent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DashboardStatusPanel(
    serverStatus: String,
    serverSubText: String,
    pendingStocktakeCount: Int,
    onOpenSync: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BizSurface),
        border = BorderStroke(1.dp, BizBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "システム状況",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "接続状態",
                        style = MaterialTheme.typography.bodySmall,
                        color = BizTextMuted,
                    )
                    Text(
                        text = serverStatus,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (serverStatus == "ONLINE") BizSuccess else BizDanger,
                    )
                }

                Text(
                    text = "同期管理へ",
                    modifier = Modifier.clickable { onOpenSync() },
                    style = MaterialTheme.typography.labelLarge,
                    color = BizAccent,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(color = BizBorder)

            DashboardStatusRow("最終状態", serverSubText)
            DashboardStatusRow(
                "未確定棚卸",
                if (pendingStocktakeCount > 0) "${pendingStocktakeCount}件" else "なし"
            )
            DashboardStatusRow(
                "リアルタイム反映",
                if (serverStatus == "ONLINE") "有効" else "停止中"
            )
        }
    }
}

@Composable
private fun DashboardStatusRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = BizTextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DashboardRecentMovementTable(
    movements: List<StockMovementDto>,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BizSurface),
        border = BorderStroke(1.dp, BizBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "直近処理履歴",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (movements.isEmpty()) {
                Text(
                    text = "表示対象の処理履歴はありません",
                    style = MaterialTheme.typography.bodySmall,
                    color = BizTextMuted,
                )
            } else {
                DashboardRecentMovementHeader()

                movements.forEachIndexed { index, movement ->
                    DashboardRecentMovementRow(movement)
                    if (index != movements.lastIndex) {
                        HorizontalDivider(color = BizBorder)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRecentMovementHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BizSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "伝票/区分",
            modifier = Modifier.weight(1.7f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BizTextMuted,
        )
        Text(
            text = "商品",
            modifier = Modifier.weight(1.8f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BizTextMuted,
        )
        Text(
            text = "倉庫",
            modifier = Modifier.weight(1.3f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BizTextMuted,
        )
        Text(
            text = "数量",
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BizTextMuted,
        )
        Text(
            text = "担当/時刻",
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BizTextMuted,
        )
    }
}

@Composable
private fun DashboardRecentMovementRow(
    movement: StockMovementDto,
) {
    val operationLabel = when (movement.operation) {
        StockOperation.INBOUND -> "入庫"
        StockOperation.OUTBOUND -> "出庫"
        StockOperation.ADJUST -> "調整"
    }

    val operationColor = when (movement.operation) {
        StockOperation.INBOUND -> BizAccent
        StockOperation.OUTBOUND -> BizSuccess
        StockOperation.ADJUST -> BizWarning
    }

    val operationBg = when (movement.operation) {
        StockOperation.INBOUND -> BizAccentSoft
        StockOperation.OUTBOUND -> BizSuccessSoft
        StockOperation.ADJUST -> BizWarningSoft
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1.7f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = movement.referenceNo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .background(operationBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = operationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = operationColor,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1.8f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = movement.itemName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = movement.itemId,
                style = MaterialTheme.typography.bodySmall,
                color = BizTextMuted,
            )
        }

        Text(
            text = "${movement.warehouseCode}/${movement.locationCode}",
            modifier = Modifier.weight(1.3f),
            style = MaterialTheme.typography.bodySmall,
            color = BizTextMuted,
        )

        Text(
            text = movement.quantity.toString(),
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )

        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = movement.operatorName,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = formatOccurredAt(movement.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = BizTextMuted,
            )
        }
    }
}

private fun formatOccurredAt(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
    }.getOrElse {
        value
    }
}
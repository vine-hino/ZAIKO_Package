package com.vine.pc_app

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

private val HomeStatBlue = Color(0xFFF0F4FF)
private val HomeStatGreen = Color(0xFFF1FBF6)
private val HomeStatOrange = Color(0xFFFFF8EC)
private val HomeStatPurple = Color(0xFFF7F2FF)

private val HomeActionInbound = Color(0xFFEFF6FF)
private val HomeActionOutbound = Color(0xFFFDF2F8)
private val HomeActionStocktake = Color(0xFFF0FDF4)
private val HomeActionMove = Color(0xFFFFFBEB)
private val HomeActionAdjustment = Color(0xFFFFF7ED)
private val HomeActionStock = Color(0xFFF5F3FF)
private val HomeActionMaster = Color(0xFFF8FAFC)
private val HomeActionSync = Color(0xFFECFEFF)
private val HomeActionSettings = Color(0xFFF4F4F5)

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
                serverStatus = "Online",
                serverSubText = "最新更新 ${java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}",
                recentMovements = movements
                    .sortedByDescending { it.occurredAt }
                    .take(5),
                errorMessage = null,
            )
        }.onFailure { error ->
            uiState = uiState.copy(
                serverStatus = "Offline",
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "本日入庫",
                value = "${uiState.inboundTodayCount}件",
                subText = "今日登録された入庫件数",
                backgroundColor = HomeStatBlue,
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "本日出庫",
                value = "${uiState.outboundTodayCount}件",
                subText = "今日登録された出庫件数",
                backgroundColor = HomeStatGreen,
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "棚卸差異",
                value = "${uiState.pendingStocktakeCount}件",
                subText = "未確定の棚卸ドラフト",
                backgroundColor = HomeStatOrange,
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "接続状態",
                value = uiState.serverStatus,
                subText = uiState.serverSubText,
                backgroundColor = HomeStatPurple,
            )
        }

        uiState.errorMessage?.let { message ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "業務メニュー",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "各業務画面へ移動します",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "入庫",
                        description = "入庫一覧・検索・確認",
                        accentColor = HomeActionInbound,
                        onClick = onOpenInbound,
                    )
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "出庫",
                        description = "出庫一覧・検索・確認",
                        accentColor = HomeActionOutbound,
                        onClick = onOpenOutbound,
                    )
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "棚卸",
                        description = "棚卸ドラフト・差異確認",
                        accentColor = HomeActionStocktake,
                        onClick = onOpenStocktake,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "移動",
                        description = "ロケーション・倉庫間移動",
                        accentColor = HomeActionMove,
                        onClick = onOpenMove,
                    )
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "在庫調整",
                        description = "差異や補正の登録",
                        accentColor = HomeActionAdjustment,
                        onClick = onOpenAdjustment,
                    )
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "在庫照会",
                        description = "商品・倉庫・履歴照会",
                        accentColor = HomeActionStock,
                        onClick = onOpenStock,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "マスタ管理",
                        description = "商品・倉庫・ロケーション管理",
                        accentColor = HomeActionMaster,
                        onClick = onOpenMaster,
                    )
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "同期管理",
                        description = "サーバー状態・再接続確認",
                        accentColor = HomeActionSync,
                        onClick = onOpenSync,
                    )
                    DashboardActionCard(
                        modifier = Modifier.weight(1f),
                        title = "設定",
                        description = "接続先・端末設定",
                        accentColor = HomeActionSettings,
                        onClick = onOpenSettings,
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "最新実績",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                if (uiState.recentMovements.isEmpty()) {
                    Text(
                        text = "まだ実績がありません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.recentMovements.forEach { movement ->
                        DashboardRecentMovementRow(movement)
                        HorizontalDivider()
                    }
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
    backgroundColor: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DashboardActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(170.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "開く →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DashboardRecentMovementRow(
    movement: StockMovementDto,
) {
    val operationLabel = when (movement.operation) {
        StockOperation.INBOUND -> "入庫"
        StockOperation.OUTBOUND -> "出庫"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${movement.referenceNo}  $operationLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${movement.itemId} / ${movement.itemName}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${movement.warehouseCode} / ${movement.locationCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${movement.quantity}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = movement.operatorName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatOccurredAt(movement.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
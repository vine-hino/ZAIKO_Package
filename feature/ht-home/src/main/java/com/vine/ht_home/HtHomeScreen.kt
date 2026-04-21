package com.vine.ht_home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vine.designsystem.component.ZaikoScreenScaffold

@Composable
fun HtHomeRoute(
    onStockClick: () -> Unit,
    onInboundClick: () -> Unit,
    onOutboundClick: () -> Unit,
    onStocktakeClick: () -> Unit,
    viewModel: HtHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ZaikoScreenScaffold(title = "HT ホーム") { padding ->
        HtHomeContent(
            padding = padding,
            uiState = uiState,
            onStockClick = onStockClick,
            onInboundClick = onInboundClick,
            onOutboundClick = onOutboundClick,
            onStocktakeClick = onStocktakeClick,
            onRefresh = viewModel::refresh,
        )
    }
}

private data class HtMenuItem(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
private fun HtHomeContent(
    padding: PaddingValues,
    uiState: HtHomeUiState,
    onStockClick: () -> Unit,
    onInboundClick: () -> Unit,
    onOutboundClick: () -> Unit,
    onStocktakeClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    val menuItems = listOf(
        HtMenuItem("在庫照会", "商品・ロケーション別の在庫確認", onStockClick),
        HtMenuItem("入庫登録", "受入・格納の登録", onInboundClick),
        HtMenuItem("出庫登録", "払出・出荷の登録", onOutboundClick),
        HtMenuItem("棚卸入力", "実棚数の入力と差異確認", onStocktakeClick),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
//        HeaderPanel(
//            operatorName = uiState.operatorName,
//            warehouseName = uiState.warehouseName,
//        )

        Spacer(modifier = Modifier.height(12.dp))

        SummaryRow(
            todayInbound = uiState.todayInbound,
            todayOutbound = uiState.todayOutbound,
            unsyncedCount = uiState.unsyncedCount,
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        ) {
            Text(if (uiState.isLoading) "更新中..." else "ホーム集計を更新")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "作業メニュー",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(menuItems) { item ->
                OperationCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick,
                )
            }
        }
    }
}

//@Composable
//private fun HeaderPanel(
//    operatorName: String,
//    warehouseName: String,
//) {
//    val gradient = Brush.horizontalGradient(
//        colors = listOf(
//            MaterialTheme.colorScheme.primary,
//            MaterialTheme.colorScheme.primaryContainer,
//        ),
//    )
//
//    Surface(
//        shape = RoundedCornerShape(20.dp),
//        tonalElevation = 2.dp,
//        shadowElevation = 4.dp,
//        modifier = Modifier.fillMaxWidth(),
//    ) {
//        Box(
//            modifier = Modifier
//                .background(gradient)
//                .padding(16.dp),
//        ) {
//            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                Text(
//                    text = "在庫管理 HT",
//                    style = MaterialTheme.typography.headlineSmall,
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    fontWeight = FontWeight.Bold,
//                )
//                Text(
//                    text = "本日作業を開始できます",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onPrimary,
//                )
//                Text(
//                    text = "担当者: $operatorName / 倉庫: $warehouseName",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onPrimary,
//                )
//            }
//        }
//    }
//}

@Composable
private fun SummaryRow(
    todayInbound: Long,
    todayOutbound: Long,
    unsyncedCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "本日入庫",
            value = todayInbound.toString(),
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "本日出庫",
            value = todayOutbound.toString(),
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "未同期",
            value = unsyncedCount.toString(),
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OperationCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("開く")
            }
        }
    }
}

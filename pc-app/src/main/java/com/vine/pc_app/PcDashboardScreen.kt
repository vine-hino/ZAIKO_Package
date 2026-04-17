package com.vine.pc_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val HomeHeroBg = Color(0xFFF3F7FF)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
//        Card(
//            shape = RoundedCornerShape(22.dp),
//            colors = CardDefaults.cardColors(containerColor = HomeHeroBg),
//        ) {
//            Column(
//                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
//                verticalArrangement = Arrangement.spacedBy(6.dp),
//            ) {
//                Text(
//                    text = "ホーム",
//                    style = MaterialTheme.typography.headlineMedium,
//                    fontWeight = FontWeight.Bold,
//                )
//                Text(
//                    text = "上段で状況を把握し、下段のボタンから必要な業務画面へ移動します。",
//                    style = MaterialTheme.typography.bodyLarge,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                )
//            }
//        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "本日入庫",
                value = "12件",
                subText = "最新更新 10:42",
                backgroundColor = HomeStatBlue,
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "本日出庫",
                value = "8件",
                subText = "最新更新 10:20",
                backgroundColor = HomeStatGreen,
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "棚卸差異",
                value = "3件",
                subText = "確認待ちあり",
                backgroundColor = HomeStatOrange,
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "同期状態",
                value = "正常",
                subText = "最終同期 10:30",
                backgroundColor = HomeStatPurple,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "業務メニュー",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "入庫",
                    description = "入庫一覧・検索・確認",
                    backgroundColor = HomeActionInbound,
                    onClick = onOpenInbound,
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "出庫",
                    description = "出庫一覧・検索・確認",
                    backgroundColor = HomeActionOutbound,
                    onClick = onOpenOutbound,
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "棚卸",
                    description = "棚卸登録・差異確認",
                    backgroundColor = HomeActionStocktake,
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
                    backgroundColor = HomeActionMove,
                    onClick = onOpenMove,
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "在庫調整",
                    description = "差異や補正の登録",
                    backgroundColor = HomeActionAdjustment,
                    onClick = onOpenAdjustment,
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "在庫照会",
                    description = "商品・倉庫・履歴照会",
                    backgroundColor = HomeActionStock,
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
                    backgroundColor = HomeActionMaster,
                    onClick = onOpenMaster,
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "同期管理",
                    description = "JSON取込・再同期・履歴確認",
                    backgroundColor = HomeActionSync,
                    onClick = onOpenSync,
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    title = "設定",
                    description = "DB・同期・端末設定",
                    backgroundColor = HomeActionSettings,
                    onClick = onOpenSettings,
                )
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "運用メモ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "・入庫画面は利用可能",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "・次はホーム導線を基準に出庫・棚卸へ横展開",
                    style = MaterialTheme.typography.bodyMedium,
                )
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
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "開く →",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
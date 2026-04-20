package com.vine.pc_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.pcapp.StocktakeConfirmScreen

@Composable
fun PcAppShell() {
    val selectedMenu = remember { mutableStateOf(PcMenu.DASHBOARD) }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            PcTopBar(
                selected = selectedMenu.value,
                onGoHome = { selectedMenu.value = PcMenu.DASHBOARD },
            )

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                when (selectedMenu.value) {
                    PcMenu.DASHBOARD -> PcDashboardScreen(
                        onOpenInbound = { selectedMenu.value = PcMenu.INBOUND },
                        onOpenOutbound = { selectedMenu.value = PcMenu.OUTBOUND },
                        onOpenStocktake = { selectedMenu.value = PcMenu.STOCKTAKE },
                        onOpenMove = { selectedMenu.value = PcMenu.MOVE },
                        onOpenAdjustment = { selectedMenu.value = PcMenu.ADJUSTMENT },
                        onOpenStock = { selectedMenu.value = PcMenu.STOCK },
                        onOpenMaster = { selectedMenu.value = PcMenu.MASTER },
                        onOpenSync = { selectedMenu.value = PcMenu.SYNC },
                        onOpenSettings = { selectedMenu.value = PcMenu.SETTINGS },
                    )

                    PcMenu.STOCKTAKE -> PcStocktakeManagementScreen()

                    PcMenu.INBOUND -> PcInboundManagementScreen()

                    PcMenu.OUTBOUND -> PcOutboundManagementScreen()

                    PcMenu.MOVE -> PcPlaceholderScreen(
                        title = "移動管理",
                        description = "将来はロケーション移動、倉庫間移動の管理をここへ集約します。",
                    )

                    PcMenu.ADJUSTMENT -> PcAdjustmentManagementScreen()

                    PcMenu.STOCK -> PcStockReferenceScreen()

                    PcMenu.MASTER -> PcMasterManagementScreen()

                    PcMenu.SYNC -> PcPlaceholderScreen(
                        title = "同期管理",
                        description = "将来は JSON 取込履歴、エラー、再取込、重複確認をここで管理します。",
                    )

                    PcMenu.SETTINGS -> PcPlaceholderScreen(
                        title = "設定",
                        description = "将来は DB 接続先、同期フォルダ、端末設定をここで管理します。",
                    )
                }
            }
        }
    }
}

@Composable
private fun PcTopBar(
    selected: PcMenu,
    onGoHome: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != PcMenu.DASHBOARD) {
            Card(
                modifier = Modifier
                    .clickable { onGoHome() }
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "← ホームへ戻る",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = selected.title,
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.weight(1f))

        TopBarChip("DB", "PostgreSQL")
        Spacer(modifier = Modifier.width(8.dp))
        TopBarChip("同期", "JSON連携")
        Spacer(modifier = Modifier.width(8.dp))
        TopBarChip("担当", "OP-0001")
    }
}

@Composable
private fun TopBarChip(
    label: String,
    value: String,
) {
    Card {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
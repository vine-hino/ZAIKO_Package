package com.vine.pc_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
        Row(modifier = Modifier.fillMaxSize()) {
            PcSidebar(
                selected = selectedMenu.value,
                onSelect = { selectedMenu.value = it },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                PcTopBar(
                    selected = selectedMenu.value,
                )

                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    when (selectedMenu.value) {
                        PcMenu.DASHBOARD -> PcDashboardScreen()
                        PcMenu.STOCKTAKE -> StocktakeConfirmScreen()

                        PcMenu.INBOUND -> PcInboundManagementScreen()

                        PcMenu.OUTBOUND -> PcOutboundManagementScreen()

                        PcMenu.MOVE -> PcPlaceholderScreen(
                            title = "移動管理",
                            description = "将来はロケーション移動、倉庫間移動の管理をここへ集約します。",
                        )
                        PcMenu.ADJUSTMENT -> PcPlaceholderScreen(
                            title = "在庫調整",
                            description = "将来は調整理由付きで手動補正や差異調整を行います。",
                        )
                        PcMenu.STOCK -> PcPlaceholderScreen(
                            title = "在庫照会",
                            description = "将来は商品別、ロケーション別、倉庫別に在庫・履歴を照会します。",
                        )
                        PcMenu.MASTER -> PcPlaceholderScreen(
                            title = "マスタ管理",
                            description = "将来は商品、倉庫、ロケーション、担当者、理由を管理します。",
                        )
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
}

@Composable
private fun PcSidebar(
    selected: PcMenu,
    onSelect: (PcMenu) -> Unit,
) {
    val menus = PcMenu.entries

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "ZAIKO PACKAGE",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "PC 管理コンソール",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        var currentSection = ""

        menus.forEach { menu ->
            if (menu.section != currentSection) {
                currentSection = menu.section
                Text(
                    text = currentSection,
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val selectedColor = if (menu == selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(menu) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(selectedColor)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = menu.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Card {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("運用メモ", style = MaterialTheme.typography.titleSmall)
                Text("棚卸管理は利用可能", style = MaterialTheme.typography.bodySmall)
                Text("他メニューは準備中", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PcTopBar(
    selected: PcMenu,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Row(
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
package com.vine.pc_app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.GetStocktakeSummariesQuery
import com.vine.inventory_contract.StocktakeSummary
import com.vine.pc_app.PcDependencies

@Composable
fun PcDashboardScreen() {
    val repository = PcDependencies.stocktakeRepository
    val draftStocktakes = repository.getSummaries(
        GetStocktakeSummariesQuery(
            status = "DRAFT",
            limit = 5,
        ),
    )

    val confirmedStocktakes = repository.getSummaries(
        GetStocktakeSummariesQuery(
            status = "CONFIRMED",
            limit = 5,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "ダッシュボード",
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "未確定棚卸",
                value = draftStocktakes.size.toString(),
                sub = "PCで確認・確定待ち",
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "確定済棚卸",
                value = confirmedStocktakes.size.toString(),
                sub = "直近データ件数",
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "同期方式",
                value = "JSON",
                sub = "HT → PC 取込",
            )
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "DB",
                value = "PostgreSQL",
                sub = "ローカル正本",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardListCard(
                modifier = Modifier.weight(1f),
                title = "未確定棚卸",
                emptyText = "未確定棚卸はありません",
                items = draftStocktakes,
            )

            DashboardListCard(
                modifier = Modifier.weight(1f),
                title = "確定済棚卸",
                emptyText = "確定済棚卸はありません",
                items = confirmedStocktakes,
            )
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DashboardListCard(
    modifier: Modifier = Modifier,
    title: String,
    emptyText: String,
    items: List<StocktakeSummary>,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.operationUuid }) { row ->
                        Card {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = row.stocktakeNo,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text("棚卸日: ${row.stocktakeDate}")
                                Text("倉庫: ${row.warehouseName ?: row.warehouseCode.orEmpty()}")
                                Text("状態: ${row.status}")
                                Text("明細件数: ${row.lineCount}")
                            }
                        }
                    }
                }
            }
        }
    }
}
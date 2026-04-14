package com.vine.ht_home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vine.designsystem.component.ZaikoScreenScaffold

@Composable
fun HtHomeRoute(
    onStockClick: () -> Unit,
    onInboundClick: () -> Unit,
    onOutboundClick: () -> Unit,
    onMoveClick: () -> Unit,
    onStocktakeClick: () -> Unit,
    onAdjustmentClick: () -> Unit,
) {
    ZaikoScreenScaffold(title = "HT ホーム") { padding ->
        HtHomeContent(
            padding = padding,
            onStockClick = onStockClick,
            onInboundClick = onInboundClick,
            onOutboundClick = onOutboundClick,
            onMoveClick = onMoveClick,
            onStocktakeClick = onStocktakeClick,
            onAdjustmentClick = onAdjustmentClick,
        )
    }
}

@Composable
private fun HtHomeContent(
    padding: PaddingValues,
    onStockClick: () -> Unit,
    onInboundClick: () -> Unit,
    onOutboundClick: () -> Unit,
    onMoveClick: () -> Unit,
    onStocktakeClick: () -> Unit,
    onAdjustmentClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MenuButton(label = "在庫照会", onClick = onStockClick)
        MenuButton(label = "入庫登録", onClick = onInboundClick)
        MenuButton(label = "出庫登録", onClick = onOutboundClick)
        MenuButton(label = "在庫移動", onClick = onMoveClick)
        MenuButton(label = "棚卸入力", onClick = onStocktakeClick)
        MenuButton(label = "在庫調整", onClick = onAdjustmentClick)
    }
}

@Composable
private fun MenuButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(text = label)
    }
}
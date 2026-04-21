package com.vine.pc_app.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// -------------------------
// 共通カラー
// -------------------------
val OperationPageBg = Color(0xFFF7F9FC)           // 画面全体の背景
val OperationHeaderBg = Color(0xFFEEF4FF)         // 画面上部ヘッダー
val OperationSearchBg = Color(0xFFF2FBF7)         // 検索条件カード
val OperationResultBg = Color(0xFFFFFFFF)         // 結果エリア
val OperationResultTitleBg = Color(0xFFEEF1FF)    // 結果タイトル帯
val OperationTableHeaderBg = Color(0xFFE7ECFA)    // テーブルヘッダー
val OperationRowEvenBg = Color(0xFFFFFFFF)        // 偶数行
val OperationRowOddBg = Color(0xFFF9FBFF)         // 奇数行

val SummaryCountBg = Color(0xFFF0F4FF)            // 件数サマリ
val SummaryQtyBg = Color(0xFFF1FBF6)              // 数量サマリ
val SummaryWarehouseBg = Color(0xFFFFF8EC)        // 倉庫/対象数サマリ

// -------------------------
// 共通フォーマッタ
// -------------------------
val operationDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val operationDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
val operationQuantityFormatter = DecimalFormat("#,##0")

// -------------------------
// 画面上部ヘッダー
// -------------------------
@Composable
fun OperationHeaderCard(
    title: String,
    description: String,
    totalText: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OperationHeaderBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

// -------------------------
// 検索条件カード
// -------------------------
@Composable
fun OperationSearchCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OperationSearchBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "検索条件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

// -------------------------
// サマリカード
// -------------------------
@Composable
fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    backgroundColor: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------
// 検索結果カード
// -------------------------
@Composable
fun OperationResultCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OperationResultBg)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OperationResultTitleBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "検索結果",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

@Composable
fun OperationEmptyState(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------
// テーブルセル
// -------------------------
@Composable
fun RowScope.OperationHeaderCell(
    text: String,
    weight: Float,
) {
    Box(
        modifier = Modifier.weight(weight)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RowScope.OperationBodyCell(
    text: String,
    weight: Float,
) {
    Box(
        modifier = Modifier.weight(weight)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -------------------------
// 日付ピッカー
// -------------------------
@Composable
fun DatePickerField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isError: Boolean = false,
    onDateSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDate = parseOperationDateOrNull(value)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label) },
            placeholder = { Text("yyyy-MM-dd") },
            trailingIcon = { Text("📅") },
            supportingText = {
                if (isError) {
                    Text("日付形式を確認してください")
                }
            }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CalendarPicker(
                selectedDate = selectedDate,
                initialMonth = selectedDate ?: LocalDate.now(),
                onSelect = {
                    onDateSelected(it.format(operationDateFormatter))
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun CalendarPicker(
    selectedDate: LocalDate?,
    initialMonth: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    var currentMonth by remember(initialMonth) {
        mutableStateOf(YearMonth.from(initialMonth))
    }

    val firstDayOfMonth = currentMonth.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val calendarStartDate = firstDayOfMonth.minusDays(startOffset.toLong())
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .width(300.dp)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(
                onClick = { currentMonth = currentMonth.minusMonths(1) }
            ) {
                Text("＜")
            }

            Text(
                text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            androidx.compose.material3.TextButton(
                onClick = { currentMonth = currentMonth.plusMonths(1) }
            ) {
                Text("＞")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "月", "火", "水", "木", "金", "土").forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        repeat(6) { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                repeat(7) { dayIndex ->
                    val date = calendarStartDate.plusDays((week * 7 + dayIndex).toLong())
                    val isCurrentMonth = date.monthValue == currentMonth.monthValue
                    val isSelected = date == selectedDate
                    val isToday = date == today

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(36.dp)
                            .background(
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelect(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            androidx.compose.material3.TextButton(
                onClick = { onSelect(LocalDate.now()) }
            ) {
                Text("今日")
            }
        }
    }
}

// -------------------------
// 小さいクイック期間ボタン
// -------------------------
@Composable
fun CompactQuickRangeButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// -------------------------
// ドロップダウン
// -------------------------
@Composable
fun SimpleDropdownField(
    modifier: Modifier = Modifier,
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text(
                    text = selectedValue.ifBlank { "すべて" },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("▼")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("すべて") },
                    onClick = {
                        expanded = false
                        onSelected("")
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        }
                    )
                }
            }
        }
    }
}

// -------------------------
// 共通日付パース
// -------------------------
fun parseOperationDateOrNull(text: String): LocalDate? {
    return try {
        if (text.isBlank()) null else LocalDate.parse(text, operationDateFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}
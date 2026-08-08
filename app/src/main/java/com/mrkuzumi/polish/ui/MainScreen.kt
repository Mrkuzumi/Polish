package com.mrkuzumi.polish.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日历主页：
 * 上 3/4 —— 日历卡片（月切换 + 星期表头 + 日期网格）
 * 下 1/4 —— 按钮拓展区（已选日期摘要 + 功能按钮）
 */
@Composable
fun MainScreen() {
    val today = remember { LocalDate.now() }

    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.monthValue) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selected = LocalDate.parse(selectedIso)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // 上 3/4：日历卡片
            CalendarCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f),
                year = year,
                month = month,
                today = today,
                selected = selected,
                onMonthChange = { y, m ->
                    year = y
                    month = m
                },
                onDayClick = { date ->
                    selectedIso = date.toString()
                    if (date.year != year || date.monthValue != month) {
                        year = date.year
                        month = date.monthValue
                    }
                },
            )

            Spacer(Modifier.height(16.dp))

            // 下 1/4：按钮拓展区
            BottomActionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                selected = selected,
                onToday = {
                    selectedIso = today.toString()
                    year = today.year
                    month = today.monthValue
                },
                onPlaceholder = { label ->
                    scope.launch {
                        snackbarHostState.showSnackbar("「$label」功能开发中，敬请期待")
                    }
                },
            )
        }
    }
}

// ===================== 日历卡片 =====================

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
private fun CalendarCard(
    modifier: Modifier = Modifier,
    year: Int,
    month: Int,
    today: LocalDate,
    selected: LocalDate,
    onMonthChange: (year: Int, month: Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // 月份头部：‹ 2026年8月 ›
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "%d年%d月".format(year, month),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { shiftMonth(year, month, -1, onMonthChange) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { shiftMonth(year, month, 1, onMonthChange) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                WEEKDAYS.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 日期网格（周一开头，含前后月补位日期）
            val monthGrid = buildMonthGrid(YearMonth.of(year, month))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                monthGrid.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        week.forEach { cell ->
                            DayCell(
                                dayNumber = cell.dayNumber,
                                inMonth = cell.inMonth,
                                isSelected = cell.date == selected,
                                isToday = cell.date == today,
                                onClick = { onDayClick(cell.date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shiftMonth(
    year: Int,
    month: Int,
    delta: Int,
    onMonthChange: (year: Int, month: Int) -> Unit,
) {
    val shifted = YearMonth.of(year, month).plusMonths(delta.toLong())
    onMonthChange(shifted.year, shifted.monthValue)
}

private data class DayCellData(val date: LocalDate, val dayNumber: Int, val inMonth: Boolean)

/** 以周一为起点的 42 格（6 行 x 7 列）月历数据 */
private fun buildMonthGrid(month: YearMonth): List<DayCellData> {
    val firstOffset = (month.atDay(1).dayOfWeek.value + 6) % 7 // 周一 = 0
    val daysInMonth = month.lengthOfMonth()
    val prevMonth = month.minusMonths(1)
    val daysInPrev = prevMonth.lengthOfMonth()
    val totalCells = ((firstOffset + daysInMonth + 6) / 7) * 7

    return (0 until totalCells).map { index ->
        val inMonth = index >= firstOffset && index < firstOffset + daysInMonth
        when {
            inMonth -> {
                val day = index - firstOffset + 1
                DayCellData(month.atDay(day), day, true)
            }
            index < firstOffset -> {
                val day = daysInPrev - (firstOffset - index) + 1
                DayCellData(prevMonth.atDay(day), day, false)
            }
            else -> {
                val day = index - firstOffset - daysInMonth + 1
                DayCellData(month.plusMonths(1).atDay(day), day, false)
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    val textColor = when {
        isSelected -> colorScheme.onPrimaryContainer
        isToday -> colorScheme.primary
        !inMonth -> colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(
                if (isSelected) colorScheme.primaryContainer
                else if (isToday) colorScheme.surfaceVariant
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayNumber.toString(),
            fontSize = 14.sp,
            color = textColor,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ===================== 底部按钮拓展区 =====================

private val WEEKDAY_NAMES = listOf(
    "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
)

@Composable
private fun BottomActionCard(
    modifier: Modifier = Modifier,
    selected: LocalDate,
    onToday: () -> Unit,
    onPlaceholder: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 已选日期摘要
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "已选择 %d月%d日 · %s".format(
                        selected.monthValue,
                        selected.dayOfMonth,
                        WEEKDAY_NAMES[selected.dayOfWeek.value - 1],
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onPrimaryContainer,
                )
            }

            // 按钮拓展区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onToday,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("今天", style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = { onPlaceholder("记录") },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("记录", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { onPlaceholder("统计") },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("统计", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

package com.mrkuzumi.polish.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrkuzumi.polish.data.Record
import com.mrkuzumi.polish.data.RecordRepository
import java.time.LocalDate
import java.time.YearMonth

// ===================== 主页 =====================

@Composable
fun HomeScreen(
    dataVersion: Int,
    onDataChanged: () -> Unit,
    showSnackbar: (String) -> Unit,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.monthValue) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }

    // 每当 dataVersion 变化时重新加载
    val records = remember(dataVersion) { RecordRepository.loadAll(context) }

    val selected = LocalDate.parse(selectedIso)
    val selectedRecord = records[selectedIso] ?: Record(dateIso = selectedIso)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 日历卡片（上 3/4）
        CalendarCard(
            modifier = Modifier.fillMaxWidth().weight(3f),
            year = year,
            month = month,
            today = today,
            selected = selected,
            recordDates = records.keys,
            onMonthChange = { y, m -> year = y; month = m },
            onDayClick = { date ->
                selectedIso = date.toString()
                if (date.year != year || date.monthValue != month) {
                    year = date.year; month = date.monthValue
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        // 日期详情卡片（含 🦌 计数器 + 编辑按钮，占 1/4）
        DateDetailCard(
            modifier = Modifier.fillMaxWidth().weight(1f),
            selected = selected,
            record = selectedRecord,
            onIncrement = {
                val r = selectedRecord.copy(
                    count = selectedRecord.count + 1,
                    timestamps = selectedRecord.timestamps + System.currentTimeMillis(),
                )
                RecordRepository.save(context, r)
                onDataChanged()
                showSnackbar("🦌 ×${r.count}")
            },
            onDecrement = {
                if (selectedRecord.count > 0) {
                    val r = selectedRecord.copy(
                        count = selectedRecord.count - 1,
                        timestamps = if (selectedRecord.timestamps.isNotEmpty())
                            selectedRecord.timestamps.dropLast(1) else emptyList(),
                    )
                    RecordRepository.save(context, r)
                    onDataChanged()
                    showSnackbar("🦌 ×${r.count}")
                }
            },
            onEdit = { showEditSheet = true },
        )
    }

    // 编辑 BottomSheet
    if (showEditSheet) {
        EditRecordSheet(
            record = selectedRecord,
            onSave = { updated ->
                RecordRepository.save(context, updated)
                onDataChanged()
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false },
        )
    }
}

// ===================== 日历卡片 =====================

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")
private val WEEKDAY_NAMES = listOf(
    "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
)

@Composable
private fun CalendarCard(
    modifier: Modifier,
    year: Int,
    month: Int,
    today: LocalDate,
    selected: LocalDate,
    recordDates: Set<String>,
    onMonthChange: (year: Int, month: Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // 月份头部
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%d年%d月".format(year, month),
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { shiftMonth(year, month, -1, onMonthChange) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上个月", tint = cs.onSurfaceVariant)
                }
                IconButton(onClick = { shiftMonth(year, month, 1, onMonthChange) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下个月", tint = cs.onSurfaceVariant)
                }
            }
            // 星期表头
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                WEEKDAYS.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // 日期网格
            val grid = buildMonthGrid(YearMonth.of(year, month))
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                grid.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        week.forEach { cell ->
                            DayCell(
                                dayNumber = cell.dayNumber,
                                inMonth = cell.inMonth,
                                isSelected = cell.date == selected,
                                isToday = cell.date == today,
                                hasRecord = cell.inMonth && cell.date.toString() in recordDates,
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

private fun shiftMonth(year: Int, month: Int, delta: Int, cb: (Int, Int) -> Unit) {
    val ym = YearMonth.of(year, month).plusMonths(delta.toLong())
    cb(ym.year, ym.monthValue)
}

private data class DayCellData(val date: LocalDate, val dayNumber: Int, val inMonth: Boolean)

private fun buildMonthGrid(month: YearMonth): List<DayCellData> {
    val offset = (month.atDay(1).dayOfWeek.value + 6) % 7
    val days = month.lengthOfMonth()
    val prev = month.minusMonths(1)
    val prevDays = prev.lengthOfMonth()
    val total = ((offset + days + 6) / 7) * 7
    return (0 until total).map { i ->
        when {
            i >= offset && i < offset + days -> DayCellData(month.atDay(i - offset + 1), i - offset + 1, true)
            i < offset -> DayCellData(prev.atDay(prevDays - (offset - i) + 1), prevDays - (offset - i) + 1, false)
            else -> DayCellData(month.plusMonths(1).atDay(i - offset - days + 1), i - offset - days + 1, false)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dayNumber: Int,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> cs.onPrimaryContainer
        isToday -> cs.primary
        !inMonth -> cs.onSurfaceVariant.copy(alpha = 0.35f)
        else -> cs.onSurface
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> cs.primaryContainer
                    isToday -> cs.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                fontSize = 14.sp,
                color = textColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (hasRecord) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) cs.onPrimaryContainer else cs.primary),
                )
            }
        }
    }
}

// ===================== 日期详情卡片 =====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateDetailCard(
    modifier: Modifier,
    selected: LocalDate,
    record: Record,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 已选日期
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = cs.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "已选择 %d月%d日 · %s".format(
                        selected.monthValue, selected.dayOfMonth,
                        WEEKDAY_NAMES[selected.dayOfWeek.value - 1],
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onPrimaryContainer,
                )
            }

            // 🦌 计数器（大号、居中、可点击）
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(cs.surface.copy(alpha = 0.6f))
                        .combinedClickable(onClick = onIncrement, onLongClick = onDecrement)
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🦌 ×${record.count}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary,
                    )
                }
            }
            Text(
                text = "点击 +1   ·   长按 -1",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            // 编辑按钮
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("编辑细节", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

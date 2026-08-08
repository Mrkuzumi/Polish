package com.mrkuzumi.polish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrkuzumi.polish.data.Record
import com.mrkuzumi.polish.data.RecordRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")
private val WEEKDAY_NAMES = listOf(
    "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
)

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

    val records = remember(dataVersion) { RecordRepository.loadAll(context) }
    val selected = LocalDate.parse(selectedIso)
    val selectedRecord = records[selectedIso] ?: Record(dateIso = selectedIso)

    fun inc(date: LocalDate, count: Int, ts: List<Long>) {
        val r = Record(date.toString(), count + 1, ts + System.currentTimeMillis())
        RecordRepository.save(context, r)
        onDataChanged()
    }
    fun dec(date: LocalDate, count: Int, ts: List<Long>) {
        if (count <= 0) return
        val r = Record(date.toString(), count - 1, if (ts.isNotEmpty()) ts.dropLast(1) else ts)
        RecordRepository.save(context, r)
        onDataChanged()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 日历卡片（上 3/4）
        CalendarCard(
            modifier = Modifier.fillMaxWidth().weight(3f),
            year = year, month = month, today = today, selected = selected,
            records = records,
            onMonthChange = { y, m -> year = y; month = m },
            onDayTap = { date ->
                selectedIso = date.toString()
                if (date.year != year || date.monthValue != month) { year = date.year; month = date.monthValue }
                val r = records[date.toString()] ?: Record(date.toString())
                inc(date, r.count, r.timestamps)
            },
            onDayLongPress = { date, localCount ->
                selectedIso = date.toString()
                val r = records[date.toString()] ?: Record(date.toString())
                if (r.count > 0) {
                    val decBy = r.count - localCount
                    if (decBy > 0) {
                        val ts = r.timestamps.dropLast(decBy)
                        RecordRepository.save(context, Record(date.toString(), localCount, ts))
                        onDataChanged()
                    }
                }
            },
        )

        Spacer(Modifier.height(10.dp))

        // 底部 1/4：日期信息 + [-][编辑细节][+] 按钮
        BottomActionBar(
            modifier = Modifier.fillMaxWidth().weight(1f),
            selected = selected,
            count = selectedRecord.count,
            onMinus = { dec(selected, selectedRecord.count, selectedRecord.timestamps) },
            onPlus = { inc(selected, selectedRecord.count, selectedRecord.timestamps) },
            onEdit = { showEditSheet = true },
        )
    }

    // 编辑 BottomSheet
    if (showEditSheet) {
        EditRecordSheet(
            record = selectedRecord,
            onSave = { updated ->
                RecordRepository.save(context, updated); onDataChanged(); showEditSheet = false
            },
            onDismiss = { showEditSheet = false },
        )
    }
}

// ===================== 日历卡片 =====================

@Composable
private fun CalendarCard(
    modifier: Modifier,
    year: Int, month: Int,
    today: LocalDate, selected: LocalDate,
    records: Map<String, Record>,
    onMonthChange: (year: Int, month: Int) -> Unit,
    onDayTap: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate, localCount: Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // 月份头部
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("%d年%d月".format(year, month), style = MaterialTheme.typography.titleLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = { shiftMonth(year, month, -1, onMonthChange) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月", tint = cs.onSurfaceVariant)
                }
                IconButton(onClick = { shiftMonth(year, month, 1, onMonthChange) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月", tint = cs.onSurfaceVariant)
                }
            }
            // 星期表头
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                WEEKDAYS.forEach { d ->
                    Text(d, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            // 日期网格
            val grid = buildMonthGrid(YearMonth.of(year, month))
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                grid.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        week.forEach { cell ->
                            val iso = cell.date.toString()
                            val cnt = if (cell.inMonth) records[iso]?.count ?: 0 else 0
                            DayCell(
                                dayNumber = cell.dayNumber,
                                inMonth = cell.inMonth,
                                isSelected = cell.date == selected,
                                isToday = cell.date == today,
                                recordCount = cnt,
                                onTap = { if (cell.inMonth) onDayTap(cell.date) },
                                onLongPressEnd = { localCount -> if (cell.inMonth) onDayLongPress(cell.date, localCount) },
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
    val ym = YearMonth.of(year, month).plusMonths(delta.toLong()); cb(ym.year, ym.monthValue)
}

private data class DayCellData(val date: LocalDate, val dayNumber: Int, val inMonth: Boolean)

private fun buildMonthGrid(ym: YearMonth): List<DayCellData> {
    val offset = (ym.atDay(1).dayOfWeek.value + 6) % 7
    val days = ym.lengthOfMonth()
    val prev = ym.minusMonths(1)
    val prevDays = prev.lengthOfMonth()
    val total = ((offset + days + 6) / 7) * 7
    return (0 until total).map { i ->
        when {
            i >= offset && i < offset + days -> DayCellData(ym.atDay(i - offset + 1), i - offset + 1, true)
            i < offset -> DayCellData(prev.atDay(prevDays - (offset - i) + 1), prevDays - (offset - i) + 1, false)
            else -> DayCellData(ym.plusMonths(1).atDay(i - offset - days + 1), i - offset - days + 1, false)
        }
    }
}

// ===================== 日期单元格（含 🦌 计数器） =====================

@Composable
private fun DayCell(
    dayNumber: Int,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    recordCount: Int,
    onTap: () -> Unit,
    onLongPressEnd: (localCount: Int) -> Unit,
    modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var longPressing by remember { mutableStateOf(false) }
    var localCount by remember(recordCount) { mutableIntStateOf(recordCount) }

    // 长按持续减少
    LaunchedEffect(longPressing) {
        if (longPressing) {
            localCount = recordCount
            delay(400)
            while (longPressing && localCount > 0) {
                localCount--
                delay(150) // ～每秒减少约 6 次
            }
            onLongPressEnd(localCount)
            longPressing = false
        }
    }

    val displayCount = if (longPressing) localCount else recordCount

    val bgColor = when {
        isSelected -> cs.primaryContainer
        isToday -> cs.surfaceVariant
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> cs.onPrimaryContainer
        isToday -> cs.primary
        !inMonth -> cs.onSurfaceVariant.copy(alpha = 0.35f)
        else -> cs.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .pointerInput(dayNumber) {
                detectTapGestures(
                    onTap = { if (inMonth) onTap() },
                    onPress = {
                        val tapNotLong = tryAwaitRelease()  // true=轻点, false=长按
                        if (!tapNotLong && inMonth) {
                            longPressing = true
                            awaitRelease()                  // 等待手指松开
                            longPressing = false
                            onLongPressEnd(localCount)       // 松开时保存累积的减少次数
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                fontSize = 14.sp,
                color = textColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (inMonth && displayCount > 0) {
                Text(
                    text = "🦌×$displayCount",
                    fontSize = 8.5.sp,
                    color = cs.primary,
                    maxLines = 1,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ===================== 底部操作栏 =====================

@Composable
private fun BottomActionBar(
    modifier: Modifier,
    selected: LocalDate,
    count: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
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
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 已选日期 + 计数摘要
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
                    modifier = Modifier.weight(1f),
                )
                Text("🦌×$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            }

            // [-][编辑细节][+]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onMinus,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Icon(Icons.Default.Edit, null); Spacer(Modifier.size(6.dp)); Text("编辑细节", style = MaterialTheme.typography.labelLarge) }

                FilledTonalButton(
                    onClick = onPlus,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

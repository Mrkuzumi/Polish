package com.mrkuzumi.polish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrkuzumi.polish.MainActivity
import com.mrkuzumi.polish.data.Record
import com.mrkuzumi.polish.data.RecordRepository
import com.mrkuzumi.polish.util.Terminology
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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

    // 预约未来日期的弹窗
    var bookingDate by remember { mutableStateOf<LocalDate?>(null) }

    // ★ 性能核心：mutableStateOf 包装普通 Map，确保每次变更触发精准重组
    var records by remember { mutableStateOf(RecordRepository.loadAll(context).toMutableMap()) }

    // Debounce 持久化：变更后 500ms 无新操作才写磁盘
    var saveToken by remember { mutableLongStateOf(0L) }
    LaunchedEffect(saveToken) {
        if (saveToken > 0L) {
            delay(500)
            withContext(Dispatchers.IO) {
                RecordRepository.saveAll(context, records)
            }
            onDataChanged() // 通知 StatsScreen 刷新
        }
    }

    fun inc(date: LocalDate) {
        val key = date.toString()
        val r = records[key] ?: Record(key)
        records = records.toMutableMap().apply {
            this[key] = r.copy(count = r.count + 1, timestamps = r.timestamps + System.currentTimeMillis())
        }
        saveToken = System.currentTimeMillis()
    }

    fun decBatch(date: LocalDate, newCount: Int) {
        val key = date.toString()
        val r = records[key] ?: return
        if (newCount >= r.count) return
        val drop = r.count - newCount
        records = records.toMutableMap().apply {
            this[key] = r.copy(
                count = newCount,
                timestamps = if (r.timestamps.size >= drop) r.timestamps.dropLast(drop) else emptyList(),
            )
        }
        saveToken = System.currentTimeMillis()
    }

    val selected = LocalDate.parse(selectedIso)
    val selectedRecord = records[selectedIso] ?: Record(dateIso = selectedIso)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        CalendarCard(
            modifier = Modifier.fillMaxWidth().weight(3f),
            year = year, month = month, today = today, selected = selected,
            records = records,
            onMonthChange = { y, m -> year = y; month = m },
            onDayTap = { date ->
                selectedIso = date.toString()
                if (date.year != year || date.monthValue != month) { year = date.year; month = date.monthValue }
                if (date.isAfter(today)) {
                    bookingDate = date
                } else {
                    inc(date)
                }
            },
            onDayLongPressEnd = { date, newCount ->
                selectedIso = date.toString()
                decBatch(date, newCount)
            },
        )

        Spacer(Modifier.height(10.dp))

        BottomActionBar(
            modifier = Modifier.fillMaxWidth().weight(1f),
            selected = selected,
            record = selectedRecord,
            onMinus = { decBatch(selected, (selectedRecord.count - 1).coerceAtLeast(0)) },
            onPlus = { inc(selected) },
            onEdit = { showEditSheet = true },
        )
    }

    if (showEditSheet) {
        EditRecordSheet(
            record = selectedRecord,
            onSave = { updated ->
                records = records.toMutableMap().apply { this[updated.dateIso] = updated }
                saveToken = System.currentTimeMillis()
                onDataChanged()
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false },
        )
    }

    // 未来日期预约弹窗
    bookingDate?.let { date ->
        val dateText = "%d月%d日".format(date.monthValue, date.dayOfMonth)
        AlertDialog(
            onDismissRequest = { bookingDate = null },
            title = {
                Text(
                    Terminology.bookingTitle(context),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            text = {
                Text(
                    Terminology.bookingBody(context, dateText),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                Button(onClick = {
                    MainActivity.scheduleReminder(context, date.toString())
                    bookingDate = null
                    showSnackbar("已预约 $dateText 21:00 ${Terminology.verb(context)}提醒")
                }) { Text("OK👌") }
            },
            dismissButton = {
                TextButton(onClick = { bookingDate = null }) {
                    Text("才...才不要呢", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
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
    onDayLongPressEnd: (LocalDate, localCount: Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("%d年%d月".format(year, month), style = MaterialTheme.typography.titleLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = { val p = YearMonth.of(year, month).minusMonths(1); onMonthChange(p.year, p.monthValue) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月", tint = cs.onSurfaceVariant)
                }
                IconButton(onClick = { val n = YearMonth.of(year, month).plusMonths(1); onMonthChange(n.year, n.monthValue) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月", tint = cs.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                WEEKDAYS.forEach { d ->
                    Text(d, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
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
                                onTap = { onDayTap(cell.date) },
                                onLongPressEnd = { localCount -> onDayLongPressEnd(cell.date, localCount) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
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

// ===================== 日期单元格（含 🦌 计数器、零延迟手势） =====================

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
    val haptic = LocalHapticFeedback.current
    var longPressing by remember { mutableStateOf(false) }
    var localCount by remember(recordCount) { mutableIntStateOf(recordCount) }

    // 长按期间持续减少（带震动反馈）
    LaunchedEffect(longPressing) {
        if (longPressing) {
            localCount = recordCount
            delay(400) // 长按确认后稍等再开始递减
            while (longPressing && localCount > 0) {
                localCount--
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(150)
            }
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
            // ★ 关键：自定义手势，轻点立即响应，无 500ms 延迟
            .pointerInput(dayNumber) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // 并行等待：要么手指在长按超时前抬起（轻点），要么超时（长按）
                    var isTap = false
                    val longPressMs = viewConfiguration.longPressTimeoutMillis
                    val start = System.currentTimeMillis()
                    // 等指针全部抬起或超时
                    while (System.currentTimeMillis() - start < longPressMs) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.all { it.changedToUp() }) {
                            isTap = true
                            break
                        }
                    }
                    if (isTap) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (inMonth) onTap()
                    } else {
                        // 长按开始
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (inMonth) { longPressing = true }
                        // 等待手指抬起
                        var allUp = false
                        while (!allUp) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            allUp = event.changes.all { it.changedToUp() }
                        }
                        if (inMonth) {
                            longPressing = false
                            onLongPressEnd(localCount)
                        }
                    }
                }
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
                    text = Terminology.emojiWithCount(LocalContext.current, displayCount),
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
    record: com.mrkuzumi.polish.data.Record,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val count = record.count
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 日期 + 计数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = cs.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "%d月%d日 · %s".format(selected.monthValue, selected.dayOfMonth, WEEKDAY_NAMES[selected.dayOfWeek.value - 1]),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(Terminology.emojiWithCount(LocalContext.current, count), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            }

            // 已保存的细节信息
            if (record.dish.isNotBlank() || record.hand.isNotBlank()) {
                Column {
                    if (record.dish.isNotBlank()) {
                        Text("下饭菜：${record.dish}", style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer)
                    }
                    if (record.hand.isNotBlank()) {
                        val handLabel = when (record.hand) { "left" -> "左手"; "right" -> "右手"; else -> record.hand }
                        Text("惯用手：$handLabel", style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer)
                    }
                }
            }

            // 操作按钮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onMinus, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onEdit, modifier = Modifier.weight(2f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Edit, null); Spacer(Modifier.size(6.dp)); Text("编辑细节", style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(onClick = onPlus, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

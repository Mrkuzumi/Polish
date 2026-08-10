package com.mrkuzumi.polish.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrkuzumi.polish.data.RecordRepository
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(dataVersion: Int) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(RecordRepository.loadAll(context)) }
    LaunchedEffect(dataVersion) { records = RecordRepository.loadAll(context) }
    val today = remember { LocalDate.now() }

    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.monthValue) }

    val ym = YearMonth.of(year, month)
    val prefix = "%d-%02d".format(year, month)
    val monthRecords = records.filterKeys { it.startsWith(prefix) }
    val total = monthRecords.values.sumOf { it.count }
    val allTs = monthRecords.values.flatMap { it.timestamps }
    val avgTime = averageTimeOfDay(allTs)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 月份切换
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    val prev = ym.minusMonths(1); year = prev.year; month = prev.monthValue
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "%d年%d月".format(year, month),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = {
                    val next = ym.plusMonths(1); year = next.year; month = next.monthValue
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 统计摘要卡片
        val cs = MaterialTheme.colorScheme
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "本月次数",
                value = total.toString(),
                unit = "次",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "平均时段",
                value = avgTime,
                unit = "",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // 每日柱状图
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("每日分布", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                DailyBarChart(
                    yearMonth = ym,
                    records = monthRecords,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 时段分布（带 0:00～24:00 刻度）
        Card(
            Modifier.fillMaxWidth().weight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("时段分布", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.height(4.dp))
                HourlyBarChart(
                    timestamps = allTs,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer),
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun DailyBarChart(
    yearMonth: YearMonth,
    records: Map<String, com.mrkuzumi.polish.data.Record>,
    modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val days = yearMonth.lengthOfMonth()
    val maxCount = records.values.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = (w / days) * 0.55f
        val gap = w / days
        val baseY = h * 0.95f

        for (d in 1..days) {
            val key = "${yearMonth}-%02d".format(d)
            val cnt = records[key]?.count ?: 0
            val barH = if (cnt > 0) ((cnt.toFloat() / maxCount) * h * 0.9f).coerceAtLeast(4f) else 0f
            val x = gap * (d - 1) + (gap - barW) / 2f

            drawRect(
                color = if (cnt > 0) cs.primary else cs.outlineVariant.copy(alpha = 0.4f),
                topLeft = Offset(x, baseY - barH),
                size = Size(barW, barH.coerceAtLeast(1f)),
            )
        }
    }
}

// ===================== 时段分布柱状图（0:00 ~ 24:00） =====================

@Composable
private fun HourlyBarChart(timestamps: List<Long>, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val hourCounts = IntArray(24)
    timestamps.forEach { ts ->
        val inst = java.time.Instant.ofEpochMilli(ts)
        val ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.systemDefault())
        hourCounts[ldt.hour]++
    }
    val maxCount = hourCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val w = size.width
            val h = size.height
            val barW = (w / 24) * 0.55f
            val gap = w / 24f
            val baseY = h * 0.95f

            for (hour in 0..23) {
                val cnt = hourCounts[hour]
                val barH = if (cnt > 0) ((cnt.toFloat() / maxCount) * h * 0.9f).coerceAtLeast(3f) else 0f
                val x = gap * hour + (gap - barW) / 2f
                drawRect(
                    color = if (cnt > 0) cs.primary else cs.outlineVariant.copy(alpha = 0.3f),
                    topLeft = Offset(x, baseY - barH),
                    size = Size(barW, barH.coerceAtLeast(1f)),
                )
            }
        }
        // 时间刻度标签
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            listOf("0:00", "6:00", "12:00", "18:00", "24:00").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 从时间戳列表计算平均时段（圆形均值，正确处理跨午夜），返回 "HH:MM" 字符串 */
private fun averageTimeOfDay(timestamps: List<Long>): String {
    if (timestamps.isEmpty()) return "--:--"
    // 圆形均值：把每个时间映射为单位圆上的角度（0~2π），求平均向量后反算时刻
    val angles = timestamps.map { ts ->
        val ldt = java.time.Instant.ofEpochMilli(ts)
            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        ldt.toSecondOfDay().toDouble() / 86400.0 * (2 * Math.PI)
    }
    val sinSum = angles.sumOf { kotlin.math.sin(it) }
    val cosSum = angles.sumOf { kotlin.math.cos(it) }
    val avgAngle = kotlin.math.atan2(sinSum, cosSum) // (-π, π]
    val secOfDay = ((avgAngle / (2 * Math.PI)) * 86400 + 86400).toLong() % 86400
    val h = ((secOfDay / 3600) % 24).toInt()
    val m = ((secOfDay % 3600) / 60).toInt()
    return "%02d:%02d".format(h, m)
}

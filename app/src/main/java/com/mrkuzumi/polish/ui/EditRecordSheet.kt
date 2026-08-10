package com.mrkuzumi.polish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrkuzumi.polish.data.Record
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * 自下而上抽屉式弹出页面，编辑当日磨剑细节。
 * - 下饭菜（文本输入）
 * - 左 / 右手（二选一 FilterChip）
 * - 具体时间（仿 iOS 竖向滚轮：时 : 分，当天每次记录各一行，可逐条调整）
 *
 * 时间默认取每次点击时的系统时间；只有某条被手动调整过后，保存时才重建当天
 * 时间戳（供"统计"页平均时段 / 时段分布使用），未调整的条目原样保留。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordSheet(
    record: Record,
    onSave: (Record) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dish by remember(record.dateIso) { mutableStateOf(record.dish) }
    var hand by remember(record.dateIso) { mutableStateOf(record.hand) }

    // 逐条时间：默认取每次点击时的系统时间，缺失条目用当前系统时间补足
    val initTimes = remember(record.dateIso) { initialTimesOf(record) }
    val times = remember(record.dateIso) { mutableStateListOf(*initTimes.toTypedArray()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "编辑 ${record.dateIso}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(20.dp))

            // 下饭菜
            Text(
                text = "下饭菜",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dish,
                onValueChange = { dish = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：红烧肉、麻婆豆腐…") },
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            // 左 / 右手
            Text(
                text = "惯用手",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = hand == "left",
                    onClick = { hand = if (hand == "left") "" else "left" },
                    label = { Text("左手") },
                )
                FilterChip(
                    selected = hand == "right",
                    onClick = { hand = if (hand == "right") "" else "right" },
                    label = { Text("右手") },
                )
            }

            Spacer(Modifier.height(20.dp))

            // 具体时间：逐条编辑每次记录的时间（仿 iOS 滚轮），不调整的条目保留点击时的系统时间
            if (times.isNotEmpty()) {
                Text(
                    text = "具体时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                times.forEachIndexed { i, (h, m) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "第 ${i + 1} 次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(56.dp),
                        )
                        WheelPicker(
                            range = 0..23,
                            selected = h,
                            onSelected = { times[i] = it to m },
                            modifier = Modifier.weight(1f),
                            itemHeight = 32.dp,
                        )
                        Text(
                            text = "：",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        WheelPicker(
                            range = 0..59,
                            selected = m,
                            onSelected = { times[i] = h to it },
                            modifier = Modifier.weight(1f),
                            itemHeight = 32.dp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    var updated = record.copy(dish = dish.trim(), hand = hand)
                    // 仅当用户手动调整过任何一条时间（或旧数据缺少时间戳）时才重建时间戳，
                    // 否则逐条保留点击时的系统时间，统计口径不被破坏
                    if (times != initTimes || updated.timestamps.isEmpty()) {
                        if (updated.count > 0) {
                            updated = updated.copy(
                                timestamps = times.map { (h, m) ->
                                    LocalDate.parse(record.dateIso)
                                        .atTime(h, m)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                },
                            )
                        } else {
                            updated = updated.copy(timestamps = emptyList())
                        }
                    }
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("保存", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 取每次点击时记录的系统时间（共 count 条），缺失时间戳的旧数据条目用当前系统时间补足 */
private fun initialTimesOf(record: Record): List<Pair<Int, Int>> {
    val now = LocalTime.now()
    return (0 until record.count).map { i ->
        val ts = record.timestamps.getOrNull(i)
        if (ts != null) {
            val ldt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
            ldt.hour to ldt.minute
        } else {
            now.hour to now.minute
        }
    }
}

/**
 * 仿 iOS 闹钟的竖向滚轮数字选择器：
 * 上下滑动数字，松手自动吸附居中；中间行高亮放大，两侧缩小变淡；点击任意行可直达。
 */
@Composable
private fun WheelPicker(
    range: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
) {
    val values = remember(range) { range.toList() }
    val cs = MaterialTheme.colorScheme
    val visibleCount = 5
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val topPaddingPx = itemHeightPx * ((visibleCount - 1) / 2f) // 上下各留 2 行
    val viewportH = itemHeightPx * visibleCount

    val listState = rememberLazyListState()
    val initialIndex = remember { (selected - range.first).coerceIn(0, values.lastIndex) }
    val scope = rememberCoroutineScope()

    // 初始定位：选中项垂直居中（contentPadding 上下各留 2 行）
    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex, 0)
    }

    // 滚动过程中实时跟踪中心行（决定高亮与选中值）
    var centerIndex by remember { mutableIntStateOf(initialIndex) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = info.viewportSize.height / 2f
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index
        }.collect { idx -> if (idx != null) { centerIndex = idx; onSelected(range.first + idx) } }
    }

    // 滚动停止后吸附到最近的一行
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val info = listState.layoutInfo
                val center = info.viewportSize.height / 2f
                val nearest = info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }
                if (nearest != null) {
                    val target = nearest.index * itemHeightPx
                    val current = listState.firstVisibleItemScrollOffset + listState.firstVisibleItemIndex * itemHeightPx
                    if (abs(current - target) > 1f) {
                        listState.animateScrollToItem(nearest.index, 0)
                    }
                }
            }
    }

    // 滚动位置状态（供各行计算与中心距离，实现缩放/淡化）
    var scrollPos by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset + listState.firstVisibleItemIndex * itemHeightPx }
            .collect { scrollPos = it }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(itemHeight * visibleCount),
        contentPadding = PaddingValues(vertical = itemHeight * ((visibleCount - 1) / 2)),
    ) {
        itemsIndexed(values) { index, value ->
            val isCenter = index == centerIndex
            val top = topPaddingPx + index * itemHeightPx - scrollPos
            val distance = abs(top + itemHeightPx / 2f - viewportH / 2f) / itemHeightPx
            val scaleFactor = (1f - 0.16f * distance).coerceIn(0.7f, 1f)
            val alphaFactor = (1f - 0.3f * distance).coerceIn(0.3f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCenter) cs.primaryContainer.copy(alpha = 0.45f) else Color.Transparent)
                    .clickable { scope.launch { listState.animateScrollToItem(index, 0) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%02d".format(value),
                    fontSize = 18.sp,
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCenter) cs.primary else cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { scaleX = scaleFactor; scaleY = scaleFactor; alpha = alphaFactor },
                )
            }
        }
    }
}

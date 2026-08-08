package com.mrkuzumi.polish.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrkuzumi.polish.data.Record

/**
 * 自下而上抽屉式弹出页面，编辑当日磨剑细节。
 * - 下饭菜（文本输入）
 * - 左 / 右手（二选一 FilterChip）
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    onSave(record.copy(dish = dish.trim(), hand = hand))
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

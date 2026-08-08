package com.mrkuzumi.polish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首次启动的性别选择页：
 * 居中展示 App 名称，下方一左一右两个大圆角卡片按钮（男 / 女）
 */
@Composable
fun GenderSelectScreen(onSelect: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(containerColor = colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 品牌圆标
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "磨",
                    color = colorScheme.primary,
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Polish",
                style = MaterialTheme.typography.headlineLarge,
                color = colorScheme.primary,
            )
            Text(
                text = "磨剑",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = "初次使用，请选择你的性别",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // 一左一右两个性别按钮
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                GenderCard(
                    symbol = "♂",
                    label = "男",
                    onClick = { onSelect("male") },
                    modifier = Modifier.weight(1f),
                )
                GenderCard(
                    symbol = "♀",
                    label = "女",
                    onClick = { onSelect("female") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GenderCard(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        modifier = modifier.height(220.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = symbol,
                fontSize = 56.sp,
                color = colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onPrimaryContainer,
            )
        }
    }
}

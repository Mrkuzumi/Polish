package com.mrkuzumi.polish.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = OnPrimary,
    primaryContainer = PinkPrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = PinkSecondary,
    onSecondary = OnSecondary,
    secondaryContainer = PinkSecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = PinkTertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = PinkTertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = PinkBackground,
    onBackground = OnBackground,
    surface = PinkSurface,
    onSurface = OnSurface,
    surfaceVariant = PinkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = PinkOutline,
    outlineVariant = PinkOutlineVariant,
)

// 卡片化 + 大圆角（Material You 最显著的特征）
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PolishTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

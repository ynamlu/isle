package com.isle.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 统一色板：品牌紫为主，海雾蓝与暖粉为辅。
 * 关闭了动态取色，确保全应用配色一致。
 */
private val LightColors = lightColorScheme(
    primary = IslePurple,
    onPrimary = ColorWhite,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF2A1E6B),

    secondary = Color(0xFF5AA9E6),
    onSecondary = ColorWhite,
    secondaryContainer = Color(0xFFE4F1FF),
    onSecondaryContainer = Color(0xFF123A5C),

    tertiary = Color(0xFFF08BA8),
    onTertiary = ColorWhite,
    tertiaryContainer = Color(0xFFFFEDF3),
    onTertiaryContainer = Color(0xFF5C1F33),

    background = Color(0xFFF6F4FF),
    onBackground = Color(0xFF1C1A24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1A24),
    surfaceVariant = Color(0xFFEFEBFA),
    onSurfaceVariant = Color(0xFF5D5876),
    outline = Color(0xFFCFC7E8),
    outlineVariant = Color(0xFFE3DDF2)
)

private val DarkColors = darkColorScheme(
    primary = IslePurpleLight,
    onPrimary = Color(0xFF251A5E),
    primaryContainer = Color(0xFF3B2F86),
    onPrimaryContainer = Color(0xFFE6E0FF),

    secondary = Color(0xFF8FCBF0),
    onSecondary = Color(0xFF0E2A40),
    secondaryContainer = Color(0xFF23445E),
    onSecondaryContainer = Color(0xFFDCEEFF),

    tertiary = Color(0xFFF3A8C0),
    onTertiary = Color(0xFF4A1526),
    tertiaryContainer = Color(0xFF6B2A42),
    onTertiaryContainer = Color(0xFFFFDCE8),

    background = Color(0xFF121016),
    onBackground = Color(0xFFEDEAF4),
    surface = Color(0xFF1B1824),
    onSurface = Color(0xFFEDEAF4),
    surfaceVariant = Color(0xFF272233),
    onSurfaceVariant = Color(0xFFBDB6D0),
    outline = Color(0xFF514A66),
    outlineVariant = Color(0xFF332D42)
)

@Composable
fun IsleTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = IsleShapes,
        content = content
    )
}

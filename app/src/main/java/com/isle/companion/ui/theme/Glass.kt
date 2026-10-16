package com.isle.companion.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 玻璃质感配色：半透明填充 + 亮边 + 柔和投影。
 */
data class GlassColors(
    val fill: Brush,
    val border: Color,
    val shadow: Color,
    val tint: Color
)

@Composable
fun rememberGlassColors(): GlassColors {
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (dark) {
            GlassColors(
                fill = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
                ),
                border = Color.White.copy(alpha = 0.14f),
                shadow = Color.Black.copy(alpha = 0.45f),
                tint = IslePurpleLight.copy(alpha = 0.16f)
            )
        } else {
            GlassColors(
                fill = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.18f))
                ),
                border = Color.White.copy(alpha = 0.75f),
                shadow = IslePurple.copy(alpha = 0.12f),
                tint = IslePurple.copy(alpha = 0.06f)
            )
        }
    }
}

/**
 * 液态玻璃卡片：圆角 + 半透明渐变 + 高光描边 + 柔和投影。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = IsleRadius.card,
    colors: GlassColors? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = colors ?: rememberGlassColors()
    val base = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Column(
        modifier = base
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false,
                ambientColor = glass.shadow,
                spotColor = glass.shadow
            )
            .clip(shape)
            .background(brush = glass.fill)
            .border(width = 1.dp, color = glass.border, shape = shape)
    ) {
        content()
    }
}

/**
 * 页面底色：为玻璃元素提供可折射的渐变背景。
 */
@Composable
fun Modifier.isleBackground(): Modifier {
    val dark = isSystemInDarkTheme()
    return this.background(if (dark) IsleGlassBackgroundDark else IsleGlassBackground)
}

/**
 * 顶栏的玻璃底色。
 */
@Composable
fun glassBarColor(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.55f)
}

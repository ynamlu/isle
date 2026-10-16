package com.isle.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * 液态玻璃：真实背景模糊（backdrop blur）+ 磨砂颗粒。
 *
 * 用法：
 *   1. 内容容器加 `Modifier.liquidGlassSource(state)`（作为模糊来源）
 *   2. 悬浮在内容之上的栏（顶栏 / 输入栏）加 `Modifier.liquidGlass(state)`
 */
@Composable
fun rememberLiquidGlassState(): HazeState = rememberHazeState()

fun Modifier.liquidGlassSource(state: HazeState): Modifier = hazeSource(state = state)

/**
 * 高模糊半径 + 低不透明度 + 噪点 = 通透的磨砂玻璃。
 */
@Composable
fun glassStyle(): HazeBlurStyle {
    val dark = isSystemInDarkTheme()
    return HazeBlurStyle {
        blurRadius(22.dp)
        noiseFactor(0.06f)
        backgroundColor(
            if (dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.14f)
        )
    }
}

@Composable
fun Modifier.liquidGlass(state: HazeState): Modifier =
    hazeBlur(
        input = HazeInput.Backdrop(state),
        style = glassStyle()
    )

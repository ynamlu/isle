package com.isle.companion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 品牌主色：岛屿暮紫
val IslePurple = Color(0xFF6C5CE7)
val IslePurpleDark = Color(0xFF4A3FB5)
val IslePurpleLight = Color(0xFFB4A7F5)

// 辅助色：海雾蓝 / 暖沙金
val IsleBlue = Color(0xFF4FC3F7)
val IsleGold = Color(0xFFFFB74D)

// 中性色
val IsleBackgroundLight = Color(0xFFFDFBFF)
val IsleBackgroundDark = Color(0xFF121016)
val IsleSurfaceLight = Color(0xFFFFFFFF)
val IsleSurfaceDark = Color(0xFF1E1B24)

// 关系类型配色
val RelationLove = Color(0xFFFF6B8A)
val RelationBestie = Color(0xFFB07CFF)
val RelationFriend = Color(0xFF4FC3F7)
val RelationFamily = Color(0xFFFFB74D)
val RelationCustom = Color(0xFF78909C)

/** 品牌渐变：暮紫 → 海雾蓝 */
val IsleBrandBrush: Brush
    get() = Brush.linearGradient(listOf(IslePurple, IsleBlue))

/** 用户气泡渐变 */
val IsleUserBubbleBrush: Brush
    get() = Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF5B8DEF)))

/** 聊天背景：极淡的暖紫渐变 */
val IsleChatBackgroundBrush: Brush
    get() = Brush.verticalGradient(
        listOf(Color(0xFFF8F6FF), Color(0xFFF2F6FF), Color(0xFFFDFBFF))
    )

val IsleChatBackgroundDark: Brush
    get() = Brush.verticalGradient(
        listOf(Color(0xFF141220), Color(0xFF121016), Color(0xFF16131C))
    )

/** 液态玻璃风格背景：柔和的三色过渡，营造可被玻璃折射的底色 */
val IsleGlassBackground: Brush
    get() = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF4F1FE),
            Color(0xFFEEF4FF),
            Color(0xFFFDF2F7)
        )
    )

val IsleGlassBackgroundDark: Brush
    get() = Brush.linearGradient(
        colors = listOf(
            Color(0xFF17141F),
            Color(0xFF131521),
            Color(0xFF1A1520)
        )
    )

fun relationColor(type: String): Color = when (type) {
    "love" -> RelationLove
    "bestie" -> RelationBestie
    "friend" -> RelationFriend
    "family" -> RelationFamily
    else -> RelationCustom
}

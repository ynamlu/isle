package com.isle.companion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * 角色头像：有自定义图片则显示图片，否则显示名称首字。
 * 可传入 ringColor 给头像加一圈关系色描边。
 */
@Composable
fun CharacterAvatar(
    avatarPath: String?,
    name: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    ringColor: Color? = null
) {
    val file = remember(avatarPath) {
        avatarPath?.let { path -> File(path).takeIf { it.exists() } }
    }

    val ringModifier = if (ringColor != null) {
        Modifier
            .border(width = 2.dp, color = ringColor, shape = CircleShape)
            .padding(2.dp)
    } else {
        Modifier
    }

    if (file != null) {
        AsyncImage(
            model = file,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .then(ringModifier)
                .clip(CircleShape)
        )
    } else {
        val fallbackBrush = remember(ringColor) {
            val base = ringColor ?: Color(0xFF6C5CE7)
            Brush.linearGradient(listOf(base.copy(alpha = 0.9f), base.copy(alpha = 0.6f)))
        }
        Box(
            modifier = modifier
                .size(size)
                .then(ringModifier)
                .clip(CircleShape)
                .background(fallbackBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).ifBlank { "屿" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

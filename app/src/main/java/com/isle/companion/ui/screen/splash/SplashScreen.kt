package com.isle.companion.ui.screen.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isle.companion.R
import com.isle.companion.ui.theme.IsleBrandBrush

/**
 * 冷启动封面：品牌展示约 1.4 秒，然后进入登录/锁屏/主界面。
 */
@Composable
fun SplashScreen() {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(650),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = tween(700),
        label = "splashScale"
    )

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IsleBrandBrush)
    ) {
        // 柔光层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .scale(scale),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .background(Color.White.copy(alpha = 0.14f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp)
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "心屿",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "一座只属于你的心灵小岛",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.86f)
            )
            Spacer(Modifier.height(36.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White.copy(alpha = 0.82f),
                strokeWidth = 2.dp
            )
        }

        Text(
            text = "情绪支持 · 本地记忆 · 安全陪伴",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.62f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp)
        )
    }
}

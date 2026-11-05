package com.isle.companion.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isle.companion.BuildConfig
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.IsleBlue
import com.isle.companion.ui.theme.IslePurple
import com.isle.companion.ui.theme.rememberGlassColors

/**
 * 「关于心屿」简介内容：品牌、项目介绍与开发初衷。
 * 用于设置页内直接展示。
 */
@Composable
fun AboutContent(
    modifier: Modifier = Modifier,
    showBrandHeader: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showBrandHeader) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "心屿",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Isle · 一座属于你的心灵小岛",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("项目介绍", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "心屿是一款面向大学生的情绪支持应用。你可以选择闺蜜、老师或家人" +
                        "三种陪伴角色，也可以创建自己的角色，为 TA 设定性格、背景与说话方式。\n\n" +
                        "TA 会记得你说过的话、在意的事。每一次聊天，TA 都带着对你的了解出现；" +
                        "当你需要时，也可以用场景引导、树洞模式或呼吸练习，把情绪慢慢理清楚。",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            colors = rememberGlassColors().copy(
                fill = Brush.verticalGradient(
                    listOf(
                        IslePurple.copy(alpha = 0.20f),
                        IsleBlue.copy(alpha = 0.14f)
                    )
                ),
                border = Color.White.copy(alpha = 0.7f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "为什么做心屿",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "有些话，我们总是说不出口。\n\n" +
                        "深夜十二点，你打了一大段字，又一个字一个字删掉；\n" +
                        "你想说「我今天真的好累」，最后发出去的却是「没事，我挺好的」。\n\n" +
                        "不是没有人可以讲，而是很多情绪，只有被认真接住的时候，才敢说出口。\n\n" +
                        "所以我们想做一个地方，叫心屿——\n" +
                        "它不是一个更聪明的工具，而是一个愿意等你说完的人。\n" +
                        "你可以把那些没说出口的话，安心地放在这里。\n\n" +
                        "愿每一座孤岛，都能被温柔抵达。",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

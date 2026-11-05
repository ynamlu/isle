package com.isle.companion.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isle.companion.ui.component.AboutContent
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState

/**
 * 「关于心屿」：项目介绍、技术亮点、开发者信息与隐私/安全说明。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val hazeState = rememberLiquidGlassState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("关于心屿") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .isleBackground()
                .liquidGlassSource(hazeState)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AboutContent()

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("技术亮点", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    TechBullet("端侧长期记忆", "语义向量检索在手机本地完成，记忆默认不上传")
                    TechBullet("三角色与场景", "闺蜜 / 老师 / 家人，配 8 个情绪支持场景")
                    TechBullet("多智能体安全路由", "高危/危机暂停角色扮演，固定转介真人资源")
                    TechBullet("本地匿名账户", "不需要手机号、邮箱和实名信息")
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "开发者信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoLine("就读院校", "沧州交通学院")
                    InfoLine("开发者", "吴思彤")
                    InfoLine("联系方式", "ynawst@foxmail.com")
                    InfoLine("指导老师", "徐林雪")
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("隐私与安全", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "对话记录与密钥仅保存在本机；发送消息时，内容会传输至你配置的模型服务用于生成回复。\n\n" +
                            "心屿不是心理咨询或医疗诊断工具。如果你正经历强烈痛苦或危机，" +
                            "请联系 12356 心理援助热线、学校心理中心，或拨打 120/110。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "心屿不提供恋人角色，也不进行恋爱、暧昧或亲密关系的角色扮演。" +
                            "若你未满 18 周岁，请在监护人知情同意的前提下使用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Text(
                text = "愿每一座孤岛，都能被温柔抵达。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(
        text = "$label：$value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TechBullet(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            text = "· $title",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )
        Text(
            text = "　$description",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

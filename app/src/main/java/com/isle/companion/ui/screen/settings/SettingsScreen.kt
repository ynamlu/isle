package com.isle.companion.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.di.appContainer
import com.isle.companion.ui.component.AboutContent
import com.isle.companion.ui.theme.IsleBlue
import com.isle.companion.ui.theme.IsleGold
import com.isle.companion.ui.theme.IslePurple
import com.isle.companion.ui.theme.RelationBestie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUserProfile: () -> Unit,
    onOpenModelConfig: () -> Unit,
    onOpenStyleConfig: () -> Unit,
    onOpenTechInfo: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } }
    )
    val profileViewModel: UserProfileViewModel = viewModel(
        factory = viewModelFactory { initializer { UserProfileViewModel(container) } }
    )
    val settings by viewModel.settings.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    var showAgeDialog by remember { mutableStateOf(false) }
    var showDemoResetDialog by remember { mutableStateOf(false) }

    val hazeState = rememberLiquidGlassState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("设置") },
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
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== 关于心屿（直接展示） =====
            AboutContent()

            Spacer(Modifier.height(4.dp))
            HorizontalDivider()

            // ===== 二级页面入口 =====
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    tint = IslePurple,
                    title = "我的资料",
                    subtitle = buildString {
                        if (profile.isMinor) append("青少年模式 · ")
                        append(
                            if (profile.isFilled) {
                                "已填写" + (if (profile.name.isNotBlank()) " · " + profile.name else "")
                            } else {
                                "填写后所有角色都能更快了解你"
                            }
                        )
                    },
                    onClick = onOpenUserProfile
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Filled.Shield,
                    tint = IsleGold,
                    title = "年龄分组",
                    subtitle = if (profile.isMinor) "青少年模式" else "已满 18 周岁",
                    onClick = { showAgeDialog = true }
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Filled.Restore,
                    tint = IsleBlue,
                    title = "重置演示数据",
                    subtitle = "恢复 3 个角色、6 条记忆与示例对话",
                    onClick = { showDemoResetDialog = true }
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Filled.Memory,
                    tint = IsleBlue,
                    title = "模型服务",
                    subtitle = when {
                        settings.usingDemoKey -> "已内置演示密钥 · ${settings.model}"
                        settings.isConfigured -> "已配置 · ${settings.model}"
                        else -> "未配置，点击填写 API Key"
                    },
                    onClick = onOpenModelConfig
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Filled.Tune,
                    tint = IsleGold,
                    title = "回复风格",
                    subtitle = "温度 %.1f".format(settings.temperature),
                    onClick = onOpenStyleConfig
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Filled.Info,
                    tint = RelationBestie,
                    title = "技术亮点与开发者",
                    subtitle = "端侧记忆、隐私保护与开发者信息",
                    onClick = onOpenTechInfo
                )
            }

            Text(
                text = "对话记录与密钥仅保存在本机；发送消息时，内容会传输至你配置的模型服务用于生成回复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAgeDialog) {
        AlertDialog(
            onDismissRequest = { showAgeDialog = false },
            title = { Text("重新选择年龄分组") },
            text = { Text("确认后会回到年龄确认页；已保存的角色、聊天和记忆不会删除。") },
            confirmButton = {
                TextButton(onClick = {
                    showAgeDialog = false
                    viewModel.resetAgeGroup()
                }) { Text("重新选择") }
            },
            dismissButton = {
                TextButton(onClick = { showAgeDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDemoResetDialog) {
        AlertDialog(
            onDismissRequest = { showDemoResetDialog = false },
            title = { Text("重置演示数据") },
            text = { Text("将清空当前的角色、聊天、记忆和图片，并恢复演示数据。此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDemoResetDialog = false
                    viewModel.resetDemoData()
                }) { Text("重置") }
            },
            dismissButton = {
                TextButton(onClick = { showDemoResetDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

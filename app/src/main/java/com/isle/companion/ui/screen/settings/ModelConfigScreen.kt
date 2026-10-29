package com.isle.companion.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.di.appContainer

/**
 * 二级页面：模型服务配置（API Key / 地址 / 模型）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } }
    )
    val settings by viewModel.settings.collectAsState()
    val testing by viewModel.testing.collectAsState()
    val testMessage by viewModel.testMessage.collectAsState()
    var apiKeyVisible by remember { mutableStateOf(false) }
    var maxTokensText by remember(settings.maxTokens) {
        mutableStateOf(settings.maxTokens.toString())
    }

    val hazeState = rememberLiquidGlassState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("模型服务") },
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
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("接口配置", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "填写后即可开始对话；配置仅保存在本机",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = settings.apiKey,
                        onValueChange = viewModel::updateApiKey,
                        label = { Text("API Key") },
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = "切换可见性"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = settings.baseUrl,
                        onValueChange = viewModel::updateBaseUrl,
                        label = { Text("API 地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = settings.model,
                        onValueChange = viewModel::updateModel,
                        label = { Text("模型 ID") },
                        singleLine = true,
                        placeholder = { Text("deepseek-flash") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = maxTokensText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            maxTokensText = digits
                            digits.toIntOrNull()?.let { value ->
                                if (value in 128..8192) viewModel.updateMaxTokens(value)
                            }
                        },
                        label = { Text("最大回复 tokens") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "默认使用 DeepSeek-V4.1-Flash，原生支持图片理解。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = viewModel::testConnection,
                            enabled = !testing
                        ) {
                            Text(if (testing) "测试中…" else "测试连接")
                        }
                    }

                    testMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.startsWith("连接正常")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    if (settings.usingDemoKey) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "当前使用应用内置的演示密钥，可直接开始对话；填写自己的 Key 后将优先使用你的配置。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = "提示：API Key 属于敏感信息，请勿分享给他人。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

package com.isle.companion.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.data.auth.AuthResult
import com.isle.companion.di.appContainer
import com.isle.companion.ui.component.keyboardRetryOnFocus
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 合并后的「我的资料与账户」：
 * 上半部分管理本地账户与应用锁，下半部分维护全局共享的用户资料。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAccountScreen(
    onBack: () -> Unit,
    onLock: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val account by container.localAuthRepository.account.collectAsState()
    val profileViewModel: UserProfileViewModel = viewModel(
        factory = viewModelFactory { initializer { UserProfileViewModel(container) } }
    )
    val profile by profileViewModel.profile.collectAsState()
    val scope = rememberCoroutineScope()
    val hazeState = rememberLiquidGlassState()

    var showDelete by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var recoveryPassword by remember { mutableStateOf("") }
    var newRecoveryCode by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var recoveryMessage by remember { mutableStateOf<String?>(null) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除本地账户") },
            text = { Text("会清除本机的账户、角色、聊天、记忆和图片，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        container.clearAllData()
                        container.localAuthRepository.deleteAccount()
                        onAccountDeleted()
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            }
        )
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = {
                if (newRecoveryCode == null) {
                    showRecoveryDialog = false
                    recoveryPassword = ""
                }
            },
            title = { Text(if (newRecoveryCode == null) "重新生成恢复码" else "新的恢复码") },
            text = {
                if (newRecoveryCode == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("恢复码只在生成时显示一次，应用无法查看旧码。输入当前密码后会生成新码，旧码立即失效。")
                        OutlinedTextField(
                            value = recoveryPassword,
                            onValueChange = { recoveryPassword = it },
                            label = { Text("当前密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = IsleRadius.field,
                            modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                        )
                        recoveryMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("请现在保存，关闭后将无法再次查看。")
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = newRecoveryCode.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        TextButton(
                            onClick = {
                                val clipboard = container.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                                clipboard.setPrimaryClip(
                                    android.content.ClipData.newPlainText("心屿恢复码", newRecoveryCode)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("复制恢复码") }
                    }
                }
            },
            confirmButton = {
                if (newRecoveryCode == null) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                when (val result = container.localAuthRepository.regenerateRecoveryCode(recoveryPassword)) {
                                    is AuthResult.Success -> {
                                        newRecoveryCode = result.recoveryCode
                                        recoveryMessage = null
                                    }
                                    is AuthResult.Failure -> recoveryMessage = result.message
                                    is AuthResult.Locked -> recoveryMessage = "请稍后再试"
                                }
                            }
                        },
                        enabled = recoveryPassword.isNotBlank()
                    ) { Text("生成新恢复码") }
                } else {
                    TextButton(onClick = {
                        showRecoveryDialog = false
                        newRecoveryCode = null
                        recoveryPassword = ""
                        recoveryMessage = null
                    }) { Text("我已保存") }
                }
            },
            dismissButton = if (newRecoveryCode == null) {
                {
                    TextButton(onClick = {
                        showRecoveryDialog = false
                        recoveryPassword = ""
                        recoveryMessage = null
                    }) { Text("取消") }
                }
            } else {
                null
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("我的资料与账户") },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = account?.nickname ?: "游客模式",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (account != null) "本地匿名账户 · 不联网核验" else "还没有创建本地账户",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    account?.let {
                        Text(
                            text = "创建于 " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Date(it.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    if (account == null) {
                        Text(
                            "你现在是游客模式。创建本地账户后，可以在本机锁定应用、用恢复码找回密码。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                container.localAuthRepository.leaveGuest()
                                onAccountDeleted()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("创建本地账户") }
                    } else {
                        Button(onClick = onLock, modifier = Modifier.fillMaxWidth()) {
                            Text("锁定应用")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showChangePassword = !showChangePassword },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (showChangePassword) "收起修改密码" else "修改密码") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                showRecoveryDialog = true
                                recoveryPassword = ""
                                newRecoveryCode = null
                                recoveryMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("重新生成恢复码") }

                        if (showChangePassword) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = oldPassword,
                                onValueChange = { oldPassword = it },
                                label = { Text("旧密码") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = IsleRadius.field,
                                modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("新密码（至少 6 位）") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = IsleRadius.field,
                                modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                            )
                            message?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        when (val result = container.localAuthRepository.changePassword(oldPassword, newPassword)) {
                                            is AuthResult.Success -> {
                                                message = "密码已更新"
                                                oldPassword = ""
                                                newPassword = ""
                                            }
                                            is AuthResult.Failure -> message = result.message
                                            is AuthResult.Locked -> message = "请稍后再试"
                                        }
                                    }
                                },
                                enabled = oldPassword.isNotBlank() && newPassword.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("保存新密码") }
                        }

                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { showDelete = true }) {
                            Text("删除本地账户与全部数据", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("让 TA 更快认识你", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "这里填写的资料对所有角色共享，新角色一见面就知道你是谁。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = profile.name,
                        onValueChange = profileViewModel::updateName,
                        label = { Text("希望被怎么称呼") },
                        placeholder = { Text("例如：小彤") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = profile.personality,
                        onValueChange = profileViewModel::updatePersonality,
                        label = { Text("我的性格") },
                        placeholder = { Text("例如：慢热但很重感情，容易共情，偶尔会多想") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = profile.intro,
                        onValueChange = profileViewModel::updateIntro,
                        label = { Text("想对 TA 说的话") },
                        placeholder = { Text("例如：我最近在准备考试，压力有点大，希望有人能听我说说话") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                    )
                }
            }

            Text(
                text = "提示：账户、资料与记忆默认仅保存在本机；发送消息时，内容会传输至你配置的模型服务用于生成回复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

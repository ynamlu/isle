package com.isle.companion.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.isle.companion.data.auth.AuthResult
import com.isle.companion.di.appContainer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onLock: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val account by container.localAuthRepository.account.collectAsState()
    val guest by container.localAuthRepository.isGuest.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryPassword by remember { mutableStateOf("") }
    var newRecoveryCode by remember { mutableStateOf<String?>(null) }
    var recoveryMessage by remember { mutableStateOf<String?>(null) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hazeState = rememberLiquidGlassState()

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = {
                if (newRecoveryCode == null) {
                    showRecoveryDialog = false
                    recoveryPassword = ""
                }
            },
            title = {
                Text(if (newRecoveryCode == null) "重新生成恢复码" else "新的恢复码")
            },
            text = {
                if (newRecoveryCode == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("恢复码只在生成时显示一次，应用无法查看旧码。输入当前密码后会生成一个新的恢复码，旧码立即失效。")
                        OutlinedTextField(
                            value = recoveryPassword,
                            onValueChange = { recoveryPassword = it },
                            label = { Text("当前密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = IsleRadius.field,
                            modifier = Modifier.fillMaxWidth()
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
                    TextButton(
                        onClick = {
                            showRecoveryDialog = false
                            newRecoveryCode = null
                            recoveryPassword = ""
                            recoveryMessage = null
                        }
                    ) { Text("我已保存") }
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("账户与安全") },
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
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account?.nickname ?: "游客模式",
                        style = MaterialTheme.typography.headlineSmall
                    )
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
                }
            }

            if (account == null) {
                Text(
                    "你现在是游客模式。创建本地账户后，可以在本机锁定应用、用恢复码找回密码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

                OutlinedButton(
                    onClick = { showChangePassword = !showChangePassword },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (showChangePassword) "收起修改密码" else "修改密码") }

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
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("旧密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = IsleRadius.field,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新密码（至少 6 位）") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = IsleRadius.field,
                        modifier = Modifier.fillMaxWidth()
                    )
                    message?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
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

                Spacer(Modifier.height(8.dp))
                Text(
                    "心屿不收集手机号、邮箱和实名信息；这里的账户只用于本机身份识别与数据隔离，不是学籍或实名核验。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showDelete = true }) {
                    Text("删除本地账户与全部数据", color = MaterialTheme.colorScheme.error)
                }
            }

            if (guest && account == null) {
                Text(
                    "游客模式不锁定应用；创建账户后才会启用应用锁。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

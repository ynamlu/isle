package com.isle.companion.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.isle.companion.data.auth.AuthResult
import com.isle.companion.data.auth.LocalAuthRepository
import com.isle.companion.ui.component.keyboardRetryOnFocus
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.IsleBrandBrush
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.isleBackground
import kotlinx.coroutines.launch

private enum class AuthPage { WELCOME, REGISTER, LOGIN, RECOVERY, SHOW_CODE }

/**
 * 首次启动的本地账户入口：创建账户、登录、恢复码、游客模式。
 * 所有验证都在本机完成，不联网、不收集邮箱/手机号。
 */
@Composable
fun AuthScreen(
    repository: LocalAuthRepository,
    onAuthenticated: () -> Unit,
    onGuest: () -> Unit
) {
    var page by remember { mutableStateOf(AuthPage.WELCOME) }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var recoveryCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val existingAccount by repository.account.collectAsState()

    BackHandler(enabled = page != AuthPage.WELCOME) {
        page = AuthPage.WELCOME
        message = null
    }

    fun resetMessages() {
        message = null
        password = ""
        confirm = ""
        newPassword = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .isleBackground()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "心屿",
                style = MaterialTheme.typography.displaySmall.copy(brush = IsleBrandBrush),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "一座只属于你的心灵小岛",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (page) {
                        AuthPage.WELCOME -> WelcomeContent(
                            hasAccount = existingAccount != null,
                            onRegister = { resetMessages(); page = AuthPage.REGISTER },
                            onLogin = { resetMessages(); page = AuthPage.LOGIN },
                            onGuest = onGuest
                        )

                        AuthPage.REGISTER -> RegisterContent(
                            nickname = nickname,
                            password = password,
                            confirm = confirm,
                            busy = busy,
                            message = message,
                            onNickname = { nickname = it },
                            onPassword = { password = it },
                            onConfirm = { confirm = it },
                            onBack = { page = AuthPage.WELCOME; resetMessages() },
                            onSubmit = {
                                if (password != confirm) {
                                    message = "两次输入的密码不一致"
                                    return@RegisterContent
                                }
                                busy = true
                                message = null
                                scope.launch {
                                    when (val result = repository.createAccount(nickname, password)) {
                                        is AuthResult.Success -> {
                                            recoveryCode = result.recoveryCode
                                            page = AuthPage.SHOW_CODE
                                        }
                                        is AuthResult.Failure -> message = result.message
                                        is AuthResult.Locked -> message = "请稍后再试"
                                    }
                                    busy = false
                                }
                            }
                        )

                        AuthPage.LOGIN -> LoginContent(
                            nickname = repository.account.value?.nickname.orEmpty(),
                            password = password,
                            busy = busy,
                            message = message,
                            onPassword = { password = it },
                            onBack = { page = AuthPage.WELCOME; resetMessages() },
                            onForgot = { resetMessages(); page = AuthPage.RECOVERY },
                            onSubmit = {
                                busy = true
                                message = null
                                scope.launch {
                                    when (val result = repository.login(password)) {
                                        is AuthResult.Success -> onAuthenticated()
                                        is AuthResult.Failure -> message = result.message
                                        is AuthResult.Locked -> message = "尝试次数太多，请 ${result.seconds} 秒后再试"
                                    }
                                    busy = false
                                }
                            }
                        )

                        AuthPage.RECOVERY -> RecoveryContent(
                            code = recovery,
                            newPassword = newPassword,
                            busy = busy,
                            message = message,
                            onCode = { recovery = it },
                            onNewPassword = { newPassword = it },
                            onBack = { page = AuthPage.WELCOME; resetMessages() },
                            onSubmit = {
                                busy = true
                                message = null
                                scope.launch {
                                    when (val result = repository.resetWithRecovery(recovery, newPassword)) {
                                        is AuthResult.Success -> {
                                            recoveryCode = result.recoveryCode
                                            page = AuthPage.SHOW_CODE
                                        }
                                        is AuthResult.Failure -> message = result.message
                                        is AuthResult.Locked -> message = "请稍后再试"
                                    }
                                    busy = false
                                }
                            }
                        )

                        AuthPage.SHOW_CODE -> RecoveryCodeContent(
                            code = recoveryCode.orEmpty(),
                            onDone = onAuthenticated
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "本地匿名账户 · 不需要手机号或邮箱\n只在本机验证密码，不上传身份信息\n\n" +
                    "心屿提供情绪支持，不是心理咨询或医疗诊断；" +
                    "遇到危机请优先联系真人。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun WelcomeContent(
    hasAccount: Boolean,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onGuest: () -> Unit
) {
    Text("先认识一下", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text(
        "创建一个本地账户，保护你的对话和记忆；也可以先体验，之后再创建。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Button(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Person, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("创建本地账户")
    }
    if (hasAccount) {
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("已有账户，登录")
        }
    }
    TextButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) {
        Text("先体验（游客模式）")
    }
}

@Composable
private fun RegisterContent(
    nickname: String,
    password: String,
    confirm: String,
    busy: Boolean,
    message: String?,
    onNickname: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    AuthHeader(title = "创建本地账户", onBack = onBack)
    OutlinedTextField(
        value = nickname,
        onValueChange = onNickname,
        label = { Text("昵称") },
        placeholder = { Text("不需要真名") },
        singleLine = true,
        shape = IsleRadius.field,
        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text("密码（至少 6 位）") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = IsleRadius.field,
        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
    )
    OutlinedTextField(
        value = confirm,
        onValueChange = onConfirm,
        label = { Text("再输一次密码") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = IsleRadius.field,
        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
    )
    AuthMessage(message)
    Button(
        onClick = onSubmit,
        enabled = !busy && nickname.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text("创建账户")
    }
    Text(
        "密码只在本机校验；忘记密码只能用恢复码找回。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LoginContent(
    nickname: String,
    password: String,
    busy: Boolean,
    message: String?,
    onPassword: (String) -> Unit,
    onBack: () -> Unit,
    onForgot: () -> Unit,
    onSubmit: () -> Unit
) {
    AuthHeader(title = nickname.ifBlank { "登录" }, onBack = onBack)
    Text("欢迎回来", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text("密码") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = IsleRadius.field,
        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
    )
    AuthMessage(message)
    Button(onClick = onSubmit, enabled = !busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text("解锁")
    }
    TextButton(onClick = onForgot, modifier = Modifier.fillMaxWidth()) {
        Text("忘记密码？用恢复码")
    }
}

@Composable
private fun RecoveryContent(
    code: String,
    newPassword: String,
    busy: Boolean,
    message: String?,
    onCode: (String) -> Unit,
    onNewPassword: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    AuthHeader(title = "用恢复码找回", onBack = onBack)
    Text(
        "输入注册时保存的恢复码，设置一个新密码。恢复码是一次性的，重设后会生成新的。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = code,
        onValueChange = onCode,
        label = { Text("恢复码") },
        placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
        singleLine = true,
        shape = IsleRadius.field,
        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
    )
    OutlinedTextField(
        value = newPassword,
        onValueChange = onNewPassword,
        label = { Text("新密码（至少 6 位）") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = IsleRadius.field,
        modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
    )
    AuthMessage(message)
    Button(
        onClick = onSubmit,
        enabled = !busy && code.isNotBlank() && newPassword.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text("重设密码")
    }
}

@Composable
private fun RecoveryCodeContent(code: String, onDone: () -> Unit) {
    Text("请保存恢复码", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text(
        "这是找回账户的唯一凭证，只显示这一次。建议抄在纸上或存进密码管理器。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = code,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
    val context = LocalContext.current
    TextButton(
        onClick = {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("心屿恢复码", code)
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("复制恢复码") }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text("我保存好了，进入心屿")
    }
}

@Composable
private fun AuthHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AuthMessage(message: String?) {
    message?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/** 应用锁：从后台回来或重新打开时，需要再次输入密码。 */
@Composable
fun LockScreen(
    nickname: String,
    repository: LocalAuthRepository,
    onUnlocked: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    var recovery by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .isleBackground()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(42.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(nickname, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "心屿已锁定",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (!showRecovery) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = IsleRadius.field,
                    modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                )
                AuthMessage(message)
                Button(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            when (val result = repository.login(password)) {
                                is AuthResult.Success -> onUnlocked()
                                is AuthResult.Failure -> message = result.message
                                is AuthResult.Locked -> message = "尝试次数太多，请 ${result.seconds} 秒后再试"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("解锁")
                }
                TextButton(onClick = { showRecovery = true; message = null }) {
                    Text("忘记密码？用恢复码")
                }
            } else if (newCode == null) {
                OutlinedTextField(
                    value = recovery,
                    onValueChange = { recovery = it },
                    label = { Text("恢复码") },
                    singleLine = true,
                    shape = IsleRadius.field,
                    modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = IsleRadius.field,
                    modifier = Modifier.fillMaxWidth().keyboardRetryOnFocus()
                )
                AuthMessage(message)
                Button(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            when (val result = repository.resetWithRecovery(recovery, newPassword)) {
                                is AuthResult.Success -> newCode = result.recoveryCode
                                is AuthResult.Failure -> message = result.message
                                is AuthResult.Locked -> message = "请稍后再试"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && recovery.isNotBlank() && newPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("重设密码") }
                TextButton(onClick = { showRecovery = false; message = null }) { Text("返回") }
            } else {
                RecoveryCodeContent(code = newCode.orEmpty(), onDone = onUnlocked)
            }
        }
    }
}

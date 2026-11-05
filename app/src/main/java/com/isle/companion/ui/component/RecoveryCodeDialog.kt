package com.isle.companion.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isle.companion.ui.theme.GlassCard

/**
 * 注册成功后的恢复码确认框。
 * 放在 MainActivity 层，避免账户状态切换时页面被替换掉导致用户看不到恢复码。
 */
@Composable
fun RecoveryCodeDialog(code: String, onDone: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        title = { Text("请保存恢复码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "这是找回账户的唯一凭证，只显示这一次。请抄下来或存进密码管理器。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        textAlign = TextAlign.Center
                    )
                }
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("我保存好了") }
        }
    )
}

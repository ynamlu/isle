package com.isle.companion.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.IsleSpacing

@Composable
fun RecoveryCodeDialog(code: String, onDone: () -> Unit) {
    IsleDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        RecoveryCodeContent(code = code, onDone = onDone)
    }
}

@Composable
internal fun RecoveryCodeContent(
    code: String,
    onDone: () -> Unit,
    doneLabel: String = "我已保存"
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var copied by remember(code) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(IsleSpacing.md)) {
        Text("请保存恢复码", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "忘记密码时可以用恢复码重设。关闭后无法再查看，请抄下来或存进密码管理器。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = IsleRadius.field,
            color = colors.primaryContainer.copy(alpha = 0.65f)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth().padding(IsleSpacing.lg),
                textAlign = TextAlign.Center
            )
        }
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("心屿恢复码", code))
                copied = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = IsleRadius.field
        ) { Text(if (copied) "已复制恢复码" else "复制恢复码") }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), shape = IsleRadius.field) {
            Text(doneLabel)
        }
    }
}

package com.isle.companion.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

/**
 * 部分第三方输入法（例如懒加载的插件式输入法）在第一次获得焦点时
 * 可能没有及时弹出键盘。这里在焦点进入后做几次温和的重试，
 * 不修改系统输入法，也不弹“请更换输入法”的提示。
 */
fun Modifier.keyboardRetryOnFocus(): Modifier = composed {
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(focused) {
        if (focused) {
            delay(120)
            keyboard?.show()
            delay(300)
            keyboard?.show()
            delay(700)
            keyboard?.show()
        }
    }

    onFocusChanged { focused = it.isFocused }
}

package com.isle.companion.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * 图片选择器：支持「相册」与「拍照」两种来源。
 * 返回一个可调用的函数，调用后弹出选择对话框。
 */
@Composable
fun rememberImagePicker(
    title: String = "选择头像来源",
    onPicked: (Uri) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPicked) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingCameraUri?.let(onPicked) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text("从相册选择，或使用相机拍摄。") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("相册") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    val captureDir = File(context.cacheDir, "captures").apply { mkdirs() }
                    // 清掉一天前的拍照缓存，避免 cacheDir 无限增长
                    val now = System.currentTimeMillis()
                    captureDir.listFiles()?.forEach { old ->
                        if (now - old.lastModified() > 24 * 60 * 60 * 1000L) old.delete()
                    }
                    val file = File(captureDir, "cap_${UUID.randomUUID()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    pendingCameraUri = uri
                    takePicture.launch(uri)
                }) { Text("拍照") }
            }
        )
    }

    return { showDialog = true }
}

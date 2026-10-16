package com.isle.companion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * 角色头像的本地存储：把用户选择的图片压缩后存入应用内部目录。
 */
class AvatarStorage(
    private val context: Context,
    private val folder: String = "avatars",
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {

    private val avatarDir: File
        get() = File(context.filesDir, folder).apply { if (!exists()) mkdirs() }

    fun importFromUri(uri: Uri): String? = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            val raw = BitmapFactory.decodeStream(stream) ?: return null
            val scaled = scaleDown(raw, maxSize)
            val target = File(avatarDir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (scaled !== raw) scaled.recycle()
            raw.recycle()
            target.absolutePath
        }
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSize) return bitmap
        val ratio = maxSize.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    }

    companion object {
        /** 头像默认压缩边长 */
        private const val DEFAULT_MAX_SIZE = 512
        private const val JPEG_QUALITY = 88
    }
}

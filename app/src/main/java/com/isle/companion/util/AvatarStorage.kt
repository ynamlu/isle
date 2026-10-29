package com.isle.companion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * 本地图片存储：把用户选择的图片按目标尺寸采样、压缩后存入应用内部目录。
 * 头像与聊天图片共用，只是 folder / maxSize 不同。
 */
class AvatarStorage(
    private val context: Context,
    private val folder: String = "avatars",
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {

    private val avatarDir: File
        get() = File(context.filesDir, folder).apply { if (!exists()) mkdirs() }

    fun importFromUri(uri: Uri): String? = runCatching {
        val resolver = context.contentResolver

        // 先只读尺寸，计算采样率，避免把整张 4000px 原图解码进内存
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val raw = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val scaled = scaleDown(raw, maxSize)
        val target = File(avatarDir, "img_${UUID.randomUUID()}.jpg")
        FileOutputStream(target).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (scaled !== raw) scaled.recycle()
        raw.recycle()
        target.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSize) {
            sample *= 2
        }
        return sample
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

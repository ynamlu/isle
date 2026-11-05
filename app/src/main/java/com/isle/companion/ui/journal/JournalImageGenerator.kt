package com.isle.companion.ui.journal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.isle.companion.domain.journal.JournalEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 生成 1080×1440 的情绪手账图：
 * 品牌渐变背景 + 金句 + 情绪标签 + 角色落款 + 右下角渠道二维码。
 */
object JournalImageGenerator {

    private const val W = 1080
    private const val H = 1440

    fun generate(
        context: Context,
        entry: JournalEntry,
        characterName: String,
        qrUrl: String
    ): File {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeader(canvas)

        val card = RectF(84f, 250f, W - 84f, 1080f)
        drawCard(canvas, card)

        val titlePaint = textPaint(34f, Color.argb(220, 255, 255, 255), Typeface.NORMAL)
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(entry.title, W / 2f, 370f, titlePaint)

        val quotePaint = textPaint(58f, Color.WHITE, Typeface.BOLD)
        quotePaint.textAlign = Paint.Align.CENTER
        val quoteTop = 480f
        val quoteBottom = drawCenteredWrapped(
            canvas = canvas,
            text = "“${entry.quote}”",
            centerX = W / 2f,
            top = quoteTop,
            maxWidth = card.width() - 140f,
            paint = quotePaint,
            lineHeight = 86f
        )

        drawEmotionChip(canvas, entry.emotion, W / 2f, quoteBottom + 40f)

        val signaturePaint = textPaint(32f, Color.argb(210, 255, 255, 255), Typeface.NORMAL)
        signaturePaint.textAlign = Paint.Align.LEFT
        canvas.drawText("—— $characterName", card.left + 70f, card.bottom - 70f, signaturePaint)

        val brandPaint = textPaint(24f, Color.argb(170, 255, 255, 255), Typeface.NORMAL)
        brandPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("心屿 · 情绪手账", card.left + 70f, card.bottom - 28f, brandPaint)

        drawQr(canvas, qrUrl, W - 296f, 1130f, 212f)

        val footerPaint = textPaint(26f, Color.argb(190, 255, 255, 255), Typeface.NORMAL)
        footerPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("愿每一座孤岛，都能被温柔抵达。", 84f, 1352f, footerPaint)

        val dir = File(context.cacheDir, "journal").apply { mkdirs() }
        val file = File(dir, "isle_journal_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, W.toFloat(), H.toFloat(),
            intArrayOf(
                Color.parseColor("#1D1B4B"),
                Color.parseColor("#2F6FB2"),
                Color.parseColor("#66D6C2")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)

        // 柔光圆
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.shader = RadialGradient(
            W * 0.82f, 160f, 520f,
            intArrayOf(Color.argb(70, 255, 240, 210), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(W * 0.82f, 160f, 520f, glow)

        // 左下波纹
        val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.argb(70, 255, 255, 255)
            strokeCap = Paint.Cap.ROUND
        }
        for (index in 0..2) {
            val y = 1240f + index * 26f
            val path = Path().apply {
                moveTo(84f, y)
                cubicTo(180f, y - 18f, 250f, y + 18f, 340f, y)
                cubicTo(430f, y - 18f, 500f, y + 18f, 590f, y)
            }
            canvas.drawPath(path, wave)
        }
    }

    private fun drawHeader(canvas: Canvas) {
        val label = textPaint(26f, Color.argb(220, 255, 255, 255), Typeface.BOLD)
        label.letterSpacing = 0.12f
        canvas.drawText("ISLE · 情绪手账", 84f, 128f, label)

        val date = textPaint(22f, Color.argb(160, 255, 255, 255), Typeface.NORMAL)
        date.textAlign = Paint.Align.RIGHT
        val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        canvas.drawText(dateText, W - 84f, 126f, date)

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawLine(84f, 170f, W - 84f, 170f, line)
    }

    private fun drawCard(canvas: Canvas, card: RectF) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, 255, 255, 255)
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(90, 255, 255, 255)
        }
        val radius = 36f
        canvas.drawRoundRect(card, radius, radius, fill)
        canvas.drawRoundRect(card, radius, radius, border)
    }

    private fun drawEmotionChip(canvas: Canvas, emotion: String, centerX: Float, top: Float) {
        val text = "此刻 · $emotion"
        val paint = textPaint(28f, Color.WHITE, Typeface.NORMAL)
        val width = paint.measureText(text) + 52f
        val height = 58f
        val rect = RectF(centerX - width / 2f, top, centerX + width / 2f, top + height)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(42, 255, 255, 255) }
        canvas.drawRoundRect(rect, height / 2f, height / 2f, fill)
        paint.textAlign = Paint.Align.CENTER
        val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, centerX, baseline, paint)
    }

    private fun drawQr(canvas: Canvas, url: String, left: Float, top: Float, size: Float) {
        val box = RectF(left, top, left + size, top + size)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(box, 24f, 24f, white)

        val qrSize = (size - 34f).toInt()
        val matrix = runCatching {
            QRCodeWriter().encode(
                url,
                BarcodeFormat.QR_CODE,
                qrSize,
                qrSize,
                mapOf(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8"
                )
            )
        }.getOrNull()

        if (matrix != null) {
            val qr = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    qr.setPixel(
                        x, y,
                        if (matrix[x, y]) Color.parseColor("#13233A") else Color.WHITE
                    )
                }
            }
            canvas.drawBitmap(qr, null, RectF(left + 17f, top + 17f, left + size - 17f, top + size - 17f), null)
            qr.recycle()
        }

        val caption = textPaint(22f, Color.argb(190, 255, 255, 255), Typeface.NORMAL)
        caption.textAlign = Paint.Align.RIGHT
        canvas.drawText("扫码了解心屿", left + size, top + size + 40f, caption)
    }

    private fun textPaint(size: Float, color: Int, style: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create("sans-serif", style)
        }

    private fun drawCenteredWrapped(
        canvas: Canvas,
        text: String,
        centerX: Float,
        top: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidth, null)
            if (count <= 0) break
            lines += remaining.substring(0, count)
            remaining = remaining.substring(count)
        }
        var y = top
        lines.forEach { line ->
            canvas.drawText(line, centerX, y, paint)
            y += lineHeight
        }
        return y
    }
}

package com.rojas.syncpass.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class QrPayload(val eventId: Long?, val token: String, val raw: String)

object Qr {

    private const val PREFIX = "SYNCPASS|EVENT|"

    fun payload(eventId: Long, token: String): String = "$PREFIX$eventId|$token"

    fun parse(raw: String): QrPayload? {
        val code = raw.trim()
        if (code.isBlank()) return null
        if (code.startsWith(PREFIX)) {
            val parts = code.split("|")
            if (parts.size < 4) return null
            val id = parts[2].toLongOrNull() ?: return null
            val token = parts[3].ifBlank { return null }
            return QrPayload(id, token, code)
        }
        return QrPayload(null, code, code)
    }

    fun generate(content: String, size: Int = 768, darkColor: Int = 0xFF271539.toInt()): Bitmap {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (matrix[x, y]) darkColor else Color.WHITE
            }
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }
}

package com.example.momentra.ui.shell.empty.group

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

internal fun generateInviteQrBitmap(content: String, sizePx: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val dark = 0xFF14121B.toInt()
    val light = 0xFFFFFFFF.toInt()
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        val offset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[offset + x] = if (matrix[x, y]) dark else light
        }
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
}

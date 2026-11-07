package com.ipandai.edompet.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.ipandai.edompet.data.CODE_TYPE_BARCODE
import com.ipandai.edompet.data.CODE_TYPE_QR

/** Formats the scanner accepts, same set as the iOS AVCaptureMetadataOutput. */
val SCAN_FORMATS = listOf(
    BarcodeFormat.QR_CODE, BarcodeFormat.EAN_8, BarcodeFormat.EAN_13,
    BarcodeFormat.CODE_128, BarcodeFormat.PDF_417,
)

fun codeTypeFor(formatName: String?): String =
    if (formatName == BarcodeFormat.QR_CODE.name) CODE_TYPE_QR else CODE_TYPE_BARCODE

data class DecodedCode(val value: String, val codeType: String)

/** Detect a QR code or barcode in a picked photo. Returns null if none found. */
fun decodeFromImage(context: Context, uri: Uri): DecodedCode? {
    val bitmap = loadScaledBitmap(context, uri, maxSide = 1600) ?: return null
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to SCAN_FORMATS,
    )
    val reader = MultiFormatReader().apply { setHints(hints) }
    val result = runCatching { reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))) }
        .recoverCatching { reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert()))) }
        .getOrNull() ?: return null
    return DecodedCode(result.text, codeTypeFor(result.barcodeFormat.name))
}

private fun loadScaledBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

/** Render the card's code so it can be shown to a cashier. */
fun renderCode(value: String, isBarcode: Boolean, width: Int, height: Int): Bitmap? {
    val format = when {
        !isBarcode -> BarcodeFormat.QR_CODE
        value.length == 13 && value.all { it.isDigit() } -> BarcodeFormat.EAN_13
        value.length == 8 && value.all { it.isDigit() } -> BarcodeFormat.EAN_8
        else -> BarcodeFormat.CODE_128
    }
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = runCatching { MultiFormatWriter().encode(value, format, width, height, hints) }
        .recoverCatching {
            // e.g. EAN checksum mismatch: fall back to Code 128
            MultiFormatWriter().encode(value, BarcodeFormat.CODE_128, width, height, hints)
        }
        .getOrNull() ?: return null
    val px = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) {
        val off = y * matrix.width
        for (x in 0 until matrix.width) px[off + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(px, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}


package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.example.data.model.Sale
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QrCodeUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeFromYuvPlane(
        yBytes: ByteArray,
        width: Int,
        height: Int
    ): String? {
        return try {
            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(
                            BarcodeFormat.QR_CODE,
                            BarcodeFormat.CODE_128,
                            BarcodeFormat.EAN_13,
                            BarcodeFormat.EAN_8,
                            BarcodeFormat.UPC_A
                        ),
                        DecodeHintType.TRY_HARDER to true
                    )
                )
            }
            reader.decode(binaryBitmap).text
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses product QR payloads `KBP-PROD:<4-digit>:<id>` or raw 4-digit codes / SKUs.
     */
    fun extractProductCode(rawPayload: String): String {
        val trimmed = rawPayload.trim()
        if (trimmed.startsWith("KBP-PROD:", ignoreCase = true)) {
            val parts = trimmed.split(":")
            if (parts.size >= 2 && parts[1].isNotBlank()) {
                return parts[1].trim()
            }
        }
        return trimmed
    }

    /**
     * Parses wireless scanner pairing payloads `KBP-PAIR:<4-digit>:<storeId>` or `/?scanner=XXXX`.
     */
    fun extractScannerRoomCode(rawPayload: String): String? {
        val trimmed = rawPayload.trim()
        if (trimmed.startsWith("KBP-PAIR:", ignoreCase = true)) {
            val parts = trimmed.split(":")
            if (parts.size >= 2 && parts[1].length in 4..8) {
                return parts[1].trim()
            }
        }
        val queryIdx = trimmed.indexOf("scanner=")
        if (queryIdx >= 0) {
            val after = trimmed.substring(queryIdx + 8).takeWhile { it.isLetterOrDigit() }
            if (after.length in 4..8) return after
        }
        if (trimmed.length in 4..6 && trimmed.all { it.isDigit() }) {
            return trimmed
        }
        return null
    }

    fun buildSelfContainedBillUrl(sale: Sale): String {
        val compact = JSONObject().apply {
            put("b", sale.shortBillId)
            put("inv", sale.invoiceNumber)
            put("s", sale.storeName.take(40))
            put("c", sale.currencySymbol)
            put("sub", sale.subtotal)
            put("gst", sale.gstTotal)
            put("disc", sale.discountAmount)
            put("tot", sale.grandTotal)
            put("pm", sale.paymentMethod)
            val arr = JSONArray()
            sale.items.take(15).forEach { li ->
                arr.put(
                    JSONObject().apply {
                        put("n", li.name.take(28))
                        put("q", li.quantity)
                        put("p", li.unitPrice)
                        put("g", li.gstPercent)
                        put("t", li.lineTotal)
                    }
                )
            }
            put("i", arr)
        }
        val encoded = Base64.encodeToString(
            compact.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "https://kalambilling.app/?b=${sale.shortBillId}#d=$encoded"
    }
}

object FormatUtils {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun formatMoney(amount: Double, currencySymbol: String = "₹"): String {
        return "$currencySymbol${numberFormat.format(amount)}"
    }

    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else String.format(Locale.US, "%.2f", qty)
    }

    fun formatDateTime(date: Date?): String {
        val target = date ?: Date()
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(target)
    }

    fun formatShortDate(date: Date?): String {
        val target = date ?: Date()
        return SimpleDateFormat("dd MMM", Locale.US).format(target)
    }
}

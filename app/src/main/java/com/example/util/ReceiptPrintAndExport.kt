package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.model.Product
import com.example.data.model.QrLabelSize
import com.example.data.model.Sale
import com.example.data.model.Store
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.ceil

object ReceiptPrintAndExport {

    private const val MM_TO_PT = 2.83465f

    fun generateInvoicePdfBytes(sale: Sale, thermalMode: Boolean = true): ByteArray {
        val pageWidthPt = if (thermalMode) {
            if (sale.thermalWidthMm == 58) (58f * MM_TO_PT).toInt() else (80f * MM_TO_PT).toInt()
        } else {
            595 // A4 width in points
        }
        val baseHeight = if (thermalMode) 360 + (sale.items.size * 28) else 842
        val pageHeightPt = baseHeight.coerceAtLeast(if (thermalMode) 420 else 842)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
        val page = document.startPage(pageInfo)
        drawInvoiceOnCanvas(
            canvas = page.canvas,
            width = pageWidthPt.toFloat(),
            height = pageHeightPt.toFloat(),
            sale = sale,
            thermalMode = thermalMode
        )
        document.finishPage(page)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    fun generateReceiptPngBytes(sale: Sale): ByteArray {
        val widthPx = 760
        val heightPx = (920 + sale.items.size * 68).coerceAtLeast(980)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val scale = widthPx / 226f
        canvas.save()
        canvas.scale(scale, scale)
        drawInvoiceOnCanvas(
            canvas = canvas,
            width = 226f,
            height = heightPx / scale,
            sale = sale,
            thermalMode = true
        )
        canvas.restore()

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun drawInvoiceOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        sale: Sale,
        thermalMode: Boolean
    ) {
        canvas.drawColor(Color.WHITE)
        val margin = if (thermalMode) 10f else 36f
        val contentWidth = width - margin * 2
        val titleSize = if (thermalMode) 11f else 18f
        val bodySize = if (thermalMode) 7.5f else 10.5f
        val smallSize = if (thermalMode) 6.5f else 9f

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = titleSize
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = bodySize
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.MONOSPACE
            textSize = bodySize
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = smallSize
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }

        var y = margin + titleSize
        val centerX = width / 2f

        if (sale.receiptHeader.isNotBlank()) {
            canvas.drawText(sale.receiptHeader.take(46), centerX, y, mutedPaint)
            y += smallSize + 4f
        }

        canvas.drawText(sale.storeName.ifBlank { "KALAM BILLING PRO" }.take(34), centerX, y, boldPaint)
        y += titleSize * 0.9f + 2f

        if (sale.storeAddress.isNotBlank()) {
            canvas.drawText(sale.storeAddress.take(48), centerX, y, mutedPaint)
            y += smallSize + 3f
        }
        val contactLine = listOfNotNull(
            sale.storePhone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            sale.storeGstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" }
        ).joinToString("  |  ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine.take(54), centerX, y, mutedPaint)
            y += smallSize + 5f
        }

        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += bodySize + 4f

        // Invoice metadata
        bodyPaint.textAlign = Paint.Align.LEFT
        monoPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Invoice: ${sale.invoiceNumber}", margin, y, bodyPaint)
        canvas.drawText(sale.paymentMethod, width - margin, y, monoPaint)
        y += bodySize + 3f
        val dateStr = FormatUtils.formatDateTime(sale.createdAt?.toDate() ?: Date())
        canvas.drawText("Date: $dateStr", margin, y, bodyPaint)
        canvas.drawText("#${sale.shortBillId}", width - margin, y, monoPaint)
        y += 6f

        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += bodySize + 4f

        // Table Header
        val headerPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.parseColor("#0F172A")
        }
        canvas.drawText("ITEM", margin, y, headerPaint)
        val rightHeader = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("QTY × RATE", width - margin - (contentWidth * 0.28f), y, rightHeader)
        canvas.drawText("AMT", width - margin, y, rightHeader)
        y += 4f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += bodySize + 4f

        // Line items
        for (item in sale.items) {
            bodyPaint.textAlign = Paint.Align.LEFT
            val nameTrunc = item.name.take(if (thermalMode) 22 else 42)
            canvas.drawText(nameTrunc, margin, y, bodyPaint)
            val qtyRate = "${FormatUtils.formatQty(item.quantity)}×${FormatUtils.formatMoney(item.unitPrice, sale.currencySymbol)}"
            monoPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(qtyRate, width - margin - (contentWidth * 0.28f), y, monoPaint)
            canvas.drawText(FormatUtils.formatMoney(item.lineTotal, sale.currencySymbol), width - margin, y, monoPaint)
            y += bodySize + 2f
            val subNotePaint = Paint(mutedPaint).apply { textAlign = Paint.Align.LEFT }
            canvas.drawText(
                "Code ${item.shortCode} · GST ${FormatUtils.formatQty(item.gstPercent)}%",
                margin,
                y,
                subNotePaint
            )
            y += smallSize + 5f
        }

        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += bodySize + 5f

        // Totals
        fun drawTotalRow(label: String, value: String, bold: Boolean = false) {
            val lPaint = if (bold) headerPaint else bodyPaint
            val vPaint = Paint(monoPaint).apply {
                if (bold) {
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textSize = bodySize + 1.5f
                }
            }
            lPaint.textAlign = Paint.Align.LEFT
            vPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, margin, y, lPaint)
            canvas.drawText(value, width - margin, y, vPaint)
            y += (if (bold) bodySize + 6f else bodySize + 4f)
        }

        drawTotalRow("Taxable Subtotal", FormatUtils.formatMoney(sale.subtotal, sale.currencySymbol))
        drawTotalRow("Total GST", FormatUtils.formatMoney(sale.gstTotal, sale.currencySymbol))
        if (sale.discountAmount > 0.0) {
            drawTotalRow("Bill Discount", "-${FormatUtils.formatMoney(sale.discountAmount, sale.currencySymbol)}")
        }
        canvas.drawLine(margin, y - 2f, width - margin, y - 2f, linePaint)
        y += 4f
        drawTotalRow("GRAND TOTAL", FormatUtils.formatMoney(sale.grandTotal, sale.currencySymbol), bold = true)

        if (sale.storeUpiId.isNotBlank()) {
            y += 2f
            canvas.drawText("UPI: ${sale.storeUpiId}", centerX, y, mutedPaint)
            y += smallSize + 4f
        }

        // Digital Bill QR Code
        val qrSize = if (thermalMode) 76f else 110f
        if (y + qrSize + 24f < height) {
            val qrBitmap = QrCodeUtils.generateQrBitmap(sale.digitalBillUrl, 256)
            if (qrBitmap != null) {
                val left = (width - qrSize) / 2f
                val dest = RectF(left, y + 4f, left + qrSize, y + 4f + qrSize)
                canvas.drawBitmap(qrBitmap, null, dest, null)
                qrBitmap.recycle()
                y += qrSize + 14f
                canvas.drawText("Scan for Digital Receipt (${sale.shortBillId})", centerX, y, mutedPaint)
                y += smallSize + 6f
            }
        }

        if (sale.receiptFooter.isNotBlank() && y + 10f < height) {
            canvas.drawText(sale.receiptFooter.take(56), centerX, y, mutedPaint)
        }
    }

    fun generateQrStickerSheetPdfBytes(
        store: Store,
        selections: List<Pair<Product, Int>>,
        labelSize: QrLabelSize
    ): ByteArray {
        val expanded = mutableListOf<Product>()
        selections.forEach { (product, copies) ->
            repeat(copies.coerceIn(0, 200)) { expanded.add(product) }
        }
        val pageWidthPt = 595 // A4
        val pageHeightPt = 842
        val marginPt = 24f
        val gapPt = 8f
        val labelW = labelSize.widthMm * MM_TO_PT
        val labelH = labelSize.heightMm * MM_TO_PT

        val cols = ((pageWidthPt - marginPt * 2 + gapPt) / (labelW + gapPt)).toInt().coerceAtLeast(1)
        val rows = ((pageHeightPt - marginPt * 2 - 24f + gapPt) / (labelH + gapPt)).toInt().coerceAtLeast(1)
        val perPage = cols * rows
        val totalPages = ceil(expanded.size.coerceAtLeast(1).toDouble() / perPage).toInt()

        val document = PdfDocument()
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = if (labelSize == QrLabelSize.LARGE_70X35) 9f else 6.8f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = if (labelSize == QrLabelSize.LARGE_70X35) 9.5f else 7.2f
        }
        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = if (labelSize == QrLabelSize.LARGE_70X35) 11f else 8f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
        }

        val qrCache = mutableMapOf<String, Bitmap?>()
        try {
            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                canvas.drawText(
                    "${store.name} · QR Sticker Sheet (${labelSize.label}) · Page ${pageIndex + 1}/$totalPages",
                    marginPt,
                    16f,
                    headerPaint
                )

                val startIdx = pageIndex * perPage
                val endIdx = minOf(startIdx + perPage, expanded.size)
                for (i in startIdx until endIdx) {
                    val slot = i - startIdx
                    val col = slot % cols
                    val row = slot / cols
                    val left = marginPt + col * (labelW + gapPt)
                    val top = marginPt + 12f + row * (labelH + gapPt)
                    val rect = RectF(left, top, left + labelW, top + labelH)
                    canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

                    val product = expanded[i]
                    val payload = product.qrPayload.ifBlank { "KBP-PROD:${product.shortCode}:${product.id}" }
                    val qr = qrCache.getOrPut(payload) { QrCodeUtils.generateQrBitmap(payload, 200) }
                    val qrSide = (labelH - 8f).coerceAtMost(labelW * 0.42f)
                    if (qr != null) {
                        val qrDest = RectF(
                            left + 4f,
                            top + (labelH - qrSide) / 2f,
                            left + 4f + qrSide,
                            top + (labelH + qrSide) / 2f
                        )
                        canvas.drawBitmap(qr, null, qrDest, null)
                    }

                    val textLeft = left + qrSide + 8f
                    val maxChars = if (labelSize == QrLabelSize.LARGE_70X35) 22 else 13
                    canvas.drawText(
                        product.name.take(maxChars),
                        textLeft,
                        top + (labelH * 0.30f),
                        namePaint
                    )
                    canvas.drawText(
                        "#${product.shortCode}",
                        textLeft,
                        top + (labelH * 0.56f),
                        monoPaint
                    )
                    canvas.drawText(
                        FormatUtils.formatMoney(product.sellingPrice, store.currencySymbol),
                        textLeft,
                        top + (labelH * 0.84f),
                        pricePaint
                    )
                }

                document.finishPage(page)
            }
        } finally {
            qrCache.values.forEach { it?.recycle() }
        }

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    /**
     * Explicit Android PrintManager job. Never called automatically on bill creation.
     * Returns true if print dialog launched, false if print service is unavailable (caller can trigger PDF save).
     */
    fun printPdfBytes(context: Context, jobName: String, pdfBytes: ByteArray): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
        return try {
            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    try {
                        FileOutputStream(destination.fileDescriptor).use { it.write(pdfBytes) }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.localizedMessage)
                    }
                }
            }
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Generates a genuine Office Open XML (.xlsx) workbook with Summary, Invoices, and Inventory sheets.
     */
    fun generateExcelXlsxBytes(
        store: Store,
        sales: List<Sale>,
        products: List<Product>
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun addEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
            )
            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )
            addEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Sales Summary" sheetId="1" r:id="rId1"/>
    <sheet name="Invoices" sheetId="2" r:id="rId2"/>
    <sheet name="Inventory" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""
            )
            addEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
</Relationships>"""
            )

            // Sheet 1: Summary & GST
            val totalRevenue = sales.sumOf { it.grandTotal }
            val totalGst = sales.sumOf { it.gstTotal }
            val totalProfit = sales.sumOf { it.estimatedProfit }
            val totalUnits = sales.sumOf { it.totalUnits }
            val summaryRows = listOf(
                listOf("KALAM BILLING PRO — SALES & GST REPORT"),
                listOf("Business Name", store.name),
                listOf("GSTIN", store.gstin.ifBlank { "N/A" }),
                listOf("Generated At", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())),
                listOf(""),
                listOf("Metric", "Value"),
                listOf("Total Bills", sales.size.toString()),
                listOf("Total Units Sold", FormatUtils.formatQty(totalUnits)),
                listOf("Gross Revenue (${store.currencySymbol})", String.format(Locale.US, "%.2f", totalRevenue)),
                listOf("Total GST Collected (${store.currencySymbol})", String.format(Locale.US, "%.2f", totalGst)),
                listOf("Estimated Net Profit (${store.currencySymbol})", String.format(Locale.US, "%.2f", totalProfit))
            )
            addEntry("xl/worksheets/sheet1.xml", buildWorksheetXml(summaryRows))

            // Sheet 2: Invoices
            val invoiceRows = mutableListOf(
                listOf("Invoice Number", "Bill ID", "Date", "Payment", "Items", "Subtotal", "GST", "Discount", "Grand Total", "Est. Profit")
            )
            sales.forEach { s ->
                invoiceRows.add(
                    listOf(
                        s.invoiceNumber,
                        s.shortBillId,
                        FormatUtils.formatDateTime(s.createdAt?.toDate()),
                        s.paymentMethod,
                        FormatUtils.formatQty(s.totalUnits),
                        String.format(Locale.US, "%.2f", s.subtotal),
                        String.format(Locale.US, "%.2f", s.gstTotal),
                        String.format(Locale.US, "%.2f", s.discountAmount),
                        String.format(Locale.US, "%.2f", s.grandTotal),
                        String.format(Locale.US, "%.2f", s.estimatedProfit)
                    )
                )
            }
            addEntry("xl/worksheets/sheet2.xml", buildWorksheetXml(invoiceRows))

            // Sheet 3: Inventory
            val invRows = mutableListOf(
                listOf("4-Digit Code", "Name", "SKU", "Category", "Unit", "Purchase Price", "Selling Price", "GST %", "Stock Qty", "Min Alert")
            )
            products.forEach { p ->
                invRows.add(
                    listOf(
                        p.shortCode,
                        p.name,
                        p.sku,
                        p.category,
                        p.unit,
                        String.format(Locale.US, "%.2f", p.purchasePrice),
                        String.format(Locale.US, "%.2f", p.sellingPrice),
                        String.format(Locale.US, "%.2f", p.gstPercent),
                        FormatUtils.formatQty(p.stockQty),
                        FormatUtils.formatQty(p.minStockAlert)
                    )
                )
            }
            addEntry("xl/worksheets/sheet3.xml", buildWorksheetXml(invRows))
        }
        return out.toByteArray()
    }

    private fun buildWorksheetXml(rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, row ->
            sb.append("""<row r="${rIdx + 1}">""")
            row.forEachIndexed { cIdx, cell ->
                val colRef = ('A' + cIdx).toString() + (rIdx + 1)
                val escaped = cell
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                sb.append("""<c r="$colRef" t="inlineStr"><is><t>$escaped</t></is></c>""")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }
}

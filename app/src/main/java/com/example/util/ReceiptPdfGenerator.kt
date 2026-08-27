package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingsEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

enum class ReceiptPaperSize(val label: String, val widthPt: Int, val description: String) {
    THERMAL_58MM("Thermal 58mm", 204, "Ukuran standar printer bluetooth portabel (58mm)"),
    THERMAL_80MM("Thermal 80mm", 288, "Ukuran printer kasir meja POS (80mm)"),
    STANDARD_A4("Standar A4", 595, "Ukuran dokumen kertas faktur A4")
}

object ReceiptPdfGenerator {

    /**
     * Generates a PDF file for a transaction receipt based on the chosen paper size.
     */
    fun generateReceiptPdf(
        context: Context,
        transactionWithItems: TransactionWithItems,
        storeSettings: StoreSettingsEntity?,
        paperSize: ReceiptPaperSize = ReceiptPaperSize.THERMAL_58MM,
        customStoreName: String? = null,
        customAddress: String? = null,
        customTextLogo: String? = null
    ): File? {
        val trx = transactionWithItems.transaction
        val items = transactionWithItems.items
        val isVoided = trx.status == "VOIDED"

        val width = paperSize.widthPt
        val margin = when (paperSize) {
            ReceiptPaperSize.THERMAL_58MM -> 12f
            ReceiptPaperSize.THERMAL_80MM -> 18f
            ReceiptPaperSize.STANDARD_A4 -> 36f
        }
        val contentWidth = width - (margin * 2)

        // Setup typography paints
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = when (paperSize) {
                ReceiptPaperSize.THERMAL_58MM -> 8.5f
                ReceiptPaperSize.THERMAL_80MM -> 10f
                ReceiptPaperSize.STANDARD_A4 -> 11f
            }
            typeface = Typeface.MONOSPACE
        }

        val logoPaint = Paint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
            textSize = when (paperSize) {
                ReceiptPaperSize.THERMAL_58MM -> 10f
                ReceiptPaperSize.THERMAL_80MM -> 12f
                ReceiptPaperSize.STANDARD_A4 -> 14f
            }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = when (paperSize) {
                ReceiptPaperSize.THERMAL_58MM -> 11.5f
                ReceiptPaperSize.THERMAL_80MM -> 13.5f
                ReceiptPaperSize.STANDARD_A4 -> 16f
            }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
            textSize = when (paperSize) {
                ReceiptPaperSize.THERMAL_58MM -> 7.5f
                ReceiptPaperSize.THERMAL_80MM -> 8.5f
                ReceiptPaperSize.STANDARD_A4 -> 9.5f
            }
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = textPaint.textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val totalPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = when (paperSize) {
                ReceiptPaperSize.THERMAL_58MM -> 10.5f
                ReceiptPaperSize.THERMAL_80MM -> 12f
                ReceiptPaperSize.STANDARD_A4 -> 13f
            }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val discountPaint = Paint().apply {
            color = Color.rgb(180, 0, 0)
            isAntiAlias = true
            textSize = textPaint.textSize * 0.9f
            typeface = Typeface.MONOSPACE
        }

        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }

        val solidDividerPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }

        val logoBorderPaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // Calculate estimated height dynamically
        val lineHeight = textPaint.textSize + 4f
        var estimatedHeight = margin * 2 + 100f // store header
        if (!customTextLogo.isNullOrBlank()) estimatedHeight += 40f
        estimatedHeight += 90f // meta rows
        estimatedHeight += (items.size * (lineHeight * 2.5f + 8f)) // item lines
        estimatedHeight += 120f // calculation & total
        if (trx.paymentMethod == "TUNAI") estimatedHeight += 40f
        estimatedHeight += 80f // footer & barcode representation

        val pageHeight = if (paperSize == ReceiptPaperSize.STANDARD_A4) {
            842 // Standard A4 height in pt
        } else {
            estimatedHeight.toInt().coerceAtLeast(350)
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background white
        canvas.drawColor(Color.WHITE)

        var y = margin + 14f

        // 1. Text Logo (if configured)
        if (!customTextLogo.isNullOrBlank()) {
            val logoText = customTextLogo.trim()
            val textWidth = logoPaint.measureText(logoText)
            val boxPaddingH = 12f
            val boxPaddingV = 4f
            val boxLeft = (width / 2f) - (textWidth / 2f) - boxPaddingH
            val boxRight = (width / 2f) + (textWidth / 2f) + boxPaddingH
            val boxTop = y - logoPaint.textSize + 2f
            val boxBottom = y + boxPaddingV + 2f

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 4f, 4f, logoBorderPaint)
            canvas.drawText(logoText, width / 2f, y, logoPaint)
            y += logoPaint.textSize + 12f
        }

        // 2. Store Header
        val finalStoreName = if (!customStoreName.isNullOrBlank()) customStoreName.trim() else storeSettings?.storeName ?: "KASIR POS OFFLINE"
        canvas.drawText(finalStoreName, width / 2f, y, titlePaint)
        y += titlePaint.textSize + 3f

        val finalAddress = if (!customAddress.isNullOrBlank()) customAddress.trim() else storeSettings?.address
        if (!finalAddress.isNullOrBlank()) {
            val addressLines = wrapText(finalAddress, contentWidth, headerPaint)
            for (line in addressLines) {
                canvas.drawText(line, width / 2f, y, headerPaint)
                y += headerPaint.textSize + 2f
            }
        }

        val phone = storeSettings?.phone
        if (!phone.isNullOrBlank()) {
            canvas.drawText("Telp: $phone", width / 2f, y, headerPaint)
            y += headerPaint.textSize + 4f
        }

        y += 4f
        canvas.drawLine(margin, y, width - margin, y, dividerPaint)
        y += 12f

        // 2. Transaction Metadata
        drawTwoColText(canvas, "No. Trx", trx.transactionNumber, margin, contentWidth, y, textPaint, boldPaint)
        y += lineHeight
        drawTwoColText(canvas, "Waktu", FormatUtils.formatDate(trx.timestamp), margin, contentWidth, y, textPaint, textPaint)
        y += lineHeight
        drawTwoColText(canvas, "Kasir", trx.cashierName, margin, contentWidth, y, textPaint, textPaint)
        y += lineHeight
        drawTwoColText(canvas, "Pelanggan", trx.customerName, margin, contentWidth, y, textPaint, textPaint)
        y += lineHeight
        drawTwoColText(canvas, "Metode", trx.paymentMethod, margin, contentWidth, y, textPaint, boldPaint)
        y += lineHeight

        if (isVoided) {
            drawTwoColText(canvas, "STATUS", "BATAL / VOID", margin, contentWidth, y, discountPaint, discountPaint)
            y += lineHeight
            if (trx.voidReason.isNotBlank()) {
                drawTwoColText(canvas, "Alasan", trx.voidReason, margin, contentWidth, y, discountPaint, discountPaint)
                y += lineHeight
            }
        }

        y += 4f
        canvas.drawLine(margin, y, width - margin, y, dividerPaint)
        y += 12f

        // 3. Items List
        for (item in items) {
            // Product Name
            val nameLines = wrapText(item.productName, contentWidth, boldPaint)
            for (nLine in nameLines) {
                canvas.drawText(nLine, margin, y, boldPaint)
                y += boldPaint.textSize + 2f
            }

            // Qty x Price and Subtotal
            val qtyStr = "${item.quantity} x ${FormatUtils.formatRupiah(item.sellingPrice)}"
            val subtotalStr = FormatUtils.formatRupiah(item.subtotal)
            drawTwoColText(canvas, qtyStr, subtotalStr, margin, contentWidth, y, textPaint, textPaint)
            y += lineHeight

            // Item discount if any
            if (item.itemDiscount > 0) {
                val discLabel = "  Diskon Item"
                val discVal = "-${FormatUtils.formatRupiah(item.itemDiscount)}"
                drawTwoColText(canvas, discLabel, discVal, margin, contentWidth, y, discountPaint, discountPaint)
                y += lineHeight
            }

            y += 2f
        }

        y += 4f
        canvas.drawLine(margin, y, width - margin, y, dividerPaint)
        y += 12f

        // 4. Calculations
        drawTwoColText(canvas, "Subtotal", FormatUtils.formatRupiah(trx.subtotal), margin, contentWidth, y, textPaint, textPaint)
        y += lineHeight

        if (trx.discountAmount > 0) {
            drawTwoColText(canvas, "Diskon Transaksi", "-${FormatUtils.formatRupiah(trx.discountAmount)}", margin, contentWidth, y, discountPaint, discountPaint)
            y += lineHeight
        }

        if (trx.taxAmount > 0) {
            val taxLabel = "PPN (${storeSettings?.taxRate ?: 0}%)"
            drawTwoColText(canvas, taxLabel, FormatUtils.formatRupiah(trx.taxAmount), margin, contentWidth, y, textPaint, textPaint)
            y += lineHeight
        }

        y += 2f
        canvas.drawLine(margin, y, width - margin, y, solidDividerPaint)
        y += 14f

        // Total Row
        drawTwoColText(canvas, "TOTAL", FormatUtils.formatRupiah(trx.totalAmount), margin, contentWidth, y, totalPaint, totalPaint)
        y += totalPaint.textSize + 6f

        if (trx.paymentMethod == "TUNAI") {
            drawTwoColText(canvas, "Bayar Tunai", FormatUtils.formatRupiah(trx.cashReceived), margin, contentWidth, y, textPaint, textPaint)
            y += lineHeight
            drawTwoColText(canvas, "Kembalian", FormatUtils.formatRupiah(trx.cashChange), margin, contentWidth, y, boldPaint, boldPaint)
            y += lineHeight
        }

        y += 6f
        canvas.drawLine(margin, y, width - margin, y, dividerPaint)
        y += 14f

        // 5. Footer Note
        val footer = storeSettings?.receiptFooter ?: "Terima Kasih Atas Kunjungan Anda!"
        val footerLines = wrapText(footer, contentWidth, headerPaint)
        for (fLine in footerLines) {
            canvas.drawText(fLine, width / 2f, y, headerPaint)
            y += headerPaint.textSize + 3f
        }

        y += 8f

        // Draw Simulated Code-128 Barcode Pattern for POS Scanners
        drawSimulatedBarcode(canvas, trx.transactionNumber, width / 2f, y, (contentWidth * 0.75f).coerceAtMost(160f), 24f)
        y += 28f

        val barcodeTextPaint = Paint().apply {
            color = Color.GRAY
            textSize = 7f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("* ${trx.transactionNumber} *", width / 2f, y, barcodeTextPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        return try {
            val receiptsDir = File(context.cacheDir, "receipts").apply { if (!exists()) mkdirs() }
            val cleanTxNum = trx.transactionNumber.replace("/", "_").replace(" ", "_")
            val pdfFile = File(receiptsDir, "Struk_${cleanTxNum}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Helper to draw two-column text (left-aligned and right-aligned).
     */
    private fun drawTwoColText(
        canvas: Canvas,
        left: String,
        right: String,
        x: Float,
        width: Float,
        y: Float,
        leftPaint: Paint,
        rightPaint: Paint
    ) {
        canvas.drawText(left, x, y, leftPaint)
        val rightWidth = rightPaint.measureText(right)
        canvas.drawText(right, x + width - rightWidth, y, rightPaint)
    }

    /**
     * Splits long text into multiple lines that fit within maxWidth.
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return if (lines.isEmpty()) listOf(text) else lines
    }

    /**
     * Draws a crisp visual barcode pattern for thermal paper.
     */
    private fun drawSimulatedBarcode(
        canvas: Canvas,
        code: String,
        centerX: Float,
        topY: Float,
        width: Float,
        height: Float
    ) {
        val startX = centerX - (width / 2f)
        val barPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        // Generate pseudo-code128 bars from hash of the transaction string
        val bitString = buildString {
            append("11010010000") // start code
            code.forEach { ch ->
                val charVal = ch.code
                val pattern = (charVal * 31 % 1024).toString(2).padStart(10, '0')
                append(pattern)
            }
            append("1100011101011") // stop code
        }

        val totalBars = bitString.length
        val barStep = width / totalBars

        for (i in bitString.indices) {
            if (bitString[i] == '1') {
                val bx = startX + (i * barStep)
                canvas.drawLine(bx, topY, bx, topY + height, barPaint)
            }
        }
    }

    /**
     * Triggers Android PrintManager to send the receipt directly to Thermal Bluetooth Printer drivers,
     * Network Printers, or Android Print Spooler (which allows Save as PDF or direct Bluetooth output).
     */
    fun printReceipt(
        context: Context,
        transactionWithItems: TransactionWithItems,
        storeSettings: StoreSettingsEntity?,
        paperSize: ReceiptPaperSize = ReceiptPaperSize.THERMAL_58MM,
        customStoreName: String? = null,
        customAddress: String? = null,
        customTextLogo: String? = null
    ) {
        val pdfFile = generateReceiptPdf(
            context = context,
            transactionWithItems = transactionWithItems,
            storeSettings = storeSettings,
            paperSize = paperSize,
            customStoreName = customStoreName,
            customAddress = customAddress,
            customTextLogo = customTextLogo
        )
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Gagal membuat dokumen struk cetak", Toast.LENGTH_SHORT).show()
            return
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Layanan cetak tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Struk_${transactionWithItems.transaction.transactionNumber}"

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }

                var input: FileInputStream? = null
                var output: FileOutputStream? = null

                try {
                    input = FileInputStream(pdfFile)
                    output = FileOutputStream(destination?.fileDescriptor)

                    val buf = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } > 0) {
                        output.write(buf, 0, bytesRead)
                    }

                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    try {
                        input?.close()
                        output?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMediaSize(
                when (paperSize) {
                    ReceiptPaperSize.THERMAL_58MM -> PrintAttributes.MediaSize("THERMAL_58", "Thermal 58mm", 2280, 5000)
                    ReceiptPaperSize.THERMAL_80MM -> PrintAttributes.MediaSize("THERMAL_80", "Thermal 80mm", 3150, 7000)
                    ReceiptPaperSize.STANDARD_A4 -> PrintAttributes.MediaSize.ISO_A4
                }
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Shares the formatted PDF receipt via Android Chooser (Bluetooth, WhatsApp, Email, or Bluetooth Printer App).
     */
    fun shareReceiptPdf(
        context: Context,
        transactionWithItems: TransactionWithItems,
        storeSettings: StoreSettingsEntity?,
        paperSize: ReceiptPaperSize = ReceiptPaperSize.THERMAL_58MM,
        customStoreName: String? = null,
        customAddress: String? = null,
        customTextLogo: String? = null
    ) {
        val pdfFile = generateReceiptPdf(
            context = context,
            transactionWithItems = transactionWithItems,
            storeSettings = storeSettings,
            paperSize = paperSize,
            customStoreName = customStoreName,
            customAddress = customAddress,
            customTextLogo = customTextLogo
        )
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Gagal membuat berkas PDF struk", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Struk Transaksi ${transactionWithItems.transaction.transactionNumber}")
                putExtra(Intent.EXTRA_TEXT, "Berikut adalah berkas PDF Struk Pembayaran untuk transaksi ${transactionWithItems.transaction.transactionNumber}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan / Cetak Struk PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan struk PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the generated PDF in an external PDF viewer.
     */
    fun openReceiptPdf(
        context: Context,
        transactionWithItems: TransactionWithItems,
        storeSettings: StoreSettingsEntity?,
        paperSize: ReceiptPaperSize = ReceiptPaperSize.THERMAL_58MM,
        customStoreName: String? = null,
        customAddress: String? = null,
        customTextLogo: String? = null
    ) {
        val pdfFile = generateReceiptPdf(
            context = context,
            transactionWithItems = transactionWithItems,
            storeSettings = storeSettings,
            paperSize = paperSize,
            customStoreName = customStoreName,
            customAddress = customAddress,
            customTextLogo = customTextLogo
        )
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Gagal membuat berkas PDF struk", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}

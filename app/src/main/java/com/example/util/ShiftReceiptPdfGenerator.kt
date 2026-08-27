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
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.StoreSettingsEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object ShiftReceiptPdfGenerator {

    fun generateShiftPdf(
        context: Context,
        shift: CashierShiftEntity,
        storeSettings: StoreSettingsEntity?,
        paperSize: ReceiptPaperSize = ReceiptPaperSize.THERMAL_58MM
    ): File? {
        val width = paperSize.widthPt
        val margin = when (paperSize) {
            ReceiptPaperSize.THERMAL_58MM -> 12f
            ReceiptPaperSize.THERMAL_80MM -> 18f
            ReceiptPaperSize.STANDARD_A4 -> 36f
        }
        val contentWidth = width - (margin * 2)

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

        val boldPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
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

        val centerPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 2f), 0f)
        }

        val lineHeight = textPaint.textSize * 1.45f
        val calculatedHeight = when (paperSize) {
            ReceiptPaperSize.STANDARD_A4 -> 842
            else -> 600
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, calculatedHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var y = margin + 12f
        val centerX = width / 2f

        // 1. Header
        val storeName = storeSettings?.storeName?.ifBlank { "KASIR POS" } ?: "KASIR POS"
        canvas.drawText(storeName.uppercase(), centerX, y, titlePaint)
        y += lineHeight * 1.2f

        canvas.drawText("LAPORAN REKAP SHIFT KASIR", centerX, y, boldPaint.apply { textAlign = Paint.Align.CENTER })
        boldPaint.textAlign = Paint.Align.LEFT
        y += lineHeight

        // Divider
        y += 4f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineHeight

        // 2. Shift Info
        fun drawRow(label: String, value: String, isBold: Boolean = false) {
            val p = if (isBold) boldPaint else textPaint
            canvas.drawText(label, margin, y, p)
            val valWidth = p.measureText(value)
            canvas.drawText(value, width - margin - valWidth, y, p)
            y += lineHeight
        }

        drawRow("No. Shift:", shift.shiftNumber)
        drawRow("Kasir:", shift.cashierName)
        drawRow("Status:", if (shift.status == "OPEN") "AKTIF (OPEN)" else "SELESAI (CLOSED)", true)
        drawRow("Buka Shift:", FormatUtils.formatDate(shift.startTime))
        if (shift.endTime != null) {
            drawRow("Tutup Shift:", FormatUtils.formatDate(shift.endTime))
        }

        y += 4f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineHeight

        // 3. Financial Reconciliation Breakdown
        drawRow("Saldo Kas Awal (Modal):", FormatUtils.formatRupiah(shift.startCash))
        drawRow("Penjualan Tunai (+):", FormatUtils.formatRupiah(shift.cashSales))
        drawRow("Kas Masuk Tambahan (+):", FormatUtils.formatRupiah(shift.cashIn))
        drawRow("Kas Keluar / Beban (-):", FormatUtils.formatRupiah(shift.cashOut))

        y += 4f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineHeight

        drawRow("Total Kas Diharapkan:", FormatUtils.formatRupiah(shift.expectedCash), true)

        if (shift.actualCash != null) {
            drawRow("Uang Fisik Dihitung:", FormatUtils.formatRupiah(shift.actualCash), true)
            val diff = shift.cashDifference ?: 0.0
            val diffText = when {
                diff == 0.0 -> "Rp 0 (Cocok / Pas)"
                diff > 0 -> "+ ${FormatUtils.formatRupiah(diff)} (Surplus / Lebih)"
                else -> "- ${FormatUtils.formatRupiah(Math.abs(diff))} (Defisit / Kurang)"
            }
            drawRow("Selisih Kas:", diffText, true)
        }

        y += 4f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineHeight

        // 4. Non-Cash & Other Stats
        drawRow("Total Non-Tunai (QRIS/Debit):", FormatUtils.formatRupiah(shift.nonCashSales))
        drawRow("Total Diskon Diberikan:", FormatUtils.formatRupiah(shift.totalDiscount))
        drawRow("Total Transaksi:", "${shift.transactionCount} transaksi")

        if (shift.notes.isNotBlank()) {
            y += 4f
            drawRow("Catatan:", shift.notes)
        }

        y += 8f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineHeight * 1.5f

        canvas.drawText("Dicetak pada: ${FormatUtils.formatDate(System.currentTimeMillis())}", centerX, y, centerPaint)
        y += lineHeight
        canvas.drawText("POS Kasir Pintar Offline", centerX, y, centerPaint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "shift_receipts")
        if (!outputDir.exists()) outputDir.mkdirs()

        val file = File(outputDir, "Shift_${shift.shiftNumber}_${paperSize.name}.pdf")
        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.flush()
            fos.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun printShiftPdf(context: Context, file: File, jobName: String = "Struk Rekap Shift POS") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Layanan cetak tidak tersedia di perangkat ini.", Toast.LENGTH_SHORT).show()
            return
        }

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
                val info = PrintDocumentInfo.Builder(file.name)
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
                var input: FileInputStream? = null
                var output: FileOutputStream? = null
                try {
                    input = FileInputStream(file)
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
                    } catch (_: Exception) {}
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.JPN_YOU4)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    fun shareShiftPdf(context: Context, file: File, shiftNumber: String) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Rekap Shift $shiftNumber")
                putExtra(Intent.EXTRA_TEXT, "Berikut adalah laporan rekap shift kasir $shiftNumber.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Rekap Shift").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan struk shift: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportUtils {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID"))
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Escape special characters for standard CSV compliance (RFC 4180)
     */
    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = value.toString()
        // If string contains comma, quote, or newline, wrap in quotes and escape internal quotes
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"${str.replace("\"", "\"\"")}\""
        } else {
            "\"$str\""
        }
    }

    /**
     * Builds standard CSV content for transaction records and line items.
     */
    fun buildTransactionsCsv(
        transactions: List<TransactionWithItems>,
        storeName: String? = null
    ): String {
        val sb = StringBuilder()

        // Optional metadata header
        if (!storeName.isNullOrBlank()) {
            sb.append("# Toko: ").append(storeName).append("\n")
            sb.append("# Diekspor Pada: ").append(dateTimeFormat.format(Date())).append("\n")
            sb.append("# Total Transaksi: ").append(transactions.size).append("\n\n")
        }

        // CSV Header Columns
        val headers = listOf(
            "No Transaksi",
            "Tanggal & Waktu",
            "Status",
            "Kasir",
            "Pelanggan",
            "Metode Pembayaran",
            "Jumlah Item",
            "Subtotal (Rp)",
            "Diskon (Rp)",
            "Pajak (Rp)",
            "Total Akhir (Rp)",
            "Uang Diterima (Rp)",
            "Kembalian (Rp)",
            "Catatan / Alasan Void",
            "Rincian Produk"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        // Rows
        for (trxWithItems in transactions) {
            val trx = trxWithItems.transaction
            val itemsFormatted = trxWithItems.items.joinToString("; ") { item ->
                "${item.quantity}x ${item.productName} (@Rp ${FormatUtils.formatNumber(item.sellingPrice)})"
            }

            val rowValues = listOf(
                trx.transactionNumber,
                dateTimeFormat.format(Date(trx.timestamp)),
                trx.status,
                trx.cashierName,
                if (trx.customerName.isNullOrBlank()) "-" else trx.customerName,
                trx.paymentMethod,
                trxWithItems.items.sumOf { it.quantity },
                trx.subtotal,
                trx.discountAmount,
                trx.taxAmount,
                trx.totalAmount,
                trx.cashReceived ?: trx.totalAmount,
                trx.cashChange ?: 0.0,
                if (trx.status == "VOIDED") "VOID: ${trx.voidReason}" else (if (trx.note.isBlank()) "-" else trx.note),
                itemsFormatted
            )

            sb.append(rowValues.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        return sb.toString()
    }

    /**
     * Builds summary report CSV containing aggregate metrics and top products.
     */
    fun buildSalesSummaryCsv(
        periodName: String,
        transactions: List<TransactionWithItems>,
        totalRevenue: Double,
        totalProfit: Double,
        topProducts: List<TopProductSummary>,
        storeName: String? = null
    ): String {
        val sb = StringBuilder()
        val completed = transactions.filter { it.transaction.status == "COMPLETED" }
        val totalItemsSold = completed.sumOf { tx -> tx.items.sumOf { it.quantity } }

        sb.append(escapeCsv("RINGKASAN LAPORAN PENJUALAN POS")).append("\n")
        if (!storeName.isNullOrBlank()) {
            sb.append(escapeCsv("Toko")).append(",").append(escapeCsv(storeName)).append("\n")
        }
        sb.append(escapeCsv("Periode")).append(",").append(escapeCsv(periodName)).append("\n")
        sb.append(escapeCsv("Waktu Ekspor")).append(",").append(escapeCsv(dateTimeFormat.format(Date()))).append("\n")
        sb.append(escapeCsv("Total Omset")).append(",").append(escapeCsv(totalRevenue)).append("\n")
        sb.append(escapeCsv("Estimasi Laba Bersih")).append(",").append(escapeCsv(totalProfit)).append("\n")
        sb.append(escapeCsv("Total Transaksi Sukses")).append(",").append(escapeCsv(completed.size)).append("\n")
        sb.append(escapeCsv("Total Produk Terjual (Unit)")).append(",").append(escapeCsv(totalItemsSold)).append("\n\n")

        if (topProducts.isNotEmpty()) {
            sb.append(escapeCsv("PRODUK TERLARIS (TOP SELLING)")).append("\n")
            sb.append(listOf("Peringkat", "Nama Produk", "Kategori", "Jumlah Terjual (Unit)", "Total Pendapatan (Rp)").joinToString(",") { escapeCsv(it) }).append("\n")
            topProducts.forEachIndexed { index, item ->
                sb.append(listOf(
                    index + 1,
                    item.productName,
                    item.category,
                    item.totalSold,
                    item.totalRevenue
                ).joinToString(",") { escapeCsv(it) }).append("\n")
            }
            sb.append("\n")
        }

        sb.append(escapeCsv("DETAIL RIWAYAT TRANSAKSI")).append("\n")
        sb.append(buildTransactionsCsv(transactions, null))

        return sb.toString()
    }

    /**
     * Exports transactions list to a CSV file in the app cache and invokes Android Share Intent.
     */
    fun exportAndShareTransactionsCsv(
        context: Context,
        transactions: List<TransactionWithItems>,
        storeName: String? = null,
        fileNamePrefix: String = "Laporan_Transaksi_POS"
    ): File? {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "Tidak ada data transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val csvContent = buildTransactionsCsv(transactions, storeName)
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val fileName = "${fileNamePrefix}_${fileTimestampFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                // Write UTF-8 BOM so Excel opens indonesian/accents characters correctly
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor CSV: $fileName")
                putExtra(Intent.EXTRA_TEXT, "Berikut terlampir file ekspor riwayat transaksi POS (${transactions.size} transaksi).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Simpan / Bagikan Berkas CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Toast.makeText(context, "Berkas CSV berhasil dibuat (${file.name})", Toast.LENGTH_SHORT).show()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengekspor CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    /**
     * Exports full sales report (summary + transactions) to CSV and shares.
     */
    fun exportAndShareSalesReportCsv(
        context: Context,
        periodName: String,
        transactions: List<TransactionWithItems>,
        totalRevenue: Double,
        totalProfit: Double,
        topProducts: List<TopProductSummary>,
        storeName: String? = null
    ): File? {
        try {
            val csvContent = buildSalesSummaryCsv(
                periodName = periodName,
                transactions = transactions,
                totalRevenue = totalRevenue,
                totalProfit = totalProfit,
                topProducts = topProducts,
                storeName = storeName
            )
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val cleanPeriod = periodName.replace(" ", "_").replace("/", "-")
            val fileName = "Laporan_Penjualan_${cleanPeriod}_${fileTimestampFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                // Write UTF-8 BOM
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan POS - $periodName")
                putExtra(Intent.EXTRA_TEXT, "Berikut berkas CSV Laporan Penjualan POS periode $periodName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan / Simpan Laporan CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Toast.makeText(context, "Laporan CSV siap dibagikan", Toast.LENGTH_SHORT).show()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat laporan CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}

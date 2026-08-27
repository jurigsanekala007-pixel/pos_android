package com.example.util

import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingsEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val indonesianLocale = Locale("id", "ID")
    private val currencyFormat = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatRupiah(amount: Double): String {
        return try {
            val formatted = currencyFormat.format(amount)
            // Clean standard "Rp" prefix spacing
            if (!formatted.startsWith("Rp")) {
                "Rp " + NumberFormat.getNumberInstance(indonesianLocale).format(amount.toLong())
            } else {
                formatted.replace("Rp", "Rp ")
            }
        } catch (e: Exception) {
            "Rp ${amount.toLong()}"
        }
    }

    fun formatNumber(number: Number): String {
        return NumberFormat.getNumberInstance(indonesianLocale).format(number)
    }

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy, HH:mm"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, indonesianLocale)
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatDateOnly(timestamp: Long): String {
        return formatDate(timestamp, "dd MMMM yyyy")
    }

    fun formatTimeOnly(timestamp: Long): String {
        return formatDate(timestamp, "HH:mm")
    }

    fun getStartAndEndOfDay(calendar: Calendar = Calendar.getInstance()): Pair<Long, Long> {
        val startCal = calendar.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = calendar.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun generateShareableReceipt(
        trxWithItems: TransactionWithItems,
        storeSettings: StoreSettingsEntity?,
        customStoreName: String? = null,
        customAddress: String? = null,
        customTextLogo: String? = null
    ): String {
        val trx = trxWithItems.transaction
        val storeName = if (!customStoreName.isNullOrBlank()) customStoreName.trim() else storeSettings?.storeName ?: "Kasir POS"
        val address = if (!customAddress.isNullOrBlank()) customAddress.trim() else storeSettings?.address ?: ""
        val phone = storeSettings?.phone ?: ""
        val footer = storeSettings?.receiptFooter ?: "Terima Kasih!"

        val sb = StringBuilder()
        sb.append("================================\n")
        if (!customTextLogo.isNullOrBlank()) {
            sb.append("     ${customTextLogo.trim()}\n")
        }
        sb.append("        $storeName\n")
        if (address.isNotEmpty()) sb.append("   $address\n")
        if (phone.isNotEmpty()) sb.append("       Telp: $phone\n")
        sb.append("================================\n")
        sb.append("No. Trx : ${trx.transactionNumber}\n")
        sb.append("Waktu   : ${formatDate(trx.timestamp)}\n")
        sb.append("Kasir   : ${trx.cashierName}\n")
        sb.append("Customer: ${trx.customerName}\n")
        sb.append("Metode  : ${trx.paymentMethod}\n")
        sb.append("--------------------------------\n")

        for (item in trxWithItems.items) {
            sb.append("${item.productName}\n")
            val qtyStr = "${item.quantity} x ${formatRupiah(item.sellingPrice)}"
            val subtotalStr = formatRupiah(item.subtotal)
            sb.append(String.format("%-20s %11s\n", qtyStr, subtotalStr))
            if (item.itemDiscount > 0) {
                sb.append(String.format("%-20s %11s\n", "  Diskon Item", "-${formatRupiah(item.itemDiscount)}"))
            }
        }

        sb.append("--------------------------------\n")
        sb.append(String.format("%-18s %13s\n", "Subtotal", formatRupiah(trx.subtotal)))
        if (trx.discountAmount > 0) {
            sb.append(String.format("%-18s %13s\n", "Diskon Total", "-${formatRupiah(trx.discountAmount)}"))
        }
        if (trx.taxAmount > 0) {
            sb.append(String.format("%-18s %13s\n", "Pajak (PPN)", formatRupiah(trx.taxAmount)))
        }
        sb.append(String.format("%-18s %13s\n", "TOTAL", formatRupiah(trx.totalAmount)))
        
        if (trx.paymentMethod == "TUNAI") {
            sb.append(String.format("%-18s %13s\n", "Bayar Tunai", formatRupiah(trx.cashReceived)))
            sb.append(String.format("%-18s %13s\n", "Kembalian", formatRupiah(trx.cashChange)))
        }

        sb.append("================================\n")
        sb.append("   $footer\n")
        sb.append("================================\n")

        return sb.toString()
    }
}

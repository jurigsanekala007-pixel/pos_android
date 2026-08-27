package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashier_shifts")
data class CashierShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftNumber: String,
    val cashierName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val startCash: Double = 0.0, // Saldo kas awal di laci
    val cashSales: Double = 0.0, // Total penjualan tunai selama shift
    val nonCashSales: Double = 0.0, // Total penjualan QRIS, Debit, Transfer, dll
    val totalDiscount: Double = 0.0, // Total diskon yang diberikan
    val transactionCount: Int = 0, // Jumlah transaksi
    val cashIn: Double = 0.0, // Uang kas masuk tambahan (modal tambahan, dll)
    val cashOut: Double = 0.0, // Uang kas keluar (biaya operasional, dll)
    val expectedCash: Double = 0.0, // startCash + cashSales + cashIn - cashOut
    val actualCash: Double? = null, // Uang fisik dihitung saat tutup shift
    val cashDifference: Double? = null, // actualCash - expectedCash (Lebih / Kurang / Cocok)
    val status: String = "OPEN", // "OPEN", "CLOSED"
    val notes: String = ""
)

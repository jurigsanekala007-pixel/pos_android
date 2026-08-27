package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String, // e.g. TRX-20260826-0001
    val customerName: String = "Pelanggan Umum",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val totalProfit: Double, // Realized Gross Profit (Selling Price - Cost Price) - Discount
    val paymentMethod: String = "TUNAI", // TUNAI, QRIS, DEBIT, TRANSFER, PIUTANG
    val cashReceived: Double = 0.0,
    val cashChange: Double = 0.0,
    val note: String = "",
    val cashierName: String = "Kasir",
    val status: String = "COMPLETED", // COMPLETED, VOIDED
    val voidReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

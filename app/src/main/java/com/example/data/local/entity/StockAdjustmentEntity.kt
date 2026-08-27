package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_adjustments")
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val changeQuantity: Int, // e.g. +10, -5
    val previousStock: Int,
    val newStock: Int,
    val reason: String, // "Restock Barang Masuk", "Penyesuaian Fisik (Opname)", "Barang Rusak/Hilang", "Kadaluarsa", "Retur Penjualan", "Transaksi Kasir"
    val note: String = "",
    val referenceId: String = "", // e.g. transaction number
    val timestamp: Long = System.currentTimeMillis()
)

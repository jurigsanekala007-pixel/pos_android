package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "Toko Berkah Sejahtera",
    val tagline: String = "Solusi Belanja & Kuliner Terbaik",
    val address: String = "Jl. Merdeka No. 88, Jakarta Pusat",
    val phone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima Kasih Telah Berbelanja!\nBarang yang sudah dibeli tidak dapat ditukar.",
    val taxRate: Double = 0.0, // e.g. 11.0%
    val taxEnabled: Boolean = false,
    val cashierName: String = "Admin Kasir",
    val currencySymbol: String = "Rp"
)

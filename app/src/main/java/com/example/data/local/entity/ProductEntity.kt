package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String = "",
    val category: String = "Umum",
    val costPrice: Double, // Harga Beli / Modal
    val sellingPrice: Double, // Harga Jual
    val stock: Int = 0,
    val minStockAlert: Int = 5,
    val unit: String = "pcs", // pcs, cup, porsi, botol, kg, dll
    val colorHex: String = "#00685F",
    val iconName: String = "store",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

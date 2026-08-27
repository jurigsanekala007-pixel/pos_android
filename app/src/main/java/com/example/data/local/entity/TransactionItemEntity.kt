package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["productId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val category: String = "Umum",
    val costPrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val unit: String = "pcs",
    val subtotal: Double, // sellingPrice * quantity
    val itemDiscount: Double = 0.0,
    val profit: Double // (sellingPrice - costPrice) * quantity - itemDiscount
)

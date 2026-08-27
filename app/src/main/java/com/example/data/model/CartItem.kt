package com.example.data.model

import com.example.data.local.entity.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val itemDiscount: Double = 0.0,
    val note: String = ""
) {
    val subtotal: Double
        get() = product.sellingPrice * quantity

    val finalPrice: Double
        get() = (subtotal - itemDiscount).coerceAtLeast(0.0)

    val profit: Double
        get() = ((product.sellingPrice - product.costPrice) * quantity) - itemDiscount
}

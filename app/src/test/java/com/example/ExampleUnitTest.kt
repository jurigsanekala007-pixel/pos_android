package com.example

import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.model.CartItem
import com.example.ui.viewmodel.PosUiState
import com.example.util.CsvExportUtils
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCsvTransactionsExportBuilder() {
    val trx = TransactionEntity(
      id = 101L,
      transactionNumber = "TRX-2026-0001",
      timestamp = 1700000000000L,
      cashierName = "Kasir 1",
      customerName = "Pelanggan VIP",
      paymentMethod = "CASH",
      subtotal = 50000.0,
      discountAmount = 5000.0,
      taxAmount = 4500.0,
      totalAmount = 49500.0,
      totalProfit = 34000.0,
      cashReceived = 50000.0,
      cashChange = 500.0,
      status = "COMPLETED",
      note = "Pesanan dibungkus"
    )

    val item = TransactionItemEntity(
      id = 1L,
      transactionId = 101L,
      productId = 1L,
      productName = "Kopi Susu Gula Aren",
      category = "Minuman",
      costPrice = 8000.0,
      sellingPrice = 25000.0,
      quantity = 2,
      unit = "cup",
      itemDiscount = 0.0,
      subtotal = 50000.0,
      profit = 34000.0
    )

    val trxWithItems = TransactionWithItems(
      transaction = trx,
      items = listOf(item)
    )

    val csv = CsvExportUtils.buildTransactionsCsv(
      transactions = listOf(trxWithItems),
      storeName = "Kopi Bahagia"
    )

    assertTrue(csv.contains("TRX-2026-0001"))
    assertTrue(csv.contains("Kopi Susu Gula Aren"))
    assertTrue(csv.contains("COMPLETED"))
    assertTrue(csv.contains("Pesanan dibungkus"))
    assertTrue(csv.contains("Kopi Bahagia"))
  }

  @Test
  fun testCsvSalesSummaryBuilder() {
    val topProduct = TopProductSummary(
      productId = 10L,
      productName = "Roti Cokelat",
      category = "Makanan",
      totalSold = 25,
      totalRevenue = 175000.0,
      totalProfit = 75000.0
    )

    val csv = CsvExportUtils.buildSalesSummaryCsv(
      periodName = "Bulan Ini",
      transactions = emptyList(),
      totalRevenue = 500000.0,
      totalProfit = 220000.0,
      topProducts = listOf(topProduct),
      storeName = "Toko Berkah"
    )

    assertTrue(csv.contains("RINGKASAN LAPORAN PENJUALAN POS"))
    assertTrue(csv.contains("Toko Berkah"))
    assertTrue(csv.contains("Bulan Ini"))
    assertTrue(csv.contains("Roti Cokelat"))
    assertTrue(csv.contains("175000"))
  }

  @Test
  fun testPosFormatting() {
    val rupiah = FormatUtils.formatRupiah(150000.0)
    assertTrue(rupiah.contains("150.000") || rupiah.contains("150,000"))
    assertEquals("0", FormatUtils.formatNumber(0.0))
  }

  @Test
  fun testLowStockThresholdFiltering() {
    val products = listOf(
      ProductEntity(id = 1, name = "Kopi Susu", costPrice = 5000.0, sellingPrice = 15000.0, stock = 2, minStockAlert = 5),
      ProductEntity(id = 2, name = "Teh Manis", costPrice = 2000.0, sellingPrice = 8000.0, stock = 0, minStockAlert = 5),
      ProductEntity(id = 3, name = "Croissant", costPrice = 10000.0, sellingPrice = 22000.0, stock = 4, minStockAlert = 5),
      ProductEntity(id = 4, name = "Air Mineral", costPrice = 2000.0, sellingPrice = 5000.0, stock = 25, minStockAlert = 5)
    )

    val lowStock = products.filter { it.isActive && (it.stock < 5 || it.stock <= it.minStockAlert) }
    assertEquals(3, lowStock.size)

    val outOfStock = lowStock.filter { it.stock <= 0 }
    assertEquals(1, outOfStock.size)
    assertEquals("Teh Manis", outOfStock.first().name)

    val underThreshold = lowStock.filter { it.stock in 1 until 5 }
    assertEquals(2, underThreshold.size)
  }

  @Test
  fun testReceiptCustomization() {
    val trx = TransactionEntity(
      id = 202L,
      transactionNumber = "TRX-2026-9999",
      timestamp = 1700000000000L,
      cashierName = "Kasir 1",
      customerName = "Budi",
      paymentMethod = "TUNAI",
      subtotal = 30000.0,
      discountAmount = 0.0,
      taxAmount = 0.0,
      totalAmount = 30000.0,
      totalProfit = 15000.0,
      cashReceived = 50000.0,
      cashChange = 20000.0,
      status = "COMPLETED"
    )
    val item = TransactionItemEntity(
      id = 2L,
      transactionId = 202L,
      productId = 1L,
      productName = "Espresso Single",
      category = "Minuman",
      costPrice = 5000.0,
      sellingPrice = 15000.0,
      quantity = 2,
      unit = "cup",
      itemDiscount = 0.0,
      subtotal = 30000.0,
      profit = 15000.0
    )
    val trxWithItems = TransactionWithItems(transaction = trx, items = listOf(item))

    val receipt = FormatUtils.generateShareableReceipt(
      trxWithItems = trxWithItems,
      storeSettings = null,
      customStoreName = "Kopi Senja Nusantara",
      customAddress = "Jl. Danau Toba No. 12",
      customTextLogo = "☕ ★ SENJA COFFEE ★ ☕"
    )

    assertTrue(receipt.contains("Kopi Senja Nusantara"))
    assertTrue(receipt.contains("Jl. Danau Toba No. 12"))
    assertTrue(receipt.contains("☕ ★ SENJA COFFEE ★ ☕"))
    assertTrue(receipt.contains("Espresso Single"))
  }
}



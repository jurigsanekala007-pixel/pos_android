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
      sku = "KOP-001",
      costPrice = 8000.0,
      sellingPrice = 25000.0,
      quantity = 2,
      discount = 0.0,
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
      productName = "Roti Cokelat",
      category = "Makanan",
      totalSold = 25,
      totalRevenue = 175000.0
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
}


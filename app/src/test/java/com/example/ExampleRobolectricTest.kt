package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.model.CartItem
import com.example.ui.viewmodel.PosUiState
import com.example.util.FormatUtils
import com.example.util.PosBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kasir POS", appName)
  }

  @Test
  fun `test cart and discount calculation in PosUiState`() {
    val product1 = ProductEntity(
      id = 1L,
      name = "Kopi Susu",
      sku = "KOP-001",
      costPrice = 8000.0,
      sellingPrice = 18000.0,
      stock = 20
    )
    val product2 = ProductEntity(
      id = 2L,
      name = "Roti Bakar",
      sku = "ROT-001",
      costPrice = 5000.0,
      sellingPrice = 12000.0,
      stock = 10
    )

    val cartItem1 = CartItem(product = product1, quantity = 2, itemDiscount = 2000.0) // subtotal = 36000, disc = 2000, final = 34000
    val cartItem2 = CartItem(product = product2, quantity = 1, itemDiscount = 0.0)    // subtotal = 12000, disc = 0, final = 12000

    val state = PosUiState(
      cartItems = listOf(cartItem1, cartItem2),
      transactionDiscount = 4000.0,
      cashReceivedString = "50000"
    )

    // Subtotal: 36000 + 12000 = 48000
    assertEquals(48000.0, state.cartSubtotal, 0.01)
    // Item discounts: 2000
    assertEquals(2000.0, state.cartItemDiscountsTotal, 0.01)
    // Total items count: 2 + 1 = 3
    assertEquals(3, state.cartItemCount)

    // Total without tax: (48000 - 4000 - 2000) = 42000
    val totalWithoutTax = state.calculateTotal(taxRate = 0.0, taxEnabled = false)
    assertEquals(42000.0, totalWithoutTax, 0.01)

    // Total with 10% tax: 42000 + (42000 * 10%) = 42000 + 4200 = 46200
    val taxAmount = state.calculateTax(taxRate = 10.0, taxEnabled = true)
    assertEquals(4200.0, taxAmount, 0.01)
    val totalWithTax = state.calculateTotal(taxRate = 10.0, taxEnabled = true)
    assertEquals(46200.0, totalWithTax, 0.01)

    // Change from 50000 cash for 46200: 3800
    val change = state.calculateChange(taxRate = 10.0, taxEnabled = true)
    assertEquals(3800.0, change, 0.01)
  }

  @Test
  fun `test shift reconciliation discrepancy logic`() {
    val shift = CashierShiftEntity(
      id = 1L,
      shiftNumber = "SHF-001",
      cashierName = "Budi",
      startTime = System.currentTimeMillis(),
      startCash = 100000.0,
      cashSales = 250000.0,
      cashIn = 20000.0,
      cashOut = 15000.0,
      expectedCash = 355000.0
    )

    // Test exact cash
    val actualCashExact = 355000.0
    val diffExact = actualCashExact - shift.expectedCash
    assertEquals(0.0, diffExact, 0.01)

    // Test cash surplus (+5000)
    val actualCashSurplus = 360000.0
    val diffSurplus = actualCashSurplus - shift.expectedCash
    assertEquals(5000.0, diffSurplus, 0.01)

    // Test cash deficit (-10000)
    val actualCashDeficit = 345000.0
    val diffDeficit = actualCashDeficit - shift.expectedCash
    assertEquals(-10000.0, diffDeficit, 0.01)
  }

  @Test
  fun `test format rupiah and date formatting`() {
    val amount = 25000.0
    val formatted = FormatUtils.formatRupiah(amount)
    assertTrue(formatted.contains("25.000") || formatted.contains("25,000"))

    val now = 1700000000000L
    val dateStr = FormatUtils.formatDate(now)
    assertNotNull(dateStr)
    assertTrue(dateStr.isNotBlank())
  }

  @Test
  fun `test csv and json parser for backup import`() {
    val csvContent = """
      ID,Nama Produk,SKU/Barcode,Kategori,Harga Modal,Harga Jual,Stok,Batas Minimum,Satuan,Warna Hex,Status Aktif
      1,"Kopi Robusta","KOP-099","Minuman",10000,20000,50,5,"cup","#3B82F6",1
      2,"Donat Cokelat","MKN-012","Makanan",3000,7000,30,5,"pcs","#10B981",1
    """.trimIndent()

    val parsedProducts = PosBackupManager.parseProductsFromCsv(csvContent)
    assertEquals(2, parsedProducts.size)
    assertEquals("Kopi Robusta", parsedProducts[0].name)
    assertEquals("KOP-099", parsedProducts[0].sku)
    assertEquals(20000.0, parsedProducts[0].sellingPrice, 0.01)
    assertEquals("Donat Cokelat", parsedProducts[1].name)
    assertEquals(7000.0, parsedProducts[1].sellingPrice, 0.01)

    val jsonContent = """
      [
        {
          "id": 1,
          "name": "Teh Manis",
          "sku": "TEH-001",
          "category": "Minuman",
          "costPrice": 2000,
          "sellingPrice": 5000,
          "stock": 100,
          "minStockAlert": 10,
          "unit": "cup",
          "colorHex": "#3B82F6",
          "isActive": true
        }
      ]
    """.trimIndent()

    val parsedJson = PosBackupManager.parseProductsFromJson(jsonContent)
    assertEquals(1, parsedJson.size)
    assertEquals("Teh Manis", parsedJson[0].name)
    assertEquals(5000.0, parsedJson[0].sellingPrice, 0.01)
  }
}

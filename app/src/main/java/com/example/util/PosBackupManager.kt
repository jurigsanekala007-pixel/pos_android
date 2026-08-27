package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockAdjustmentEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FullBackupData(
    val storeSettings: StoreSettingsEntity?,
    val products: List<ProductEntity>,
    val transactions: List<TransactionWithItems>,
    val shifts: List<CashierShiftEntity>,
    val adjustments: List<StockAdjustmentEntity>,
    val timestamp: Long = System.currentTimeMillis()
)

object PosBackupManager {

    private fun getTimestampStr(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    private fun getExportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ==========================================
    // 1. EXPORT PRODUCTS TO CSV & JSON
    // ==========================================

    fun exportProductsToCsv(context: Context, products: List<ProductEntity>): File? {
        return try {
            val fileName = "POS_Produk_${getTimestampStr()}.csv"
            val file = File(getExportsDir(context), fileName)
            val fos = FileOutputStream(file)
            val writer = fos.bufferedWriter()

            // Header (UTF-8 BOM for Excel compatibility)
            writer.write("\uFEFF")
            writer.write("ID,Nama Produk,SKU/Barcode,Kategori,Harga Modal,Harga Jual,Stok,Batas Minimum,Satuan,Warna Hex,Status Aktif\n")

            for (p in products) {
                val line = listOf(
                    p.id.toString(),
                    escapeCsv(p.name),
                    escapeCsv(p.sku),
                    escapeCsv(p.category),
                    p.costPrice.toInt().toString(),
                    p.sellingPrice.toInt().toString(),
                    p.stock.toString(),
                    p.minStockAlert.toString(),
                    escapeCsv(p.unit),
                    p.colorHex,
                    if (p.isActive) "1" else "0"
                ).joinToString(",")
                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportProductsToJson(context: Context, products: List<ProductEntity>): File? {
        return try {
            val fileName = "POS_Produk_${getTimestampStr()}.json"
            val file = File(getExportsDir(context), fileName)
            val jsonArray = JSONArray()

            for (p in products) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("stock", p.stock)
                    put("minStockAlert", p.minStockAlert)
                    put("unit", p.unit)
                    put("colorHex", p.colorHex)
                    put("iconName", p.iconName)
                    put("isActive", p.isActive)
                }
                jsonArray.put(obj)
            }

            file.writeText(jsonArray.toString(2))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 2. EXPORT TRANSACTIONS TO CSV & JSON
    // ==========================================

    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionWithItems>): File? {
        return try {
            val fileName = "POS_Transaksi_${getTimestampStr()}.csv"
            val file = File(getExportsDir(context), fileName)
            val writer = file.bufferedWriter()

            writer.write("\uFEFF")
            writer.write("No. Transaksi,Waktu,Kasir,Pelanggan,Metode Bayar,Subtotal,Diskon,PPN,Total,Bayar,Kembalian,Status,Item Terjual\n")

            for (t in transactions) {
                val trx = t.transaction
                val itemsSummary = t.items.joinToString("; ") { "${it.productName} (${it.quantity} ${it.unit} x ${it.sellingPrice.toInt()})" }
                val line = listOf(
                    escapeCsv(trx.transactionNumber),
                    escapeCsv(FormatUtils.formatDate(trx.timestamp)),
                    escapeCsv(trx.cashierName),
                    escapeCsv(trx.customerName),
                    escapeCsv(trx.paymentMethod),
                    trx.subtotal.toInt().toString(),
                    trx.discountAmount.toInt().toString(),
                    trx.taxAmount.toInt().toString(),
                    trx.totalAmount.toInt().toString(),
                    trx.cashReceived.toInt().toString(),
                    trx.cashChange.toInt().toString(),
                    escapeCsv(trx.status),
                    escapeCsv(itemsSummary)
                ).joinToString(",")
                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportTransactionsToJson(context: Context, transactions: List<TransactionWithItems>): File? {
        return try {
            val fileName = "POS_Transaksi_${getTimestampStr()}.json"
            val file = File(getExportsDir(context), fileName)
            val jsonArray = JSONArray()

            for (t in transactions) {
                val trx = t.transaction
                val trxObj = JSONObject().apply {
                    put("id", trx.id)
                    put("transactionNumber", trx.transactionNumber)
                    put("timestamp", trx.timestamp)
                    put("dateFormatted", FormatUtils.formatDate(trx.timestamp))
                    put("customerName", trx.customerName)
                    put("cashierName", trx.cashierName)
                    put("paymentMethod", trx.paymentMethod)
                    put("subtotal", trx.subtotal)
                    put("discountAmount", trx.discountAmount)
                    put("taxAmount", trx.taxAmount)
                    put("totalAmount", trx.totalAmount)
                    put("totalProfit", trx.totalProfit)
                    put("cashReceived", trx.cashReceived)
                    put("cashChange", trx.cashChange)
                    put("status", trx.status)
                    put("note", trx.note)
                    put("voidReason", trx.voidReason)

                    val itemsArray = JSONArray()
                    for (item in t.items) {
                        val itemObj = JSONObject().apply {
                            put("productId", item.productId)
                            put("productName", item.productName)
                            put("category", item.category)
                            put("costPrice", item.costPrice)
                            put("sellingPrice", item.sellingPrice)
                            put("quantity", item.quantity)
                            put("unit", item.unit)
                            put("subtotal", item.subtotal)
                            put("itemDiscount", item.itemDiscount)
                            put("profit", item.profit)
                        }
                        itemsArray.put(itemObj)
                    }
                    put("items", itemsArray)
                }
                jsonArray.put(trxObj)
            }

            file.writeText(jsonArray.toString(2))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 3. FULL BACKUP TO JSON (ALL TABLES)
    // ==========================================

    fun exportFullBackupJson(
        context: Context,
        settings: StoreSettingsEntity?,
        products: List<ProductEntity>,
        transactions: List<TransactionWithItems>,
        shifts: List<CashierShiftEntity>,
        adjustments: List<StockAdjustmentEntity>
    ): File? {
        return try {
            val fileName = "Backup_POS_Offline_${getTimestampStr()}.json"
            val file = File(getExportsDir(context), fileName)

            val rootObj = JSONObject()
            rootObj.put("app", "POS_KASIR_OFFLINE")
            rootObj.put("version", "2.0")
            rootObj.put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            rootObj.put("timestamp", System.currentTimeMillis())

            // Store Settings
            if (settings != null) {
                val setObj = JSONObject().apply {
                    put("storeName", settings.storeName)
                    put("tagline", settings.tagline)
                    put("address", settings.address)
                    put("phone", settings.phone)
                    put("receiptFooter", settings.receiptFooter)
                    put("taxRate", settings.taxRate)
                    put("taxEnabled", settings.taxEnabled)
                    put("cashierName", settings.cashierName)
                }
                rootObj.put("storeSettings", setObj)
            }

            // Products
            val prodArray = JSONArray()
            for (p in products) {
                prodArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("stock", p.stock)
                    put("minStockAlert", p.minStockAlert)
                    put("unit", p.unit)
                    put("colorHex", p.colorHex)
                    put("iconName", p.iconName)
                    put("isActive", p.isActive)
                })
            }
            rootObj.put("products", prodArray)

            // Transactions
            val trxArray = JSONArray()
            for (t in transactions) {
                val trx = t.transaction
                val trxObj = JSONObject().apply {
                    put("id", trx.id)
                    put("transactionNumber", trx.transactionNumber)
                    put("customerName", trx.customerName)
                    put("subtotal", trx.subtotal)
                    put("discountAmount", trx.discountAmount)
                    put("taxAmount", trx.taxAmount)
                    put("totalAmount", trx.totalAmount)
                    put("totalProfit", trx.totalProfit)
                    put("paymentMethod", trx.paymentMethod)
                    put("cashReceived", trx.cashReceived)
                    put("cashChange", trx.cashChange)
                    put("note", trx.note)
                    put("cashierName", trx.cashierName)
                    put("status", trx.status)
                    put("voidReason", trx.voidReason)
                    put("timestamp", trx.timestamp)

                    val itemsArray = JSONArray()
                    for (i in t.items) {
                        itemsArray.put(JSONObject().apply {
                            put("productId", i.productId)
                            put("productName", i.productName)
                            put("category", i.category)
                            put("costPrice", i.costPrice)
                            put("sellingPrice", i.sellingPrice)
                            put("quantity", i.quantity)
                            put("unit", i.unit)
                            put("subtotal", i.subtotal)
                            put("itemDiscount", i.itemDiscount)
                            put("profit", i.profit)
                        })
                    }
                    put("items", itemsArray)
                }
                trxArray.put(trxObj)
            }
            rootObj.put("transactions", trxArray)

            // Shifts
            val shiftArray = JSONArray()
            for (s in shifts) {
                shiftArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("shiftNumber", s.shiftNumber)
                    put("cashierName", s.cashierName)
                    put("startTime", s.startTime)
                    put("endTime", s.endTime ?: JSONObject.NULL)
                    put("startCash", s.startCash)
                    put("cashSales", s.cashSales)
                    put("nonCashSales", s.nonCashSales)
                    put("totalDiscount", s.totalDiscount)
                    put("transactionCount", s.transactionCount)
                    put("cashIn", s.cashIn)
                    put("cashOut", s.cashOut)
                    put("expectedCash", s.expectedCash)
                    put("actualCash", s.actualCash ?: JSONObject.NULL)
                    put("cashDifference", s.cashDifference ?: JSONObject.NULL)
                    put("status", s.status)
                    put("notes", s.notes)
                })
            }
            rootObj.put("shifts", shiftArray)

            // Stock Adjustments
            val adjArray = JSONArray()
            for (a in adjustments) {
                adjArray.put(JSONObject().apply {
                    put("id", a.id)
                    put("productId", a.productId)
                    put("productName", a.productName)
                    put("changeQuantity", a.changeQuantity)
                    put("previousStock", a.previousStock)
                    put("newStock", a.newStock)
                    put("reason", a.reason)
                    put("referenceId", a.referenceId)
                    put("note", a.note)
                    put("timestamp", a.timestamp)
                })
            }
            rootObj.put("stockAdjustments", adjArray)

            file.writeText(rootObj.toString(2))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 4. PARSERS & IMPORTERS
    // ==========================================

    fun parseAmount(raw: String): Double {
        val cleaned = raw.trim().replace("Rp", "", ignoreCase = true).replace(" ", "").trim()
        if (cleaned.isBlank()) return 0.0
        return if (cleaned.contains(".") && cleaned.contains(",")) {
            cleaned.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        } else if (cleaned.contains(",")) {
            cleaned.replace(",", ".").toDoubleOrNull() ?: 0.0
        } else {
            cleaned.toDoubleOrNull() ?: 0.0
        }
    }

    fun parseProductsFromCsv(content: String): List<ProductEntity> {
        val products = mutableListOf<ProductEntity>()
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Skip header if line 0 contains "Nama" or "SKU"
        val startIndex = if (lines[0].contains("Nama", ignoreCase = true) || lines[0].contains("SKU", ignoreCase = true) || lines[0].contains("ID")) 1 else 0

        for (i in startIndex until lines.size) {
            val line = lines[i]
            val cols = parseCsvLine(line)
            if (cols.size < 4) continue

            // Determine if column 0 is ID or Name
            val isFirstColId = cols[0].toLongOrNull() != null
            val offset = if (isFirstColId) 1 else 0

            val name = cols.getOrElse(offset) { "" }.trim()
            if (name.isBlank()) continue

            val sku = cols.getOrElse(offset + 1) { "" }.trim()
            val category = cols.getOrElse(offset + 2) { "Umum" }.trim().ifBlank { "Umum" }
            val costPrice = parseAmount(cols.getOrElse(offset + 3) { "0" })
            val sellingPrice = parseAmount(cols.getOrElse(offset + 4) { "0" })
            val stock = cols.getOrElse(offset + 5) { "0" }.trim().toIntOrNull() ?: 0
            val minStock = cols.getOrElse(offset + 6) { "5" }.trim().toIntOrNull() ?: 5
            val unit = cols.getOrElse(offset + 7) { "pcs" }.trim().ifBlank { "pcs" }
            val colorHex = cols.getOrElse(offset + 8) { "#3B82F6" }.trim().ifBlank { "#3B82F6" }

            products.add(
                ProductEntity(
                    id = 0L, // will be auto-assigned or merged by SKU
                    name = name,
                    sku = sku.ifBlank { "SKU-${System.currentTimeMillis() % 100000}" },
                    category = category,
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    stock = stock,
                    minStockAlert = minStock,
                    unit = unit,
                    colorHex = if (colorHex.startsWith("#")) colorHex else "#3B82F6"
                )
            )
        }
        return products
    }

    fun parseProductsFromJson(content: String): List<ProductEntity> {
        val products = mutableListOf<ProductEntity>()
        val jsonArray = JSONArray(content)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products.add(
                ProductEntity(
                    id = 0L,
                    name = obj.optString("name", "Produk"),
                    sku = obj.optString("sku", "SKU-${i + 1}"),
                    category = obj.optString("category", "Umum"),
                    costPrice = obj.optDouble("costPrice", 0.0),
                    sellingPrice = obj.optDouble("sellingPrice", 0.0),
                    stock = obj.optInt("stock", 0),
                    minStockAlert = obj.optInt("minStockAlert", 5),
                    unit = obj.optString("unit", "pcs"),
                    colorHex = obj.optString("colorHex", "#3B82F6"),
                    iconName = obj.optString("iconName", "inventory_2"),
                    isActive = obj.optBoolean("isActive", true)
                )
            )
        }
        return products
    }

    fun parseFullBackupJson(content: String): FullBackupData? {
        return try {
            val rootObj = JSONObject(content)

            // Settings
            val settings = if (rootObj.has("storeSettings")) {
                val sObj = rootObj.getJSONObject("storeSettings")
                StoreSettingsEntity(
                    id = 1,
                    storeName = sObj.optString("storeName", "Kasir POS"),
                    tagline = sObj.optString("tagline", ""),
                    address = sObj.optString("address", ""),
                    phone = sObj.optString("phone", ""),
                    receiptFooter = sObj.optString("receiptFooter", "Terima Kasih"),
                    taxRate = sObj.optDouble("taxRate", 0.0),
                    taxEnabled = sObj.optBoolean("taxEnabled", false),
                    cashierName = sObj.optString("cashierName", "Kasir")
                )
            } else null

            // Products
            val products = mutableListOf<ProductEntity>()
            if (rootObj.has("products")) {
                val pArr = rootObj.getJSONArray("products")
                for (i in 0 until pArr.length()) {
                    val p = pArr.getJSONObject(i)
                    products.add(
                        ProductEntity(
                            id = p.optLong("id", 0L),
                            name = p.optString("name", "Produk"),
                            sku = p.optString("sku", ""),
                            category = p.optString("category", "Umum"),
                            costPrice = p.optDouble("costPrice", 0.0),
                            sellingPrice = p.optDouble("sellingPrice", 0.0),
                            stock = p.optInt("stock", 0),
                            minStockAlert = p.optInt("minStockAlert", 5),
                            unit = p.optString("unit", "pcs"),
                            colorHex = p.optString("colorHex", "#3B82F6"),
                            iconName = p.optString("iconName", "inventory_2"),
                            isActive = p.optBoolean("isActive", true)
                        )
                    )
                }
            }

            // Transactions
            val transactions = mutableListOf<TransactionWithItems>()
            if (rootObj.has("transactions")) {
                val tArr = rootObj.getJSONArray("transactions")
                for (i in 0 until tArr.length()) {
                    val t = tArr.getJSONObject(i)
                    val trx = TransactionEntity(
                        id = t.optLong("id", 0L),
                        transactionNumber = t.optString("transactionNumber", ""),
                        customerName = t.optString("customerName", "Pelanggan"),
                        subtotal = t.optDouble("subtotal", 0.0),
                        discountAmount = t.optDouble("discountAmount", 0.0),
                        taxAmount = t.optDouble("taxAmount", 0.0),
                        totalAmount = t.optDouble("totalAmount", 0.0),
                        totalProfit = t.optDouble("totalProfit", 0.0),
                        paymentMethod = t.optString("paymentMethod", "TUNAI"),
                        cashReceived = t.optDouble("cashReceived", 0.0),
                        cashChange = t.optDouble("cashChange", 0.0),
                        note = t.optString("note", ""),
                        cashierName = t.optString("cashierName", "Kasir"),
                        status = t.optString("status", "COMPLETED"),
                        voidReason = t.optString("voidReason", ""),
                        timestamp = t.optLong("timestamp", System.currentTimeMillis())
                    )

                    val items = mutableListOf<TransactionItemEntity>()
                    if (t.has("items")) {
                        val iArr = t.getJSONArray("items")
                        for (j in 0 until iArr.length()) {
                            val itm = iArr.getJSONObject(j)
                            items.add(
                                TransactionItemEntity(
                                    transactionId = trx.id,
                                    productId = itm.optLong("productId", 0L),
                                    productName = itm.optString("productName", ""),
                                    category = itm.optString("category", "Umum"),
                                    costPrice = itm.optDouble("costPrice", 0.0),
                                    sellingPrice = itm.optDouble("sellingPrice", 0.0),
                                    quantity = itm.optInt("quantity", 1),
                                    unit = itm.optString("unit", "pcs"),
                                    subtotal = itm.optDouble("subtotal", 0.0),
                                    itemDiscount = itm.optDouble("itemDiscount", 0.0),
                                    profit = itm.optDouble("profit", 0.0)
                                )
                            )
                        }
                    }
                    transactions.add(TransactionWithItems(transaction = trx, items = items))
                }
            }

            // Shifts
            val shifts = mutableListOf<CashierShiftEntity>()
            if (rootObj.has("shifts")) {
                val sArr = rootObj.getJSONArray("shifts")
                for (i in 0 until sArr.length()) {
                    val s = sArr.getJSONObject(i)
                    shifts.add(
                        CashierShiftEntity(
                            id = s.optLong("id", 0L),
                            shiftNumber = s.optString("shiftNumber", "SHIFT-$i"),
                            cashierName = s.optString("cashierName", "Kasir"),
                            startTime = s.optLong("startTime", System.currentTimeMillis()),
                            endTime = if (s.isNull("endTime")) null else s.optLong("endTime"),
                            startCash = s.optDouble("startCash", 0.0),
                            cashSales = s.optDouble("cashSales", 0.0),
                            nonCashSales = s.optDouble("nonCashSales", 0.0),
                            totalDiscount = s.optDouble("totalDiscount", 0.0),
                            transactionCount = s.optInt("transactionCount", 0),
                            cashIn = s.optDouble("cashIn", 0.0),
                            cashOut = s.optDouble("cashOut", 0.0),
                            expectedCash = s.optDouble("expectedCash", 0.0),
                            actualCash = if (s.isNull("actualCash")) null else s.optDouble("actualCash"),
                            cashDifference = if (s.isNull("cashDifference")) null else s.optDouble("cashDifference"),
                            status = s.optString("status", "CLOSED"),
                            notes = s.optString("notes", "")
                        )
                    )
                }
            }

            // Adjustments
            val adjustments = mutableListOf<StockAdjustmentEntity>()
            if (rootObj.has("stockAdjustments")) {
                val aArr = rootObj.getJSONArray("stockAdjustments")
                for (i in 0 until aArr.length()) {
                    val a = aArr.getJSONObject(i)
                    adjustments.add(
                        StockAdjustmentEntity(
                            id = a.optLong("id", 0L),
                            productId = a.optLong("productId", 0L),
                            productName = a.optString("productName", ""),
                            changeQuantity = a.optInt("changeQuantity", 0),
                            previousStock = a.optInt("previousStock", 0),
                            newStock = a.optInt("newStock", 0),
                            reason = a.optString("reason", ""),
                            referenceId = a.optString("referenceId", ""),
                            note = a.optString("note", ""),
                            timestamp = a.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            FullBackupData(
                storeSettings = settings,
                products = products,
                transactions = transactions,
                shifts = shifts,
                adjustments = adjustments,
                timestamp = rootObj.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 5. HELPER FUNCTIONS
    // ==========================================

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Cadangan Data Kasir POS Offline: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            reader.close()
            inputStream.close()
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}

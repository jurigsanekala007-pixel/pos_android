package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.dao.PaymentSummary
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockAdjustmentEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.model.CartItem
import com.example.util.FullBackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val stockAdjustmentDao = database.stockAdjustmentDao()
    private val storeSettingsDao = database.storeSettingsDao()
    private val cashierShiftDao = database.cashierShiftDao()

    // Products
    val allActiveProducts: Flow<List<ProductEntity>> = productDao.getAllActiveProducts()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val totalProductCount: Flow<Int> = productDao.getTotalProductCount()

    // Store Settings
    val storeSettings: Flow<StoreSettingsEntity?> = storeSettingsDao.getStoreSettings()

    // Stock Adjustments
    val allStockAdjustments: Flow<List<StockAdjustmentEntity>> = stockAdjustmentDao.getAllAdjustments()

    // Transactions
    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()

    // Cashier Shifts
    val activeShift: Flow<CashierShiftEntity?> = cashierShiftDao.getActiveShift()
    val allShifts: Flow<List<CashierShiftEntity>> = cashierShiftDao.getAllShifts()

    suspend fun getProductById(id: Long): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductBySku(sku: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductBySku(sku)
    }

    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = productDao.insertProduct(product)
            if (product.stock > 0) {
                stockAdjustmentDao.insertAdjustment(
                    StockAdjustmentEntity(
                        productId = id,
                        productName = product.name,
                        changeQuantity = product.stock,
                        previousStock = 0,
                        newStock = product.stock,
                        reason = "Stok Awal Produk Baru",
                        referenceId = "NEW-PROD"
                    )
                )
            }
            id
        } else {
            productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.softDeleteProduct(product.id)
    }

    suspend fun adjustStock(
        productId: Long,
        quantityChange: Int,
        reason: String,
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val oldStock = product.stock
        val newStock = (oldStock + quantityChange).coerceAtLeast(0)
        
        productDao.updateStock(productId, quantityChange)
        stockAdjustmentDao.insertAdjustment(
            StockAdjustmentEntity(
                productId = productId,
                productName = product.name,
                changeQuantity = quantityChange,
                previousStock = oldStock,
                newStock = newStock,
                reason = reason,
                note = note
            )
        )
    }

    suspend fun updateStoreSettings(settings: StoreSettingsEntity) = withContext(Dispatchers.IO) {
        storeSettingsDao.insertOrUpdate(settings)
    }

    // ==========================================
    // CASHIER SHIFT MANAGEMENT
    // ==========================================

    suspend fun startShift(
        startCash: Double,
        cashierName: String,
        notes: String = ""
    ): CashierShiftEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault())
        val shiftNumber = "SHF-${dateFormat.format(Date(timestamp))}"

        val shift = CashierShiftEntity(
            shiftNumber = shiftNumber,
            cashierName = cashierName.ifBlank { "Kasir" },
            startTime = timestamp,
            startCash = startCash,
            cashSales = 0.0,
            nonCashSales = 0.0,
            totalDiscount = 0.0,
            transactionCount = 0,
            cashIn = 0.0,
            cashOut = 0.0,
            expectedCash = startCash,
            status = "OPEN",
            notes = notes
        )
        val id = cashierShiftDao.insertShift(shift)
        shift.copy(id = id)
    }

    suspend fun addCashMovement(
        shiftId: Long,
        isCashIn: Boolean,
        amount: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        val shift = cashierShiftDao.getShiftById(shiftId) ?: return@withContext
        val newCashIn = if (isCashIn) shift.cashIn + amount else shift.cashIn
        val newCashOut = if (!isCashIn) shift.cashOut + amount else shift.cashOut
        val newExpectedCash = shift.startCash + shift.cashSales + newCashIn - newCashOut

        val currentNotes = if (shift.notes.isBlank()) "" else "${shift.notes}; "
        val movementLabel = if (isCashIn) "Kas Masuk (+${amount.toInt()}): $note" else "Kas Keluar (-${amount.toInt()}): $note"

        cashierShiftDao.updateShift(
            shift.copy(
                cashIn = newCashIn,
                cashOut = newCashOut,
                expectedCash = newExpectedCash,
                notes = "$currentNotes$movementLabel"
            )
        )
    }

    suspend fun closeShift(
        shiftId: Long,
        actualCash: Double,
        notes: String
    ): CashierShiftEntity? = withContext(Dispatchers.IO) {
        val shift = cashierShiftDao.getShiftById(shiftId) ?: return@withContext null
        val diff = actualCash - shift.expectedCash
        val endTimestamp = System.currentTimeMillis()

        val updated = shift.copy(
            endTime = endTimestamp,
            actualCash = actualCash,
            cashDifference = diff,
            status = "CLOSED",
            notes = if (notes.isNotBlank()) "${shift.notes}\nCatatan Penutupan: $notes" else shift.notes
        )
        cashierShiftDao.updateShift(updated)
        updated
    }

    suspend fun getShiftById(id: Long): CashierShiftEntity? = withContext(Dispatchers.IO) {
        cashierShiftDao.getShiftById(id)
    }

    // Process complete Checkout transaction
    suspend fun processCheckout(
        cartItems: List<CartItem>,
        customerName: String,
        discountAmount: Double,
        taxAmount: Double,
        paymentMethod: String,
        cashReceived: Double,
        cashChange: Double,
        note: String,
        cashierName: String
    ): TransactionWithItems = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val trxNumber = "TRX-${timeFormat.format(Date(timestamp))}"

        val subtotal = cartItems.sumOf { it.subtotal }
        val itemDiscountsTotal = cartItems.sumOf { it.itemDiscount }
        val totalAmount = (subtotal - discountAmount - itemDiscountsTotal + taxAmount).coerceAtLeast(0.0)
        val totalProfit = cartItems.sumOf { it.profit } - discountAmount

        val transactionEntity = TransactionEntity(
            transactionNumber = trxNumber,
            customerName = if (customerName.isBlank()) "Pelanggan Umum" else customerName.trim(),
            subtotal = subtotal,
            discountAmount = discountAmount + itemDiscountsTotal,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            totalProfit = totalProfit,
            paymentMethod = paymentMethod,
            cashReceived = cashReceived,
            cashChange = cashChange,
            note = note,
            cashierName = cashierName,
            status = "COMPLETED",
            timestamp = timestamp
        )

        val txId = transactionDao.insertTransaction(transactionEntity)

        val itemEntities = cartItems.map { cartItem ->
            TransactionItemEntity(
                transactionId = txId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                category = cartItem.product.category,
                costPrice = cartItem.product.costPrice,
                sellingPrice = cartItem.product.sellingPrice,
                quantity = cartItem.quantity,
                unit = cartItem.product.unit,
                subtotal = cartItem.subtotal,
                itemDiscount = cartItem.itemDiscount,
                profit = cartItem.profit
            )
        }

        transactionDao.insertTransactionItems(itemEntities)

        // Decrement stock & record adjustment audit
        for (item in cartItems) {
            val currentProduct = productDao.getProductById(item.product.id)
            val currentStock = currentProduct?.stock ?: item.product.stock
            val newStock = (currentStock - item.quantity).coerceAtLeast(0)

            productDao.updateStock(item.product.id, -item.quantity)
            stockAdjustmentDao.insertAdjustment(
                StockAdjustmentEntity(
                    productId = item.product.id,
                    productName = item.product.name,
                    changeQuantity = -item.quantity,
                    previousStock = currentStock,
                    newStock = newStock,
                    reason = "Transaksi Kasir ($trxNumber)",
                    referenceId = trxNumber
                )
            )
        }

        // Accumulate into active Cashier Shift if one is active
        val activeShift = cashierShiftDao.getActiveShiftSync()
        if (activeShift != null) {
            val isCash = paymentMethod == "TUNAI"
            val addCash = if (isCash) totalAmount else 0.0
            val addNonCash = if (!isCash) totalAmount else 0.0

            val updatedShift = activeShift.copy(
                cashSales = activeShift.cashSales + addCash,
                nonCashSales = activeShift.nonCashSales + addNonCash,
                totalDiscount = activeShift.totalDiscount + discountAmount + itemDiscountsTotal,
                transactionCount = activeShift.transactionCount + 1,
                expectedCash = activeShift.expectedCash + addCash
            )
            cashierShiftDao.updateShift(updatedShift)
        }

        TransactionWithItems(
            transaction = transactionEntity.copy(id = txId),
            items = itemEntities
        )
    }

    // Void / Cancel a completed transaction
    suspend fun voidTransaction(transactionId: Long, reason: String) = withContext(Dispatchers.IO) {
        val trxWithItems = transactionDao.getTransactionWithItemsById(transactionId) ?: return@withContext
        if (trxWithItems.transaction.status == "VOIDED") return@withContext

        transactionDao.voidTransaction(transactionId, reason)

        // Restore stock
        for (item in trxWithItems.items) {
            val product = productDao.getProductById(item.productId)
            val currentStock = product?.stock ?: 0
            val newStock = currentStock + item.quantity

            productDao.updateStock(item.productId, item.quantity)
            stockAdjustmentDao.insertAdjustment(
                StockAdjustmentEntity(
                    productId = item.productId,
                    productName = item.productName,
                    changeQuantity = item.quantity,
                    previousStock = currentStock,
                    newStock = newStock,
                    reason = "Void / Pembatalan Transaksi (${trxWithItems.transaction.transactionNumber}): $reason",
                    referenceId = trxWithItems.transaction.transactionNumber
                )
            )
        }

        // Adjust active shift financials if currently open
        val activeShift = cashierShiftDao.getActiveShiftSync()
        if (activeShift != null) {
            val isCash = trxWithItems.transaction.paymentMethod == "TUNAI"
            val totalAmount = trxWithItems.transaction.totalAmount
            val deductCash = if (isCash) totalAmount else 0.0
            val deductNonCash = if (!isCash) totalAmount else 0.0

            val updatedShift = activeShift.copy(
                cashSales = (activeShift.cashSales - deductCash).coerceAtLeast(0.0),
                nonCashSales = (activeShift.nonCashSales - deductNonCash).coerceAtLeast(0.0),
                totalDiscount = (activeShift.totalDiscount - trxWithItems.transaction.discountAmount).coerceAtLeast(0.0),
                transactionCount = (activeShift.transactionCount - 1).coerceAtLeast(0),
                expectedCash = (activeShift.expectedCash - deductCash).coerceAtLeast(0.0)
            )
            cashierShiftDao.updateShift(updatedShift)
        }
    }

    // Permanently delete a transaction record and its line items
    suspend fun deleteTransaction(transactionId: Long, restoreStock: Boolean = false) = withContext(Dispatchers.IO) {
        val trxWithItems = transactionDao.getTransactionWithItemsById(transactionId) ?: return@withContext
        if (restoreStock && trxWithItems.transaction.status != "VOIDED") {
            for (item in trxWithItems.items) {
                productDao.updateStock(item.productId, item.quantity)
            }
        }
        transactionDao.deleteTransactionItems(transactionId)
        transactionDao.deleteTransactionById(transactionId)
    }

    // ==========================================
    // IMPORT & BACKUP DATA ACCESS
    // ==========================================

    suspend fun importProducts(importedProducts: List<ProductEntity>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (p in importedProducts) {
            val existing = if (p.sku.isNotBlank()) productDao.getProductBySku(p.sku) else null
            if (existing != null) {
                // Update existing product
                productDao.updateProduct(
                    existing.copy(
                        name = p.name,
                        category = p.category,
                        costPrice = p.costPrice,
                        sellingPrice = p.sellingPrice,
                        stock = p.stock,
                        minStockAlert = p.minStockAlert,
                        unit = p.unit,
                        colorHex = p.colorHex,
                        isActive = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                // Insert new product
                val newId = productDao.insertProduct(p.copy(id = 0L))
                if (p.stock > 0) {
                    stockAdjustmentDao.insertAdjustment(
                        StockAdjustmentEntity(
                            productId = newId,
                            productName = p.name,
                            changeQuantity = p.stock,
                            previousStock = 0,
                            newStock = p.stock,
                            reason = "Impor Produk",
                            referenceId = "IMPORT-CSV-JSON"
                        )
                    )
                }
            }
            count++
        }
        count
    }

    suspend fun restoreFullDatabase(backupData: FullBackupData) = withContext(Dispatchers.IO) {
        database.withTransaction {
            // 1. Restore Store Settings
            if (backupData.storeSettings != null) {
                storeSettingsDao.insertOrUpdate(backupData.storeSettings.copy(id = 1))
            }

            // 2. Restore Products
            for (p in backupData.products) {
                productDao.insertProduct(p)
            }

            // 3. Restore Transactions & Items
            for (t in backupData.transactions) {
                transactionDao.insertTransaction(t.transaction)
                transactionDao.insertTransactionItems(t.items)
            }

            // 4. Restore Shifts
            for (s in backupData.shifts) {
                cashierShiftDao.insertShift(s)
            }

            // 5. Restore Adjustments
            for (a in backupData.adjustments) {
                stockAdjustmentDao.insertAdjustment(a)
            }
        }
    }

    suspend fun getAllProductsSync(): List<ProductEntity> = withContext(Dispatchers.IO) {
        productDao.getAllProductsSync()
    }

    suspend fun getAllTransactionsSync(): List<TransactionWithItems> = withContext(Dispatchers.IO) {
        transactionDao.getAllTransactionsWithItemsSync()
    }

    suspend fun getAllShiftsSync(): List<CashierShiftEntity> = withContext(Dispatchers.IO) {
        cashierShiftDao.getAllShiftsSync()
    }

    suspend fun getAllAdjustmentsSync(): List<StockAdjustmentEntity> = withContext(Dispatchers.IO) {
        stockAdjustmentDao.getAllAdjustmentsSync()
    }

    suspend fun getStoreSettingsSync(): StoreSettingsEntity? = withContext(Dispatchers.IO) {
        storeSettingsDao.getStoreSettingsDirect()
    }

    fun getTransactionsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithItems>> {
        return transactionDao.getTransactionsByDateRange(startTimestamp, endTimestamp)
    }

    fun getTotalRevenue(startTimestamp: Long, endTimestamp: Long): Flow<Double> {
        return transactionDao.getTotalRevenue(startTimestamp, endTimestamp)
    }

    fun getTotalProfit(startTimestamp: Long, endTimestamp: Long): Flow<Double> {
        return transactionDao.getTotalProfit(startTimestamp, endTimestamp)
    }

    fun getCompletedTransactionCount(startTimestamp: Long, endTimestamp: Long): Flow<Int> {
        return transactionDao.getCompletedTransactionCount(startTimestamp, endTimestamp)
    }

    fun getTopSellingProducts(startTimestamp: Long, endTimestamp: Long, limit: Int = 10): Flow<List<TopProductSummary>> {
        return transactionDao.getTopSellingProducts(startTimestamp, endTimestamp, limit)
    }

    fun getPaymentMethodBreakdown(startTimestamp: Long, endTimestamp: Long): Flow<List<PaymentSummary>> {
        return transactionDao.getPaymentMethodBreakdown(startTimestamp, endTimestamp)
    }

    fun getCategorySalesBreakdown(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySalesSummary>> {
        return transactionDao.getCategorySalesBreakdown(startTimestamp, endTimestamp)
    }

    suspend fun resetDatabaseDemoData() = withContext(Dispatchers.IO) {
        AppDatabase.populateInitialData(database)
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.dao.PaymentSummary
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockAdjustmentEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.model.CartItem
import com.example.data.model.ReportPeriod
import com.example.data.repository.PosRepository
import com.example.util.FormatUtils
import com.example.util.FullBackupData
import com.example.util.PosBackupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class PosScreen(val title: String, val route: String) {
    CASHIER("Kasir", "cashier"),
    INVENTORY("Produk & Stok", "inventory"),
    REPORTS("Laporan", "reports"),
    TRANSACTIONS("Riwayat", "transactions"),
    SETTINGS("Pengaturan", "settings")
}

data class PosUiState(
    val currentScreen: PosScreen = PosScreen.CASHIER,
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val isCartOpen: Boolean = false,
    val isCheckoutOpen: Boolean = false,
    val isBarcodeScannerOpen: Boolean = false,
    val lastScannedBarcode: String? = null,
    val scannedProductFeedback: ProductEntity? = null,
    val customerName: String = "Pelanggan Umum",
    val selectedPaymentMethod: String = "TUNAI",
    val cashReceivedString: String = "",
    val transactionDiscount: Double = 0.0,
    val transactionNote: String = "",
    val completedReceipt: TransactionWithItems? = null,
    val selectedTransactionDetail: TransactionWithItems? = null,
    val editingProduct: ProductEntity? = null,
    val isProductFormOpen: Boolean = false,
    val adjustingProduct: ProductEntity? = null,
    val isStockAdjustmentOpen: Boolean = false,
    val voidingTransaction: TransactionWithItems? = null,
    val isVoidDialogOpen: Boolean = false,
    val reportPeriod: ReportPeriod = ReportPeriod.TODAY,
    val inventoryTab: Int = 0, // 0: Katalog, 1: Laporan Nilai Stok, 2: Riwayat Mutasi
    val inventoryThreshold: Int = 5, // Configurable minimum stock threshold
    val inventoryReportFilter: String = "ALL", // ALL, LOW_STOCK, OUT_OF_STOCK, HEALTHY
    val isShiftSheetOpen: Boolean = false,
    val isTransactionDiscountDialogOpen: Boolean = false,
    val editingItemDiscountProduct: CartItem? = null,
    val isItemDiscountDialogOpen: Boolean = false,
    val isBackupRestoreSheetOpen: Boolean = false,
    val pendingFullBackupToRestore: FullBackupData? = null,
    val isFullBackupRestoreConfirmOpen: Boolean = false
) {
    val cartSubtotal: Double
        get() = cartItems.sumOf { it.subtotal }

    val cartItemDiscountsTotal: Double
        get() = cartItems.sumOf { it.itemDiscount }

    val cartItemCount: Int
        get() = cartItems.sumOf { it.quantity }

    val cashReceived: Double
        get() = cashReceivedString.toDoubleOrNull() ?: 0.0

    fun calculateTotal(taxRate: Double, taxEnabled: Boolean): Double {
        val afterDiscount = (cartSubtotal - transactionDiscount - cartItemDiscountsTotal).coerceAtLeast(0.0)
        val tax = if (taxEnabled) (afterDiscount * taxRate / 100.0) else 0.0
        return afterDiscount + tax
    }

    fun calculateTax(taxRate: Double, taxEnabled: Boolean): Double {
        if (!taxEnabled) return 0.0
        val afterDiscount = (cartSubtotal - transactionDiscount - cartItemDiscountsTotal).coerceAtLeast(0.0)
        return afterDiscount * taxRate / 100.0
    }

    fun calculateChange(taxRate: Double, taxEnabled: Boolean): Double {
        val total = calculateTotal(taxRate, taxEnabled)
        return (cashReceived - total).coerceAtLeast(0.0)
    }
}

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PosRepository(database)

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Store Settings Flow
    val storeSettings: StateFlow<StoreSettingsEntity?> = repository.storeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All Active Products
    val allActiveProducts: StateFlow<List<ProductEntity>> = repository.allActiveProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Categories
    val categories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Low Stock Alert Count
    val lowStockCount: StateFlow<Int> = repository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Stock Adjustments
    val stockAdjustments: StateFlow<List<StockAdjustmentEntity>> = repository.allStockAdjustments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Transactions
    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cashier Shifts
    val activeShift: StateFlow<CashierShiftEntity?> = repository.activeShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShifts: StateFlow<List<CashierShiftEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Report Period Range calculation
    private val _reportDateRange = MutableStateFlow(calculateTimestampsForPeriod(ReportPeriod.TODAY))

    @OptIn(ExperimentalCoroutinesApi::class)
    val periodTransactions: StateFlow<List<TransactionWithItems>> = _reportDateRange
        .flatMapLatest { range ->
            repository.getTransactionsByDateRange(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val periodRevenue: StateFlow<Double> = _reportDateRange
        .flatMapLatest { range ->
            repository.getTotalRevenue(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val periodProfit: StateFlow<Double> = _reportDateRange
        .flatMapLatest { range ->
            repository.getTotalProfit(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val topSellingProducts: StateFlow<List<TopProductSummary>> = _reportDateRange
        .flatMapLatest { range ->
            repository.getTopSellingProducts(range.first, range.second, 10)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val paymentSummaries: StateFlow<List<PaymentSummary>> = _reportDateRange
        .flatMapLatest { range ->
            repository.getPaymentMethodBreakdown(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val categorySummaries: StateFlow<List<CategorySalesSummary>> = _reportDateRange
        .flatMapLatest { range ->
            repository.getCategorySalesBreakdown(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: PosScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun setInventoryTab(tab: Int) {
        _uiState.value = _uiState.value.copy(inventoryTab = tab)
    }

    fun setInventoryThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(inventoryThreshold = threshold.coerceAtLeast(0))
    }

    fun setInventoryReportFilter(filter: String) {
        _uiState.value = _uiState.value.copy(inventoryReportFilter = filter)
    }

    // --- Barcode Scanner Actions ---
    fun setBarcodeScannerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(
            isBarcodeScannerOpen = isOpen,
            scannedProductFeedback = if (!isOpen) null else _uiState.value.scannedProductFeedback
        )
    }

    fun dismissScannedProductFeedback() {
        _uiState.value = _uiState.value.copy(scannedProductFeedback = null)
    }

    fun onBarcodeScanned(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return

        val products = allActiveProducts.value
        val product = products.find { it.sku.equals(trimmed, ignoreCase = true) }
            ?: products.find { it.sku.contains(trimmed, ignoreCase = true) }
            ?: products.find { it.id.toString() == trimmed }
            ?: products.find { it.name.contains(trimmed, ignoreCase = true) }

        if (product != null) {
            if (product.stock <= 0) {
                showSnackbar("⚠️ Produk '${product.name}' ($trimmed) stok habis (0 ${product.unit})!")
                return
            }
            addToCart(product)
            _uiState.value = _uiState.value.copy(
                lastScannedBarcode = trimmed,
                scannedProductFeedback = product
            )
            showSnackbar("✅ Berhasil Scan: ${product.name} - ${FormatUtils.formatRupiah(product.sellingPrice)}")
        } else {
            showSnackbar("❌ Barcode '$trimmed' tidak ditemukan di basis data stok!")
        }
    }

    // --- POS & Cart Actions ---
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun addToCart(product: ProductEntity) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val currentItem = currentCart[index]
            if (currentItem.quantity + 1 > product.stock) {
                showSnackbar("Stok produk tidak mencukupi (${product.stock} ${product.unit})")
                return
            }
            currentCart[index] = currentItem.copy(quantity = currentItem.quantity + 1)
        } else {
            if (product.stock < 1) {
                showSnackbar("Stok ${product.name} habis!")
                return
            }
            currentCart.add(CartItem(product = product, quantity = 1))
        }

        _uiState.value = _uiState.value.copy(cartItems = currentCart)
    }

    fun updateCartQuantity(productId: Long, newQuantity: Int) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }

        if (index != -1) {
            val item = currentCart[index]
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else {
                if (newQuantity > item.product.stock) {
                    showSnackbar("Maksimal stok tersedia: ${item.product.stock} ${item.product.unit}")
                    return
                }
                val newSubtotal = item.product.sellingPrice * newQuantity
                val adjustedDiscount = item.itemDiscount.coerceAtMost(newSubtotal)
                currentCart[index] = item.copy(quantity = newQuantity, itemDiscount = adjustedDiscount)
            }
            _uiState.value = _uiState.value.copy(cartItems = currentCart)
        }
    }

    // --- Discount Management Actions ---
    fun setTransactionDiscountDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isTransactionDiscountDialogOpen = isOpen)
    }

    fun openItemDiscountDialog(cartItem: CartItem) {
        _uiState.value = _uiState.value.copy(
            editingItemDiscountProduct = cartItem,
            isItemDiscountDialogOpen = true
        )
    }

    fun dismissItemDiscountDialog() {
        _uiState.value = _uiState.value.copy(
            editingItemDiscountProduct = null,
            isItemDiscountDialogOpen = false
        )
    }

    fun applyItemDiscount(productId: Long, discountAmount: Double) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentCart[index]
            val validDiscount = discountAmount.coerceAtLeast(0.0).coerceAtMost(item.subtotal)
            currentCart[index] = item.copy(itemDiscount = validDiscount)
            _uiState.value = _uiState.value.copy(cartItems = currentCart)
            showSnackbar(if (validDiscount > 0) "Diskon item sebesar ${FormatUtils.formatRupiah(validDiscount)} diterapkan!" else "Diskon item dihapus.")
        }
    }

    fun applyTransactionDiscount(discountAmount: Double) {
        val maxDiscount = (_uiState.value.cartSubtotal - _uiState.value.cartItemDiscountsTotal).coerceAtLeast(0.0)
        val validDiscount = discountAmount.coerceAtLeast(0.0).coerceAtMost(maxDiscount)
        _uiState.value = _uiState.value.copy(
            transactionDiscount = validDiscount,
            isTransactionDiscountDialogOpen = false
        )
        showSnackbar(if (validDiscount > 0) "Diskon total transaksi sebesar ${FormatUtils.formatRupiah(validDiscount)} diterapkan!" else "Diskon transaksi dihapus.")
    }

    fun removeFromCart(productId: Long) {
        val currentCart = _uiState.value.cartItems.filterNot { it.product.id == productId }
        _uiState.value = _uiState.value.copy(cartItems = currentCart)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            transactionDiscount = 0.0,
            cashReceivedString = "",
            customerName = "Pelanggan Umum",
            transactionNote = "",
            isCartOpen = false,
            isCheckoutOpen = false
        )
    }

    fun setCartOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isCartOpen = isOpen)
    }

    fun setCheckoutOpen(isOpen: Boolean) {
        val state = _uiState.value
        val settings = storeSettings.value
        val taxRate = settings?.taxRate ?: 0.0
        val taxEnabled = settings?.taxEnabled ?: false
        val total = state.calculateTotal(taxRate, taxEnabled)

        _uiState.value = _uiState.value.copy(
            isCheckoutOpen = isOpen,
            cashReceivedString = if (state.cashReceivedString.isBlank()) total.toInt().toString() else state.cashReceivedString
        )
    }

    fun setCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)
    }

    fun setCashReceived(amountStr: String) {
        _uiState.value = _uiState.value.copy(cashReceivedString = amountStr)
    }

    fun setQuickCash(amount: Double) {
        _uiState.value = _uiState.value.copy(cashReceivedString = amount.toInt().toString())
    }

    fun setTransactionNote(note: String) {
        _uiState.value = _uiState.value.copy(transactionNote = note)
    }

    fun processCheckout() {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            showSnackbar("Keranjang belanja masih kosong!")
            return
        }

        val settings = storeSettings.value
        val taxRate = settings?.taxRate ?: 0.0
        val taxEnabled = settings?.taxEnabled ?: false
        val total = state.calculateTotal(taxRate, taxEnabled)
        val taxAmount = state.calculateTax(taxRate, taxEnabled)
        val cashierName = activeShift.value?.cashierName ?: settings?.cashierName ?: "Kasir"

        if (state.selectedPaymentMethod == "TUNAI" && state.cashReceived < total) {
            showSnackbar("Jumlah uang tunai kurang dari total pembayaran!")
            return
        }

        val change = if (state.selectedPaymentMethod == "TUNAI") (state.cashReceived - total).coerceAtLeast(0.0) else 0.0

        viewModelScope.launch {
            try {
                val completedTrx = repository.processCheckout(
                    cartItems = state.cartItems,
                    customerName = state.customerName,
                    discountAmount = state.transactionDiscount,
                    taxAmount = taxAmount,
                    paymentMethod = state.selectedPaymentMethod,
                    cashReceived = if (state.selectedPaymentMethod == "TUNAI") state.cashReceived else total,
                    cashChange = change,
                    note = state.transactionNote,
                    cashierName = cashierName
                )

                _uiState.value = _uiState.value.copy(
                    cartItems = emptyList(),
                    transactionDiscount = 0.0,
                    cashReceivedString = "",
                    transactionNote = "",
                    isCartOpen = false,
                    isCheckoutOpen = false,
                    completedReceipt = completedTrx
                )
                showSnackbar("Transaksi ${completedTrx.transaction.transactionNumber} Berhasil!")
            } catch (e: Exception) {
                showSnackbar("Gagal memproses transaksi: ${e.localizedMessage}")
            }
        }
    }

    fun dismissReceipt() {
        _uiState.value = _uiState.value.copy(completedReceipt = null)
    }

    fun openTransactionDetail(trx: TransactionWithItems) {
        _uiState.value = _uiState.value.copy(selectedTransactionDetail = trx)
    }

    fun dismissTransactionDetail() {
        _uiState.value = _uiState.value.copy(selectedTransactionDetail = null)
    }

    // ==========================================
    // CASHIER SHIFT ACTIONS
    // ==========================================

    fun setShiftSheetOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isShiftSheetOpen = isOpen)
    }

    fun startShift(startCash: Double, cashierName: String, notes: String) {
        viewModelScope.launch {
            try {
                val shift = repository.startShift(startCash, cashierName, notes)
                showSnackbar("Shift ${shift.shiftNumber} berhasil dibuka dengan modal ${FormatUtils.formatRupiah(startCash)}!")
            } catch (e: Exception) {
                showSnackbar("Gagal membuka shift: ${e.localizedMessage}")
            }
        }
    }

    fun addCashMovement(isCashIn: Boolean, amount: Double, note: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            try {
                repository.addCashMovement(shift.id, isCashIn, amount, note)
                showSnackbar(if (isCashIn) "Kas Masuk ${FormatUtils.formatRupiah(amount)} dicatat." else "Kas Keluar ${FormatUtils.formatRupiah(amount)} dicatat.")
            } catch (e: Exception) {
                showSnackbar("Gagal mencatat mutasi kas: ${e.localizedMessage}")
            }
        }
    }

    fun closeShift(actualCash: Double, notes: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            try {
                val closed = repository.closeShift(shift.id, actualCash, notes)
                val diff = closed?.cashDifference ?: 0.0
                val diffText = if (diff == 0.0) "Saldo Pas" else if (diff > 0) "+ ${FormatUtils.formatRupiah(diff)} (Surplus)" else "- ${FormatUtils.formatRupiah(Math.abs(diff))} (Defisit)"
                showSnackbar("Shift ${shift.shiftNumber} berhasil ditutup! Rekonsiliasi: $diffText")
            } catch (e: Exception) {
                showSnackbar("Gagal menutup shift: ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // BACKUP & RESTORE ACTIONS (CSV / JSON)
    // ==========================================

    fun exportProductsCsv(context: Context) {
        viewModelScope.launch {
            val products = repository.getAllProductsSync()
            val file = PosBackupManager.exportProductsToCsv(context, products)
            if (file != null) {
                PosBackupManager.shareFile(context, file, "text/csv", "Ekspor Daftar Produk (CSV)")
                showSnackbar("File CSV Produk berhasil dibuat: ${file.name}")
            } else {
                showSnackbar("Gagal mengekspor file CSV produk.")
            }
        }
    }

    fun exportProductsJson(context: Context) {
        viewModelScope.launch {
            val products = repository.getAllProductsSync()
            val file = PosBackupManager.exportProductsToJson(context, products)
            if (file != null) {
                PosBackupManager.shareFile(context, file, "application/json", "Ekspor Daftar Produk (JSON)")
                showSnackbar("File JSON Produk berhasil dibuat: ${file.name}")
            } else {
                showSnackbar("Gagal mengekspor file JSON produk.")
            }
        }
    }

    fun exportTransactionsCsv(context: Context) {
        viewModelScope.launch {
            val transactions = repository.getAllTransactionsSync()
            val file = PosBackupManager.exportTransactionsToCsv(context, transactions)
            if (file != null) {
                PosBackupManager.shareFile(context, file, "text/csv", "Ekspor Riwayat Transaksi (CSV)")
                showSnackbar("File CSV Transaksi berhasil dibuat: ${file.name}")
            } else {
                showSnackbar("Gagal mengekspor file CSV transaksi.")
            }
        }
    }

    fun exportTransactionsJson(context: Context) {
        viewModelScope.launch {
            val transactions = repository.getAllTransactionsSync()
            val file = PosBackupManager.exportTransactionsToJson(context, transactions)
            if (file != null) {
                PosBackupManager.shareFile(context, file, "application/json", "Ekspor Riwayat Transaksi (JSON)")
                showSnackbar("File JSON Transaksi berhasil dibuat: ${file.name}")
            } else {
                showSnackbar("Gagal mengekspor file JSON transaksi.")
            }
        }
    }

    fun exportFullBackupJson(context: Context) {
        viewModelScope.launch {
            val settings = repository.getStoreSettingsSync()
            val products = repository.getAllProductsSync()
            val transactions = repository.getAllTransactionsSync()
            val shifts = repository.getAllShiftsSync()
            val adjustments = repository.getAllAdjustmentsSync()

            val file = PosBackupManager.exportFullBackupJson(
                context,
                settings,
                products,
                transactions,
                shifts,
                adjustments
            )
            if (file != null) {
                PosBackupManager.shareFile(context, file, "application/json", "Cadangan Lengkap Database POS Offline")
                showSnackbar("Cadangan Penuh Database berhasil dibuat: ${file.name}")
            } else {
                showSnackbar("Gagal membuat file cadangan database.")
            }
        }
    }

    fun importProductsFromContent(content: String, isJson: Boolean) {
        viewModelScope.launch {
            try {
                val products = if (isJson) {
                    PosBackupManager.parseProductsFromJson(content)
                } else {
                    PosBackupManager.parseProductsFromCsv(content)
                }

                if (products.isEmpty()) {
                    showSnackbar("Tidak ada produk valid yang ditemukan dalam file.")
                    return@launch
                }

                val count = repository.importProducts(products)
                showSnackbar("✅ Berhasil mengimpor $count produk ke dalam katalog!")
            } catch (e: Exception) {
                showSnackbar("❌ Gagal mengimpor produk: ${e.localizedMessage}")
            }
        }
    }

    fun prepareFullBackupRestore(content: String) {
        try {
            val backupData = PosBackupManager.parseFullBackupJson(content)
            if (backupData != null) {
                _uiState.value = _uiState.value.copy(
                    pendingFullBackupToRestore = backupData,
                    isFullBackupRestoreConfirmOpen = true
                )
            } else {
                showSnackbar("Format file cadangan tidak valid atau rusak!")
            }
        } catch (e: Exception) {
            showSnackbar("Gagal membaca file cadangan: ${e.localizedMessage}")
        }
    }

    fun confirmFullBackupRestore() {
        val backupData = _uiState.value.pendingFullBackupToRestore ?: return
        viewModelScope.launch {
            try {
                repository.restoreFullDatabase(backupData)
                _uiState.value = _uiState.value.copy(
                    pendingFullBackupToRestore = null,
                    isFullBackupRestoreConfirmOpen = false
                )
                showSnackbar("✅ Pemulihan database offline berhasil! (${backupData.products.size} produk, ${backupData.transactions.size} transaksi)")
            } catch (e: Exception) {
                showSnackbar("❌ Gagal memulihkan database: ${e.localizedMessage}")
            }
        }
    }

    fun dismissFullBackupRestoreDialog() {
        _uiState.value = _uiState.value.copy(
            pendingFullBackupToRestore = null,
            isFullBackupRestoreConfirmOpen = false
        )
    }

    // --- Product & Inventory Management ---
    fun openAddProductForm() {
        _uiState.value = _uiState.value.copy(
            editingProduct = null,
            isProductFormOpen = true
        )
    }

    fun openEditProductForm(product: ProductEntity) {
        _uiState.value = _uiState.value.copy(
            editingProduct = product,
            isProductFormOpen = true
        )
    }

    fun dismissProductForm() {
        _uiState.value = _uiState.value.copy(
            editingProduct = null,
            isProductFormOpen = false
        )
    }

    fun saveProduct(
        id: Long,
        name: String,
        sku: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stock: Int,
        minStockAlert: Int,
        unit: String,
        colorHex: String
    ) {
        if (name.isBlank()) {
            showSnackbar("Nama produk tidak boleh kosong!")
            return
        }
        if (sellingPrice < costPrice) {
            showSnackbar("Peringatan: Harga jual lebih kecil dari harga modal!")
        }

        viewModelScope.launch {
            val product = ProductEntity(
                id = id,
                name = name.trim(),
                sku = sku.trim(),
                category = if (category.isBlank()) "Umum" else category.trim(),
                costPrice = costPrice.coerceAtLeast(0.0),
                sellingPrice = sellingPrice.coerceAtLeast(0.0),
                stock = stock.coerceAtLeast(0),
                minStockAlert = minStockAlert.coerceAtLeast(1),
                unit = if (unit.isBlank()) "pcs" else unit.trim(),
                colorHex = colorHex
            )
            repository.saveProduct(product)
            dismissProductForm()
            showSnackbar("Produk ${product.name} berhasil disimpan!")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showSnackbar("Produk ${product.name} berhasil dihapus.")
        }
    }

    fun openStockAdjustment(product: ProductEntity) {
        _uiState.value = _uiState.value.copy(
            adjustingProduct = product,
            isStockAdjustmentOpen = true
        )
    }

    fun dismissStockAdjustment() {
        _uiState.value = _uiState.value.copy(
            adjustingProduct = null,
            isStockAdjustmentOpen = false
        )
    }

    fun adjustStock(
        productId: Long,
        changeQty: Int,
        reason: String,
        note: String
    ) {
        if (changeQty == 0) {
            showSnackbar("Jumlah perubahan stok tidak boleh 0!")
            return
        }

        viewModelScope.launch {
            repository.adjustStock(productId, changeQty, reason, note)
            dismissStockAdjustment()
            showSnackbar("Stok berhasil diperbarui!")
        }
    }

    // --- Void / Cancel Transaction ---
    fun openVoidDialog(trx: TransactionWithItems) {
        _uiState.value = _uiState.value.copy(
            voidingTransaction = trx,
            isVoidDialogOpen = true
        )
    }

    fun dismissVoidDialog() {
        _uiState.value = _uiState.value.copy(
            voidingTransaction = null,
            isVoidDialogOpen = false
        )
    }

    fun confirmVoidTransaction(reason: String) {
        val trx = _uiState.value.voidingTransaction ?: return
        val voidReason = if (reason.isBlank()) "Dibatalkan oleh kasir" else reason.trim()

        viewModelScope.launch {
            repository.voidTransaction(trx.transaction.id, voidReason)
            dismissVoidDialog()
            _uiState.value = _uiState.value.copy(selectedTransactionDetail = null)
            showSnackbar("Transaksi ${trx.transaction.transactionNumber} berhasil dibatalkan & stok dikembalikan.")
        }
    }

    fun deleteTransaction(trx: TransactionWithItems, restoreStock: Boolean = true) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(trx.transaction.id, restoreStock = restoreStock)
                if (_uiState.value.selectedTransactionDetail?.transaction?.id == trx.transaction.id) {
                    _uiState.value = _uiState.value.copy(selectedTransactionDetail = null)
                }
                showSnackbar("Transaksi ${trx.transaction.transactionNumber} berhasil dihapus permanen.")
            } catch (e: Exception) {
                showSnackbar("Gagal menghapus transaksi: ${e.localizedMessage}")
            }
        }
    }

    // --- Report Period Selection ---
    fun setReportPeriod(period: ReportPeriod) {
        _uiState.value = _uiState.value.copy(reportPeriod = period)
        _reportDateRange.value = calculateTimestampsForPeriod(period)
    }

    // --- Store Settings ---
    fun saveStoreSettings(settings: StoreSettingsEntity) {
        viewModelScope.launch {
            repository.updateStoreSettings(settings)
            showSnackbar("Pengaturan toko berhasil disimpan!")
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetDatabaseDemoData()
            showSnackbar("Data demo awal berhasil dimuat ulang!")
        }
    }

    private fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarEvent.emit(message)
        }
    }

    companion object {
        fun calculateTimestampsForPeriod(period: ReportPeriod): Pair<Long, Long> {
            val now = Calendar.getInstance()
            return when (period) {
                ReportPeriod.TODAY -> {
                    val start = now.clone() as Calendar
                    start.set(Calendar.HOUR_OF_DAY, 0)
                    start.set(Calendar.MINUTE, 0)
                    start.set(Calendar.SECOND, 0)
                    start.set(Calendar.MILLISECOND, 0)
                    Pair(start.timeInMillis, now.timeInMillis)
                }
                ReportPeriod.YESTERDAY -> {
                    val start = now.clone() as Calendar
                    start.add(Calendar.DAY_OF_YEAR, -1)
                    start.set(Calendar.HOUR_OF_DAY, 0)
                    start.set(Calendar.MINUTE, 0)
                    start.set(Calendar.SECOND, 0)
                    start.set(Calendar.MILLISECOND, 0)

                    val end = start.clone() as Calendar
                    end.set(Calendar.HOUR_OF_DAY, 23)
                    end.set(Calendar.MINUTE, 59)
                    end.set(Calendar.SECOND, 59)
                    end.set(Calendar.MILLISECOND, 999)

                    Pair(start.timeInMillis, end.timeInMillis)
                }
                ReportPeriod.LAST_7_DAYS -> {
                    val start = now.clone() as Calendar
                    start.add(Calendar.DAY_OF_YEAR, -7)
                    start.set(Calendar.HOUR_OF_DAY, 0)
                    start.set(Calendar.MINUTE, 0)
                    start.set(Calendar.SECOND, 0)
                    start.set(Calendar.MILLISECOND, 0)
                    Pair(start.timeInMillis, now.timeInMillis)
                }
                ReportPeriod.THIS_MONTH -> {
                    val start = now.clone() as Calendar
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    start.set(Calendar.HOUR_OF_DAY, 0)
                    start.set(Calendar.MINUTE, 0)
                    start.set(Calendar.SECOND, 0)
                    start.set(Calendar.MILLISECOND, 0)
                    Pair(start.timeInMillis, now.timeInMillis)
                }
                ReportPeriod.ALL_TIME -> {
                    Pair(0L, Long.MAX_VALUE)
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.model.CartItem
import com.example.ui.components.BarcodeScannerSheet
import com.example.ui.components.ItemDiscountDialog
import com.example.ui.components.LowStockDashboardAlertWidget
import com.example.ui.components.ShiftManagementBottomSheet
import com.example.ui.components.TransactionDiscountDialog
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosOnErrorContainer
import com.example.ui.theme.PosOnSuccessContainer
import com.example.ui.theme.PosOnWarningContainer
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.theme.PosWarning
import com.example.ui.theme.PosWarningContainer
import com.example.ui.viewmodel.PosScreen
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    products: List<ProductEntity>,
    categories: List<String>,
    storeSettings: StoreSettingsEntity?,
    modifier: Modifier = Modifier
) {
    val activeShift by viewModel.activeShift.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()

    val filteredProducts = remember(products, uiState.searchQuery, uiState.selectedCategory) {
        products.filter { product ->
            val matchQuery = if (uiState.searchQuery.isBlank()) true else {
                product.name.contains(uiState.searchQuery, ignoreCase = true) ||
                        product.sku.contains(uiState.searchQuery, ignoreCase = true) ||
                        product.category.contains(uiState.searchQuery, ignoreCase = true)
            }
            val matchCategory = if (uiState.selectedCategory == "Semua") true else {
                product.category.equals(uiState.selectedCategory, ignoreCase = true)
            }
            matchQuery && matchCategory
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Shift Quick Status Bar
            Surface(
                color = if (activeShift != null) PosSuccessContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setShiftSheetOpen(true) }
                    .testTag("shift_status_bar")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (activeShift != null) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (activeShift != null) PosSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeShift != null) {
                                "Shift ${activeShift!!.shiftNumber} • ${activeShift!!.cashierName} • Laci: ${FormatUtils.formatRupiah(activeShift!!.expectedCash)}"
                            } else {
                                "Shift Belum Dibuka — Ketuk di sini untuk Buka Shift Kasir"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeShift != null) PosOnSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Text(
                        text = if (activeShift != null) "Kelola Shift" else "Buka Shift",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Low Stock Dashboard Alert Widget
            LowStockDashboardAlertWidget(
                products = products,
                onRestockClick = { product ->
                    viewModel.openStockAdjustment(product)
                },
                onViewAllInventory = {
                    viewModel.navigateTo(PosScreen.INVENTORY)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Search Bar & Filter Section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Search Input with Barcode Scanner Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_search_input"),
                            placeholder = { Text("Cari produk atau scan barcode...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setBarcodeScannerOpen(true) }
                                .testTag("barcode_scanner_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories Horizontal Row
                    val allCats = listOf("Semua") + categories
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(allCats) { cat ->
                            val isSelected = uiState.selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectCategory(cat) },
                                label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tidak ada produk yang cocok",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val inCart = uiState.cartItems.find { it.product.id == product.id }?.quantity ?: 0
                        ProductGridCard(
                            product = product,
                            quantityInCart = inCart,
                            onClick = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Cart Bar
        AnimatedVisibility(
            visible = uiState.cartItems.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.setCartOpen(true) }
                    .testTag("floating_cart_bar"),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val totalDiscounts = uiState.cartItemDiscountsTotal + uiState.transactionDiscount
                            Text(
                                text = if (totalDiscounts > 0) "${uiState.cartItemCount} Item (Hemat ${FormatUtils.formatRupiah(totalDiscounts)})" else "${uiState.cartItemCount} Item Terpilih",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            val finalAmount = (uiState.cartSubtotal - totalDiscounts).coerceAtLeast(0.0)
                            Text(
                                text = FormatUtils.formatRupiah(finalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.setCheckoutOpen(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("checkout_button")
                    ) {
                        Text("Bayar", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Quick Add Product Floating Action Button when cart is empty
        AnimatedVisibility(
            visible = uiState.cartItems.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            androidx.compose.material3.FloatingActionButton(
                onClick = { viewModel.openAddProductForm() },
                modifier = Modifier.testTag("cashier_add_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Produk", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Bottom Sheet: Cart Details
    if (uiState.isCartOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setCartOpen(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CartSheetContent(
                uiState = uiState,
                viewModel = viewModel,
                storeSettings = storeSettings,
                onProceedToCheckout = {
                    viewModel.setCartOpen(false)
                    viewModel.setCheckoutOpen(true)
                }
            )
        }
    }

    // Modal Bottom Sheet: Checkout & Payment
    if (uiState.isCheckoutOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setCheckoutOpen(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CheckoutSheetContent(
                uiState = uiState,
                viewModel = viewModel,
                storeSettings = storeSettings,
                onDismiss = { viewModel.setCheckoutOpen(false) }
            )
        }
    }

    // Modal Bottom Sheet: Barcode Scanner
    if (uiState.isBarcodeScannerOpen) {
        BarcodeScannerSheet(
            isOpen = uiState.isBarcodeScannerOpen,
            products = products,
            scannedProductFeedback = uiState.scannedProductFeedback,
            onBarcodeScanned = { barcode -> viewModel.onBarcodeScanned(barcode) },
            onDismissFeedback = { viewModel.dismissScannedProductFeedback() },
            onDismiss = { viewModel.setBarcodeScannerOpen(false) },
            onViewCart = {
                viewModel.setBarcodeScannerOpen(false)
                viewModel.setCartOpen(true)
            }
        )
    }

    // Modal Bottom Sheet: Shift Management
    if (uiState.isShiftSheetOpen) {
        ShiftManagementBottomSheet(
            activeShift = activeShift,
            allShifts = allShifts,
            storeSettings = storeSettings,
            onStartShift = { cash, name, notes -> viewModel.startShift(cash, name, notes) },
            onAddCashMovement = { isCashIn, amt, note -> viewModel.addCashMovement(isCashIn, amt, note) },
            onCloseShift = { actual, notes -> viewModel.closeShift(actual, notes) },
            onDismiss = { viewModel.setShiftSheetOpen(false) }
        )
    }

    // Dialog: Item Discount
    if (uiState.isItemDiscountDialogOpen && uiState.editingItemDiscountProduct != null) {
        val item = uiState.editingItemDiscountProduct!!
        ItemDiscountDialog(
            cartItem = item,
            onApplyDiscount = { discountAmount ->
                viewModel.applyItemDiscount(item.product.id, discountAmount)
            },
            onDismiss = { viewModel.dismissItemDiscountDialog() }
        )
    }

    // Dialog: Transaction Discount
    if (uiState.isTransactionDiscountDialogOpen) {
        TransactionDiscountDialog(
            currentDiscount = uiState.transactionDiscount,
            cartSubtotal = uiState.cartSubtotal,
            onApplyDiscount = { discountAmount ->
                viewModel.applyTransactionDiscount(discountAmount)
            },
            onDismiss = { viewModel.setTransactionDiscountDialogOpen(false) }
        )
    }

    // Dialog: Add / Edit Product
    if (uiState.isProductFormOpen) {
        ProductFormDialog(
            product = uiState.editingProduct,
            existingCategories = categories,
            onDismiss = { viewModel.dismissProductForm() },
            onSave = { id, name, sku, category, costPrice, sellingPrice, stock, minStockAlert, unit, colorHex ->
                viewModel.saveProduct(id, name, sku, category, costPrice, sellingPrice, stock, minStockAlert, unit, colorHex)
            }
        )
    }
}

@Composable
fun ProductGridCard(
    product: ProductEntity,
    quantityInCart: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock in 1..4 || (product.stock > 0 && product.stock <= product.minStockAlert)

    val cardBorder = when {
        isOutOfStock -> BorderStroke(1.dp, PosError.copy(alpha = 0.5f))
        isLowStock -> BorderStroke(1.5.dp, PosWarning.copy(alpha = 0.8f))
        quantityInCart > 0 -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val cardBg = when {
        isOutOfStock -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        quantityInCart > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        isLowStock -> PosWarningContainer.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isOutOfStock, onClick = onClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (quantityInCart > 0) 3.dp else 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Category tag and Quantity In Cart badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (quantityInCart > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$quantityInCart",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOutOfStock) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Selling Price
            Text(
                text = FormatUtils.formatRupiah(product.sellingPrice),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isOutOfStock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stock Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeColor = when {
                    isOutOfStock -> PosErrorContainer
                    isLowStock -> PosWarningContainer
                    else -> PosSuccessContainer
                }
                val textColor = when {
                    isOutOfStock -> PosOnErrorContainer
                    isLowStock -> PosOnWarningContainer
                    else -> PosOnSuccessContainer
                }
                val label = when {
                    isOutOfStock -> "Stok Habis"
                    isLowStock -> "Sisa ${product.stock} ${product.unit}"
                    else -> "${product.stock} ${product.unit}"
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartSheetContent(
    uiState: PosUiState,
    viewModel: PosViewModel,
    storeSettings: StoreSettingsEntity?,
    onProceedToCheckout: () -> Unit
) {
    val taxRate = storeSettings?.taxRate ?: 0.0
    val taxEnabled = storeSettings?.taxEnabled ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Keranjang Belanja",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.cartItemCount} item dalam pesanan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { viewModel.clearCart() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PosError),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kosongkan", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items List
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (cartItem in uiState.cartItems) {
                CartItemRow(
                    cartItem = cartItem,
                    onIncrease = { viewModel.updateCartQuantity(cartItem.product.id, cartItem.quantity + 1) },
                    onDecrease = { viewModel.updateCartQuantity(cartItem.product.id, cartItem.quantity - 1) },
                    onOpenDiscount = { viewModel.openItemDiscountDialog(cartItem) },
                    onRemove = { viewModel.removeFromCart(cartItem.product.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Summary Calculations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(FormatUtils.formatRupiah(uiState.cartSubtotal), fontWeight = FontWeight.SemiBold)
        }

        // Item Discounts Subtotal
        if (uiState.cartItemDiscountsTotal > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Diskon Item", color = PosError)
                Text("- ${FormatUtils.formatRupiah(uiState.cartItemDiscountsTotal)}", fontWeight = FontWeight.SemiBold, color = PosError)
            }
        }

        // Transaction Level Discount Row
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.setTransactionDiscountDialogOpen(true) }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.transactionDiscount > 0) "Diskon Transaksi (Ubah)" else "+ Tambah Diskon Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (uiState.transactionDiscount > 0) {
                Text("- ${FormatUtils.formatRupiah(uiState.transactionDiscount)}", fontWeight = FontWeight.Bold, color = PosError)
            }
        }

        if (taxEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PPN ($taxRate%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(FormatUtils.formatRupiah(uiState.calculateTax(taxRate, taxEnabled)), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Tagihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                FormatUtils.formatRupiah(uiState.calculateTotal(taxRate, taxEnabled)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onProceedToCheckout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("proceed_checkout_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Lanjut ke Pembayaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onOpenDiscount: () -> Unit,
    onRemove: () -> Unit
) {
    val hasDiscount = cartItem.itemDiscount > 0
    val finalItemPrice = (cartItem.subtotal - cartItem.itemDiscount).coerceAtLeast(0.0)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cartItem.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FormatUtils.formatRupiah(cartItem.product.sellingPrice)} / ${cartItem.product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hasDiscount) {
                            Text(
                                text = FormatUtils.formatRupiah(cartItem.subtotal),
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "Subtotal: ${FormatUtils.formatRupiah(finalItemPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // + / - Controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDecrease)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                contentDescription = "Kurang",
                                modifier = Modifier.size(16.dp),
                                tint = if (cartItem.quantity == 1) PosError else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "${cartItem.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onIncrease)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Tambah",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Bottom row: Item discount trigger
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasDiscount) PosSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenDiscount)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Percent,
                            contentDescription = null,
                            tint = if (hasDiscount) PosSuccess else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasDiscount) "Diskon: -${FormatUtils.formatRupiah(cartItem.itemDiscount)}" else "+ Diskon Item",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasDiscount) PosOnSuccessContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutSheetContent(
    uiState: PosUiState,
    viewModel: PosViewModel,
    storeSettings: StoreSettingsEntity?,
    onDismiss: () -> Unit
) {
    val taxRate = storeSettings?.taxRate ?: 0.0
    val taxEnabled = storeSettings?.taxEnabled ?: false
    val totalToPay = uiState.calculateTotal(taxRate, taxEnabled)
    val cashReceived = uiState.cashReceived
    val changeAmount = uiState.calculateChange(taxRate, taxEnabled)
    val isCashSufficient = if (uiState.selectedPaymentMethod == "TUNAI") cashReceived >= totalToPay else true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pembayaran Kasir",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Clear, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Amount Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Pembayaran",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = FormatUtils.formatRupiah(totalToPay),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val totalSavings = uiState.cartItemDiscountsTotal + uiState.transactionDiscount
                if (totalSavings > 0) {
                    Text(
                        text = "Total Hemat Diskon: ${FormatUtils.formatRupiah(totalSavings)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (taxEnabled) {
                    Text(
                        text = "Termasuk PPN $taxRate%: ${FormatUtils.formatRupiah(uiState.calculateTax(taxRate, taxEnabled))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customer Name Field
        OutlinedTextField(
            value = uiState.customerName,
            onValueChange = { viewModel.setCustomerName(it) },
            label = { Text("Nama Pelanggan") },
            placeholder = { Text("Pelanggan Umum") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Method Selector
        Text(
            text = "Metode Pembayaran",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val methods = listOf(
            Triple("TUNAI", "Tunai", Icons.Default.Payments),
            Triple("QRIS", "QRIS", Icons.Default.QrCode),
            Triple("DEBIT", "Debit/Kredit", Icons.Default.CreditCard),
            Triple("TRANSFER", "Transfer", Icons.Default.PointOfSale),
            Triple("PIUTANG", "Piutang / Kasbon", Icons.Default.LocalAtm)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            methods.forEach { (code, label, icon) ->
                val isSelected = uiState.selectedPaymentMethod == code
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.setPaymentMethod(code) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Cash Specific Inputs & Quick Buttons
        if (uiState.selectedPaymentMethod == "TUNAI") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nominal Uang Tunai Diterima",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.cashReceivedString,
                onValueChange = { viewModel.setCashReceived(it) },
                label = { Text("Jumlah Uang (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cash_input_field"),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("Rp ") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Cash Chips (Uang Pas, 10k, 20k, 50k, 100k, 200k, etc.)
            val quickAmounts = listOf(
                totalToPay to "Uang Pas",
                10000.0 to "10.000",
                20000.0 to "20.000",
                50000.0 to "50.000",
                100000.0 to "100.000",
                200000.0 to "200.000"
            ).filter { it.first >= totalToPay || it.second == "Uang Pas" }.distinctBy { it.first }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickAmounts.forEach { (amount, label) ->
                    OutlinedButton(
                        onClick = { viewModel.setQuickCash(amount) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (label == "Uang Pas") "Uang Pas" else "Rp $label",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Change (Kembalian) Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCashSufficient) PosSuccessContainer else PosErrorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCashSufficient) "Kembalian" else "Uang Kurang",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCashSufficient) PosOnSuccessContainer else PosOnErrorContainer
                        )
                        Text(
                            text = if (isCashSufficient) FormatUtils.formatRupiah(changeAmount) else FormatUtils.formatRupiah(totalToPay - cashReceived),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCashSufficient) PosOnSuccessContainer else PosOnErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Order Note Field
        OutlinedTextField(
            value = uiState.transactionNote,
            onValueChange = { viewModel.setTransactionNote(it) },
            label = { Text("Catatan Pesanan (Opsional)") },
            placeholder = { Text("Contoh: Meja 5, Kurang manis, dll") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm Checkout Button
        Button(
            onClick = { viewModel.processCheckout() },
            enabled = isCashSufficient,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("confirm_checkout_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Selesaikan Transaksi & Cetak Struk", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PosAppBar
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosScreen
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PosMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PosMainApp(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.allActiveProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val stockAdjustments by viewModel.stockAdjustments.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val periodRevenue by viewModel.periodRevenue.collectAsStateWithLifecycle()
    val periodProfit by viewModel.periodProfit.collectAsStateWithLifecycle()
    val topProducts by viewModel.topSellingProducts.collectAsStateWithLifecycle()
    val paymentSummaries by viewModel.paymentSummaries.collectAsStateWithLifecycle()
    val categorySummaries by viewModel.categorySummaries.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PosAppBar(
                storeSettings = storeSettings,
                cartItemCount = uiState.cartItemCount,
                lowStockCount = lowStockCount,
                onCartClick = { viewModel.setCartOpen(true) }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("pos_bottom_navigation"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // 1. Kasir Tab
                val isCashier = uiState.currentScreen == PosScreen.CASHIER
                NavigationBarItem(
                    selected = isCashier,
                    onClick = { viewModel.navigateTo(PosScreen.CASHIER) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.cartItemCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${uiState.cartItemCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (isCashier) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale,
                                contentDescription = "Kasir"
                            )
                        }
                    },
                    label = { Text("Kasir", fontWeight = if (isCashier) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_cashier")
                )

                // 2. Produk & Stok Tab
                val isInventory = uiState.currentScreen == PosScreen.INVENTORY
                NavigationBarItem(
                    selected = isInventory,
                    onClick = { viewModel.navigateTo(PosScreen.INVENTORY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (lowStockCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("$lowStockCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (isInventory) Icons.Filled.Inventory else Icons.Outlined.Inventory,
                                contentDescription = "Produk"
                            )
                        }
                    },
                    label = { Text("Produk & Stok", fontWeight = if (isInventory) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_inventory")
                )

                // 3. Laporan Tab
                val isReports = uiState.currentScreen == PosScreen.REPORTS
                NavigationBarItem(
                    selected = isReports,
                    onClick = { viewModel.navigateTo(PosScreen.REPORTS) },
                    icon = {
                        Icon(
                            if (isReports) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                            contentDescription = "Laporan"
                        )
                    },
                    label = { Text("Laporan", fontWeight = if (isReports) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_reports")
                )

                // 4. Riwayat Transaksi Tab
                val isTransactions = uiState.currentScreen == PosScreen.TRANSACTIONS
                NavigationBarItem(
                    selected = isTransactions,
                    onClick = { viewModel.navigateTo(PosScreen.TRANSACTIONS) },
                    icon = {
                        Icon(
                            if (isTransactions) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Riwayat"
                        )
                    },
                    label = { Text("Riwayat", fontWeight = if (isTransactions) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_transactions")
                )

                // 5. Pengaturan Tab
                val isSettings = uiState.currentScreen == PosScreen.SETTINGS
                NavigationBarItem(
                    selected = isSettings,
                    onClick = { viewModel.navigateTo(PosScreen.SETTINGS) },
                    icon = {
                        Icon(
                            if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Pengaturan"
                        )
                    },
                    label = { Text("Pengaturan", fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                PosScreen.CASHIER -> CashierScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    products = products,
                    categories = categories,
                    storeSettings = storeSettings
                )
                PosScreen.INVENTORY -> InventoryScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    products = products,
                    categories = categories,
                    stockAdjustments = stockAdjustments
                )
                PosScreen.REPORTS -> ReportsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    periodTransactions = periodTransactions,
                    periodRevenue = periodRevenue,
                    periodProfit = periodProfit,
                    topProducts = topProducts,
                    paymentSummaries = paymentSummaries,
                    categorySummaries = categorySummaries,
                    storeSettings = storeSettings
                )
                PosScreen.TRANSACTIONS -> TransactionsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    allTransactions = allTransactions,
                    storeSettings = storeSettings
                )
                PosScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    storeSettings = storeSettings
                )
            }
        }
    }

    // Modal dialog when transaction just completed
    if (uiState.completedReceipt != null) {
        ReceiptDialog(
            transactionWithItems = uiState.completedReceipt!!,
            storeSettings = storeSettings,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

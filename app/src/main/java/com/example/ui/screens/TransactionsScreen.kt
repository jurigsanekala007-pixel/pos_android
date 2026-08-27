package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingsEntity
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosOnErrorContainer
import com.example.ui.theme.PosOnSuccessContainer
import com.example.ui.theme.PosOnWarningContainer
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.theme.PosWarning
import com.example.ui.theme.PosWarningContainer
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CsvExportUtils
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterType(val label: String) {
    ALL("Semua Waktu"),
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    CUSTOM("Rentang Tanggal...")
}

@Composable
fun TransactionsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    allTransactions: List<TransactionWithItems>,
    storeSettings: StoreSettingsEntity?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Semua") }
    var selectedPayment by remember { mutableStateOf("Semua") }
    var selectedDateFilter by remember { mutableStateOf(DateFilterType.ALL) }
    var customStartDate by remember { mutableLongStateOf(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L) }
    var customEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // Long press action menu state
    var longPressedTransaction by remember { mutableStateOf<TransactionWithItems?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionWithItems?>(null) }

    // Date range calculation
    val dateRangeBounds = remember(selectedDateFilter, customStartDate, customEndDate) {
        val calendar = Calendar.getInstance()
        when (selectedDateFilter) {
            DateFilterType.ALL -> null
            DateFilterType.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            DateFilterType.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            DateFilterType.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                val end = System.currentTimeMillis()
                Pair(start, end)
            }
            DateFilterType.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                val end = System.currentTimeMillis()
                Pair(start, end)
            }
            DateFilterType.CUSTOM -> {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = customStartDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = customEndDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
        }
    }

    val filteredTransactions = remember(allTransactions, searchQuery, selectedStatus, selectedPayment, dateRangeBounds) {
        allTransactions.filter { item ->
            val trx = item.transaction
            val matchQuery = if (searchQuery.isBlank()) true else {
                trx.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                        trx.customerName.contains(searchQuery, ignoreCase = true) ||
                        item.items.any { it.productName.contains(searchQuery, ignoreCase = true) }
            }
            val matchStatus = when (selectedStatus) {
                "Selesai" -> trx.status == "COMPLETED"
                "Dibatalkan" -> trx.status == "VOIDED"
                else -> true
            }
            val matchPayment = when (selectedPayment) {
                "Semua" -> true
                else -> trx.paymentMethod.equals(selectedPayment, ignoreCase = true)
            }
            val matchDate = if (dateRangeBounds == null) true else {
                trx.timestamp in dateRangeBounds.first..dateRangeBounds.second
            }
            matchQuery && matchStatus && matchPayment && matchDate
        }
    }

    val totalCompletedRevenue = remember(filteredTransactions) {
        filteredTransactions.filter { it.transaction.status == "COMPLETED" }
            .sumOf { it.transaction.totalAmount }
    }

    val totalCompletedProfit = remember(filteredTransactions) {
        filteredTransactions.filter { it.transaction.status == "COMPLETED" }
            .sumOf { it.transaction.totalProfit }
    }

    val hasActiveFilters = searchQuery.isNotEmpty() ||
            selectedStatus != "Semua" ||
            selectedPayment != "Semua" ||
            selectedDateFilter != DateFilterType.ALL

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pencarian, filter metode bayar & rentang tanggal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedStatus = "Semua"
                                selectedPayment = "Semua"
                                selectedDateFilter = DateFilterType.ALL
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Filter", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input"),
                    placeholder = { Text("Cari nomor nota, pelanggan, atau nama produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus")
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

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Date Range Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Filter Tanggal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rentang Tanggal:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(DateFilterType.values()) { dateType ->
                        val isSelected = selectedDateFilter == dateType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (dateType == DateFilterType.CUSTOM) {
                                    showCustomDateDialog = true
                                } else {
                                    selectedDateFilter = dateType
                                }
                            },
                            label = {
                                Text(
                                    if (dateType == DateFilterType.CUSTOM && selectedDateFilter == DateFilterType.CUSTOM) {
                                        val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
                                        "${fmt.format(Date(customStartDate))} - ${fmt.format(Date(customEndDate))}"
                                    } else {
                                        dateType.label
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("date_filter_${dateType.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Payment Method Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = "Metode Pembayaran",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Metode Pembayaran:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val paymentMethods = listOf(
                    "Semua" to Icons.Default.FilterAlt,
                    "TUNAI" to Icons.Default.Payments,
                    "QRIS" to Icons.Default.QrCode,
                    "DEBIT" to Icons.Default.CreditCard,
                    "KREDIT" to Icons.Default.CreditCard,
                    "TRANSFER" to Icons.Default.AccountBalance
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(paymentMethods) { (payment, icon) ->
                        val isSelected = selectedPayment == payment
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPayment = payment },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(payment, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("payment_filter_${payment.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Status Filter Chips
                val statuses = listOf("Semua", "Selesai", "Dibatalkan")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    statuses.forEach { status ->
                        val isSelected = selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatus = status },
                            label = { Text(status, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (status == "Dibatalkan") PosError else MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("status_filter_${status.lowercase()}")
                        )
                    }
                }
            }
        }

        // Summary KPI Banner based on filtered results
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${filteredTransactions.size} Transaksi Ditemukan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Omset: ${FormatUtils.formatRupiah(totalCompletedRevenue)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // CSV Export Action
                FilledTonalButton(
                    onClick = {
                        CsvExportUtils.exportAndShareTransactionsCsv(
                            context = context,
                            transactions = filteredTransactions,
                            storeName = storeSettings?.storeName,
                            fileNamePrefix = "Riwayat_Transaksi_${selectedDateFilter.name}"
                        )
                    },
                    enabled = filteredTransactions.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_transactions_csv_button")
                ) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = "Ekspor CSV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor CSV", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (hasActiveFilters) "Tidak ada transaksi yang cocok" else "Tidak ada riwayat transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasActiveFilters) "Coba sesuaikan kata kunci pencarian, metode bayar, atau rentang tanggal." else "Transaksi dari menu Kasir akan otomatis tercatat di sini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedStatus = "Semua"
                                selectedPayment = "Semua"
                                selectedDateFilter = DateFilterType.ALL
                            }
                        ) {
                            Text("Reset Semua Filter")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("transactions_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTransactions, key = { it.transaction.id }) { trxWithItems ->
                    TransactionItemCard(
                        trxWithItems = trxWithItems,
                        onClick = { viewModel.openTransactionDetail(trxWithItems) },
                        onLongClick = { longPressedTransaction = trxWithItems },
                        onViewReceipt = { viewModel.openTransactionDetail(trxWithItems) },
                        onVoidTransaction = { viewModel.openVoidDialog(trxWithItems) }
                    )
                }
            }
        }
    }

    // Long Press Action Menu Dialog
    if (longPressedTransaction != null) {
        val trx = longPressedTransaction!!
        AlertDialog(
            onDismissRequest = { longPressedTransaction = null },
            title = {
                Text(
                    text = "Aksi Transaksi: ${trx.transaction.transactionNumber}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total: ${FormatUtils.formatRupiah(trx.transaction.totalAmount)} (${trx.items.size} item)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pilih tindakan yang ingin dilakukan pada data transaksi ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: View Line Items / Receipt Detail
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val item = trx
                                longPressedTransaction = null
                                viewModel.openTransactionDetail(item)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Lihat Rincian Item (Struk)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Lihat detail item, harga, diskon, dan bagikan struk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Export this transaction to CSV
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val item = trx
                                longPressedTransaction = null
                                CsvExportUtils.exportAndShareTransactionsCsv(
                                    context = context,
                                    transactions = listOf(item),
                                    storeName = storeSettings?.storeName,
                                    fileNamePrefix = "Transaksi_${item.transaction.transactionNumber}"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Ekspor Transaksi ke CSV",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Simpan rincian data transaksi ini ke format CSV/Excel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 3: Delete Transaction
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosErrorContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val item = trx
                                longPressedTransaction = null
                                transactionToDelete = item
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = PosError
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Hapus Riwayat Transaksi",
                                    fontWeight = FontWeight.Bold,
                                    color = PosError,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Hapus catatan transaksi ini secara permanen dari database",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressedTransaction = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        val trx = transactionToDelete!!
        var restoreStockCheckbox by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = PosError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Transaksi?")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus catatan transaksi ${trx.transaction.transactionNumber} senilai ${FormatUtils.formatRupiah(trx.transaction.totalAmount)} secara permanen?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreStockCheckbox = !restoreStockCheckbox }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = restoreStockCheckbox,
                                onCheckedChange = { restoreStockCheckbox = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kembalikan stok produk (${trx.items.sumOf { it.quantity }} unit) ke gudang",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = trx
                        val restore = restoreStockCheckbox
                        transactionToDelete = null
                        viewModel.deleteTransaction(toDelete, restoreStock = restore)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosError)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Custom Date Range Picker Dialog
    if (showCustomDateDialog) {
        CustomDateRangePickerDialog(
            initialStart = customStartDate,
            initialEnd = customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onApply = { start, end ->
                customStartDate = start
                customEndDate = end
                selectedDateFilter = DateFilterType.CUSTOM
                showCustomDateDialog = false
            }
        )
    }

    // Detail Receipt Dialog
    if (uiState.selectedTransactionDetail != null) {
        ReceiptDialog(
            transactionWithItems = uiState.selectedTransactionDetail!!,
            storeSettings = storeSettings,
            onDismiss = { viewModel.dismissTransactionDetail() },
            onSaveStoreSettings = { viewModel.saveStoreSettings(it) }
        )
    }

    // Void Transaction Dialog
    if (uiState.isVoidDialogOpen && uiState.voidingTransaction != null) {
        VoidTransactionDialog(
            trxWithItems = uiState.voidingTransaction!!,
            onDismiss = { viewModel.dismissVoidDialog() },
            onConfirm = { reason ->
                viewModel.confirmVoidTransaction(reason)
            }
        )
    }
}

@Composable
fun CustomDateRangePickerDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    var startDaysAgo by remember {
        val days = ((System.currentTimeMillis() - initialStart) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        mutableStateOf(days)
    }
    var endDaysAgo by remember {
        val days = ((System.currentTimeMillis() - initialEnd) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        mutableStateOf(days)
    }

    val computedStart = remember(startDaysAgo) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -startDaysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.timeInMillis
    }

    val computedEnd = remember(endDaysAgo) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -endDaysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.timeInMillis
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Rentang Tanggal", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Tentukan periode awal dan akhir riwayat transaksi yang ingin ditampilkan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Presets inside Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "3 Hari" to 3,
                        "7 Hari" to 7,
                        "14 Hari" to 14,
                        "30 Hari" to 30
                    )
                    presets.forEach { (label, days) ->
                        OutlinedButton(
                            onClick = {
                                startDaysAgo = days - 1
                                endDaysAgo = 0
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider()

                // Tanggal Mulai
                Column {
                    Text(
                        text = "Tanggal Mulai:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateFormat.format(Date(computedStart)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                IconButton(
                                    onClick = { startDaysAgo = (startDaysAgo + 1).coerceAtMost(365) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("-1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { startDaysAgo = (startDaysAgo - 1).coerceAtLeast(endDaysAgo) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Tanggal Selesai
                Column {
                    Text(
                        text = "Tanggal Selesai:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateFormat.format(Date(computedEnd)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                IconButton(
                                    onClick = { endDaysAgo = (endDaysAgo + 1).coerceAtMost(startDaysAgo) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("-1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { endDaysAgo = (endDaysAgo - 1).coerceAtLeast(0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(computedStart, computedEnd) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Terapkan Rentang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemCard(
    trxWithItems: TransactionWithItems,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onViewReceipt: () -> Unit,
    onVoidTransaction: () -> Unit
) {
    val trx = trxWithItems.transaction
    val isVoided = trx.status == "VOIDED"
    val itemsSummary = trxWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVoided) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("transaction_item_${trx.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Transaction Number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = trx.transactionNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = FormatUtils.formatDate(trx.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isVoided) PosErrorContainer else PosSuccessContainer
                ) {
                    Text(
                        text = if (isVoided) "VOID / BATAL" else "SELESAI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isVoided) PosOnErrorContainer else PosOnSuccessContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer & Payment Method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pelanggan: ${trx.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = trx.paymentMethod,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Items Preview
            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (isVoided && trx.voidReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alasan Batal: ${trx.voidReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosError
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Total Amount & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Tagihan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatUtils.formatRupiah(trx.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isVoided) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isVoided) {
                        OutlinedButton(
                            onClick = onVoidTransaction,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosError),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batal / Void", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Struk", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VoidTransactionDialog(
    trxWithItems: TransactionWithItems,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Salah input kasir / pelanggan batalkan") }

    val commonReasons = listOf(
        "Salah input kasir",
        "Pelanggan batalkan pesanan",
        "Barang rusak / tidak sesuai",
        "Salah metode pembayaran",
        "Lainnya"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = PosError)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batalkan Transaksi (Void)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Anda akan membatalkan transaksi ${trxWithItems.transaction.transactionNumber} senilai ${FormatUtils.formatRupiah(trxWithItems.transaction.totalAmount)}.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PosWarningContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Perhatian: Seluruh item (${trxWithItems.items.sumOf { it.quantity }} unit) akan otomatis dikembalikan ke stok inventaris.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF78350F),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Text("Pilih alasan pembatalan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    commonReasons.forEach { r ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (reason == r) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reason = r }
                        ) {
                            Text(
                                text = r,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (reason == r) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Keterangan Alasan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = PosError)
            ) {
                Text("Ya, Batalkan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kembali")
            }
        }
    )
}

package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.dao.PaymentSummary
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.model.ReportPeriod
import com.example.ui.components.DailySalesTrendDashboard
import com.example.ui.components.LowStockDashboardAlertWidget
import com.example.ui.components.StatCard
import com.example.ui.components.TopSellingProductsDashboard
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.viewmodel.PosScreen
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import androidx.compose.material.icons.filled.TableChart
import com.example.util.CsvExportUtils
import com.example.util.FormatUtils

@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    periodTransactions: List<TransactionWithItems>,
    periodRevenue: Double,
    periodProfit: Double,
    topProducts: List<TopProductSummary>,
    paymentSummaries: List<PaymentSummary>,
    categorySummaries: List<CategorySalesSummary>,
    storeSettings: StoreSettingsEntity?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProducts by viewModel.allActiveProducts.collectAsState()
    val completedTransactions = periodTransactions.filter { it.transaction.status == "COMPLETED" }
    val transactionCount = completedTransactions.size
    val totalItemsSold = completedTransactions.sumOf { tx -> tx.items.sumOf { it.quantity } }
    val avgBasketSize = if (transactionCount > 0) periodRevenue / transactionCount else 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Low Stock Dashboard Alert Widget
        item {
            LowStockDashboardAlertWidget(
                products = allProducts,
                onRestockClick = { product ->
                    viewModel.openStockAdjustment(product)
                },
                onViewAllInventory = {
                    viewModel.navigateTo(PosScreen.INVENTORY)
                }
            )
        }

        // Period Selector Filter Chips
        item {
            Column {
                Text(
                    text = "Laporan Penjualan Real-Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Data kalkulasi akurat & mutakhir langsung dari basis data offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReportPeriod.values()) { period ->
                        val isSelected = uiState.reportPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setReportPeriod(period) },
                            label = { Text(period.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Key KPI Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Omset",
                        value = FormatUtils.formatRupiah(periodRevenue),
                        subtitle = "${uiState.reportPeriod.label}",
                        icon = Icons.Default.MonetizationOn,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Laba Bersih",
                        value = FormatUtils.formatRupiah(periodProfit),
                        subtitle = "Estimasi margin untung",
                        icon = Icons.Default.TrendingUp,
                        iconTint = PosSuccess,
                        iconBackground = PosSuccessContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Jumlah Transaksi",
                        value = "$transactionCount Struk",
                        subtitle = "Transaksi sukses",
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Rata-rata Struk",
                        value = FormatUtils.formatRupiah(avgBasketSize),
                        subtitle = "$totalItemsSold total produk terjual",
                        icon = Icons.Default.ShoppingBag,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Summary Dashboard: Daily Sales Trend Chart (Interactive Bar & Spline Curve)
        item {
            DailySalesTrendDashboard(
                transactions = completedTransactions
            )
        }

        // Summary Dashboard: Top-Selling Items Visual Ranking (with proportional share bars)
        item {
            TopSellingProductsDashboard(
                topProducts = topProducts
            )
        }

        // Payment Method Breakdown
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Distribusi Metode Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (paymentSummaries.isEmpty()) {
                        Text(
                            text = "Belum ada data pembayaran.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val totalPayAmount = paymentSummaries.sumOf { it.totalAmount }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            paymentSummaries.forEach { pay ->
                                val pct = if (totalPayAmount > 0) (pay.totalAmount / totalPayAmount).toFloat() else 0f
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${pay.paymentMethod} (${pay.count} trx)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(pay.totalAmount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Sales Breakdown
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Penjualan Berdasarkan Kategori",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (categorySummaries.isEmpty()) {
                        Text(
                            text = "Belum ada data kategori.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categorySummaries.forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cat.category, fontWeight = FontWeight.SemiBold)
                                        Text("${cat.totalQuantity} unit terjual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        FormatUtils.formatRupiah(cat.totalSales),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        // Export / Share Report Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary: Export to CSV File (Spreadsheet / Excel)
                Button(
                    onClick = {
                        CsvExportUtils.exportAndShareSalesReportCsv(
                            context = context,
                            periodName = uiState.reportPeriod.label,
                            transactions = completedTransactions,
                            totalRevenue = periodRevenue,
                            totalProfit = periodProfit,
                            topProducts = topProducts,
                            storeName = storeSettings?.storeName
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_csv_report_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Laporan ke Berkas CSV", fontWeight = FontWeight.Bold)
                }

                // Secondary: Share Plain Text Summary
                OutlinedButton(
                    onClick = {
                        val reportText = buildString {
                            append("================================\n")
                            append("      LAPORAN PENJUALAN POS\n")
                            append("  ${storeSettings?.storeName ?: "Toko Kasir"}\n")
                            append("================================\n")
                            append("Periode     : ${uiState.reportPeriod.label}\n")
                            append("Waktu Cetak : ${FormatUtils.formatDate(System.currentTimeMillis())}\n")
                            append("--------------------------------\n")
                            append("Total Omset       : ${FormatUtils.formatRupiah(periodRevenue)}\n")
                            append("Total Laba Bersih : ${FormatUtils.formatRupiah(periodProfit)}\n")
                            append("Jumlah Transaksi  : $transactionCount struk\n")
                            append("Item Terjual      : $totalItemsSold unit\n")
                            append("Rata-rata/Struk   : ${FormatUtils.formatRupiah(avgBasketSize)}\n")
                            append("--------------------------------\n")
                            if (topProducts.isNotEmpty()) {
                                append("PRODUK TERLARIS:\n")
                                topProducts.take(5).forEachIndexed { i, p ->
                                    append("${i + 1}. ${p.productName} (${p.totalSold}x) - ${FormatUtils.formatRupiah(p.totalRevenue)}\n")
                                }
                                append("--------------------------------\n")
                            }
                            append("Kasir POS System - 100% Offline\n")
                            append("================================\n")
                        }

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Ringkasan Teks Laporan"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_report_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Ringkasan Teks", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TopProductRow(rank: Int, item: TopProductSummary) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = when (rank) {
                        1 -> Color(0xFFF59E0B)
                        2 -> Color(0xFF94A3B8)
                        3 -> Color(0xFFD97706)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.totalSold} terjual (${item.category})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatRupiah(item.totalRevenue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Laba: +${FormatUtils.formatRupiah(item.totalProfit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosSuccess
                )
            }
        }
    }
}

@Composable
fun SalesBarChart(
    transactions: List<TransactionWithItems>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.primaryContainer

    // Take up to 7 latest transactions or daily buckets
    val sampleTransactions = transactions.take(8).reversed()
    val maxAmount = sampleTransactions.maxOfOrNull { it.transaction.totalAmount } ?: 1.0

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val barCount = sampleTransactions.size
            if (barCount == 0) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height
            val spacing = 16.dp.toPx()
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(16.dp.toPx())

            sampleTransactions.forEachIndexed { i, trx ->
                val amount = trx.transaction.totalAmount
                val heightRatio = (amount / maxAmount).toFloat().coerceIn(0.1f, 1f)
                val barHeight = canvasHeight * heightRatio * 0.85f
                val x = spacing + i * (barWidth + spacing)
                val y = canvasHeight - barHeight

                // Draw rounded bar
                drawRoundRect(
                    color = if (i == sampleTransactions.lastIndex) primaryColor else secondaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Horizontal bottom label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sampleTransactions.forEach { trx ->
                Text(
                    text = FormatUtils.formatTimeOnly(trx.transaction.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 9.sp
                )
            }
        }
    }
}

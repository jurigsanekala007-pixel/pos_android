package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosOnErrorContainer
import com.example.ui.theme.PosOnSuccessContainer
import com.example.ui.theme.PosOnWarningContainer
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.theme.PosWarning
import com.example.ui.theme.PosWarningContainer
import com.example.util.FormatUtils

enum class InventorySortOption(val label: String) {
    VALUATION_DESC("Nilai Stok Tertinggi"),
    STOCK_ASC("Stok Paling Sedikit"),
    STOCK_DESC("Stok Paling Banyak"),
    NAME_ASC("Nama Produk (A-Z)"),
    UPDATED_DESC("Pembaruan Terakhir")
}

@Composable
fun InventoryReportView(
    products: List<ProductEntity>,
    threshold: Int,
    filterOption: String,
    onThresholdChange: (Int) -> Unit,
    onFilterChange: (String) -> Unit,
    onRestock: (ProductEntity) -> Unit,
    onEdit: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortOption by remember { mutableStateOf(InventorySortOption.VALUATION_DESC) }
    var showThresholdControls by remember { mutableStateOf(true) }

    // Aggregate Valuation Calculations
    val totalCostValuation = remember(products) {
        products.sumOf { it.stock * it.costPrice }
    }
    val totalPotentialRevenue = remember(products) {
        products.sumOf { it.stock * it.sellingPrice }
    }
    val totalPotentialProfit = remember(products) {
        products.sumOf { (it.sellingPrice - it.costPrice) * it.stock }
    }
    val totalUnits = remember(products) {
        products.sumOf { it.stock }
    }
    val lowStockCount = remember(products, threshold) {
        products.count { it.stock <= threshold }
    }
    val outOfStockCount = remember(products) {
        products.count { it.stock <= 0 }
    }

    // Filter products
    val filteredProducts = remember(products, filterOption, threshold, sortOption) {
        val filtered = when (filterOption) {
            "LOW_STOCK" -> products.filter { it.stock <= threshold }
            "OUT_OF_STOCK" -> products.filter { it.stock <= 0 }
            "HEALTHY" -> products.filter { it.stock > threshold }
            else -> products
        }

        when (sortOption) {
            InventorySortOption.VALUATION_DESC -> filtered.sortedByDescending { it.stock * it.costPrice }
            InventorySortOption.STOCK_ASC -> filtered.sortedBy { it.stock }
            InventorySortOption.STOCK_DESC -> filtered.sortedByDescending { it.stock }
            InventorySortOption.NAME_ASC -> filtered.sortedBy { it.name }
            InventorySortOption.UPDATED_DESC -> filtered.sortedByDescending { it.updatedAt }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laporan Valuasi & Nilai Stok",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audit komprehensif aset barang & batas stok minimum",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            val report = buildString {
                                append("========================================\n")
                                append("     LAPORAN VALUASI INVENTARIS STOK\n")
                                append("========================================\n")
                                append("Waktu Audit : ${FormatUtils.formatDate(System.currentTimeMillis())}\n")
                                append("Total SKU   : ${products.size} varian\n")
                                append("Total Fisik : $totalUnits unit barang\n")
                                append("Ambang Batas Minimum: $threshold unit\n")
                                append("----------------------------------------\n")
                                append("NILAI TOTAL MODAL ASET : ${FormatUtils.formatRupiah(totalCostValuation)}\n")
                                append("POTENSI NILAI JUAL     : ${FormatUtils.formatRupiah(totalPotentialRevenue)}\n")
                                append("ESTIMASI MARGIN LABA   : ${FormatUtils.formatRupiah(totalPotentialProfit)}\n")
                                append("----------------------------------------\n")
                                append("DAFTAR ITEM INVENTARIS:\n")
                                filteredProducts.forEachIndexed { i, p ->
                                    val itemVal = p.stock * p.costPrice
                                    val status = when {
                                        p.stock <= 0 -> "[HABIS]"
                                        p.stock <= threshold -> "[DI BAWAH AMBANG BATAS]"
                                        else -> "[AMAN]"
                                    }
                                    val updateDate = FormatUtils.formatDate(p.updatedAt)
                                    append("${i + 1}. ${p.name} (SKU: ${p.sku}) $status\n")
                                    append("   Stok: ${p.stock} ${p.unit} | Modal: ${FormatUtils.formatRupiah(p.costPrice)} | Jual: ${FormatUtils.formatRupiah(p.sellingPrice)}\n")
                                    append("   Nilai Total Stok: ${FormatUtils.formatRupiah(itemVal)} | Update: $updateDate\n\n")
                                }
                                append("========================================\n")
                                append("Generated by Kasir POS Offline System\n")
                            }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, report)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Inventaris"))
                        },
                        modifier = Modifier.testTag("share_inventory_report_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Laporan", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Valuation KPI Summary Cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Nilai Modal Aset Stok (Cost Valuation)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = FormatUtils.formatRupiah(totalCostValuation),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Potensi Nilai Jual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(FormatUtils.formatRupiah(totalPotentialRevenue), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column {
                            Text("Estimasi Laba Aset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text("+${FormatUtils.formatRupiah(totalPotentialProfit)}", fontWeight = FontWeight.Bold, color = PosSuccess)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Fisik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text("$totalUnits Unit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        // Configurable Minimum Stock Threshold Control Panel
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (lowStockCount > 0) PosWarningContainer else PosSuccessContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = if (lowStockCount > 0) PosWarning else PosSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Konfigurasi Ambang Batas Minimum",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tandai item dengan stok <= $threshold unit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Number controller (+ / -)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onThresholdChange(threshold - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "$threshold Unit",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onThresholdChange(threshold + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider for fast threshold adjustments
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { onThresholdChange(it.toInt()) },
                        valueRange = 0f..50f,
                        steps = 49,
                        modifier = Modifier.fillMaxWidth().testTag("threshold_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Quick Threshold Preset Chips
                    val presets = listOf(3, 5, 10, 15, 20, 30)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(presets) { p ->
                            val isSelected = threshold == p
                            FilterChip(
                                selected = isSelected,
                                onClick = { onThresholdChange(p) },
                                label = { Text("$p Unit", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter by Threshold Status Chips
                    Text("Filter Status Stok:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterOption == "ALL",
                                onClick = { onFilterChange("ALL") },
                                label = { Text("Semua (${products.size})") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterOption == "LOW_STOCK",
                                onClick = { onFilterChange("LOW_STOCK") },
                                label = { Text("⚠️ Di Bawah Ambang Batas ($lowStockCount)") },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosWarningContainer,
                                    selectedLabelColor = PosOnWarningContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterOption == "OUT_OF_STOCK",
                                onClick = { onFilterChange("OUT_OF_STOCK") },
                                label = { Text("❌ Habis ($outOfStockCount)") },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosErrorContainer,
                                    selectedLabelColor = PosOnErrorContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterOption == "HEALTHY",
                                onClick = { onFilterChange("HEALTHY") },
                                label = { Text("✅ Stok Aman (${products.size - lowStockCount})") },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosSuccessContainer,
                                    selectedLabelColor = PosOnSuccessContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sort Options & Item Count Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${filteredProducts.size} Produk",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Sort Chip Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        val allOptions = InventorySortOption.values()
                        val nextIndex = (sortOption.ordinal + 1) % allOptions.size
                        sortOption = allOptions[nextIndex]
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sortOption.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Detailed Products Valuation List
        if (filteredProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada item yang sesuai dengan filter", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                InventoryReportCard(
                    product = product,
                    threshold = threshold,
                    onRestock = { onRestock(product) },
                    onEdit = { onEdit(product) }
                )
            }
        }
    }
}

@Composable
fun InventoryReportCard(
    product: ProductEntity,
    threshold: Int,
    onRestock: () -> Unit,
    onEdit: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    val isBelowThreshold = product.stock <= threshold
    val totalCostValue = product.stock * product.costPrice
    val totalSellingValue = product.stock * product.sellingPrice
    val totalProfitPotential = (product.sellingPrice - product.costPrice) * product.stock

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOutOfStock -> PosErrorContainer.copy(alpha = 0.25f)
                isBelowThreshold -> PosWarningContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_report_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Product Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kategori: ${product.category} • SKU: ${if (product.sku.isNotBlank()) product.sku else "-"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Configurable Threshold Alert Tag
                val (badgeBg, badgeFg, label) = when {
                    isOutOfStock -> Triple(PosErrorContainer, PosOnErrorContainer, "STOK HABIS")
                    isBelowThreshold -> Triple(PosWarningContainer, PosOnWarningContainer, "DI BAWAH AMBANG BATAS")
                    else -> Triple(PosSuccessContainer, PosOnSuccessContainer, "STOK AMAN")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBelowThreshold) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = badgeFg,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Comprehensive Metrics Grid: 4 Essential Columns
            // 1. Kuantitas Saat Ini, 2. Harga Pokok & Jual, 3. Nilai Total Stok, 4. Tanggal Update
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Col 1: Kuantitas
                Column {
                    Text("Kuantitas Fisik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${product.stock} ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isBelowThreshold) PosWarning else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Col 2: Harga Pokok (Cost Price)
                Column {
                    Text("Harga Pokok", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatUtils.formatRupiah(product.costPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Col 3: Nilai Total Stok (Total Cost Value)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Nilai Total Stok", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatUtils.formatRupiah(totalCostValue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary row: Harga Jual Satuan, Potensi Nilai Jual, Tanggal Terakhir Update
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Update: ${FormatUtils.formatDate(product.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = "Potensi Jual: ${FormatUtils.formatRupiah(totalSellingValue)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Restock langsung & Edit Produk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Data", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRestock,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBelowThreshold) PosWarning else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restock / Sesuaikan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

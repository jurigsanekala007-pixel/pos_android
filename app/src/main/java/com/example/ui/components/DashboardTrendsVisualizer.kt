package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TrendBucket(
    val dateLabel: String,
    val fullDate: String,
    val totalRevenue: Double,
    val totalProfit: Double,
    val transactionCount: Int
)

@Composable
fun DailySalesTrendDashboard(
    transactions: List<TransactionWithItems>,
    modifier: Modifier = Modifier
) {
    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }
    var chartMode by remember { mutableStateOf("BAR") } // "BAR" or "LINE"
    var timeGranularity by remember { mutableStateOf("WEEKLY") } // "WEEKLY", "DAILY", or "MONTHLY"

    // Group transactions according to selected granularity
    val trendBuckets = remember(transactions, timeGranularity) {
        val completed = transactions.filter { it.transaction.status == "COMPLETED" }

        when (timeGranularity) {
            "WEEKLY" -> {
                // Generate consecutive 7 days for current/last week
                val dayFormat = SimpleDateFormat("EEE\ndd", Locale("id", "ID"))
                val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val calendar = Calendar.getInstance()
                // Last 7 days including today
                val buckets = mutableListOf<TrendBucket>()
                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                    }
                    val dateKey = keyFormat.format(cal.time)
                    val dayTrx = completed.filter {
                        keyFormat.format(Date(it.transaction.timestamp)) == dateKey
                    }
                    val rev = dayTrx.sumOf { it.transaction.totalAmount }
                    val profit = dayTrx.sumOf { tx -> tx.items.sumOf { item -> item.profit } }

                    buckets.add(
                        TrendBucket(
                            dateLabel = dayFormat.format(cal.time),
                            fullDate = fullDateFormat.format(cal.time),
                            totalRevenue = rev,
                            totalProfit = profit,
                            transactionCount = dayTrx.size
                        )
                    )
                }
                buckets
            }
            "DAILY" -> {
                val dateFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
                val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                val grouped = completed.groupBy {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.transaction.timestamp))
                }

                if (grouped.isEmpty()) {
                    emptyList()
                } else {
                    grouped.entries.sortedBy { it.key }.takeLast(10).map { entry ->
                        val list = entry.value
                        val firstTs = list.first().transaction.timestamp
                        val rev: Double = list.sumOf { it.transaction.totalAmount }
                        val profit: Double = list.sumOf { tx ->
                            tx.items.sumOf { item -> item.profit }
                        }
                        TrendBucket(
                            dateLabel = dateFormat.format(Date(firstTs)),
                            fullDate = fullDateFormat.format(Date(firstTs)),
                            totalRevenue = rev,
                            totalProfit = profit,
                            transactionCount = list.size
                        )
                    }
                }
            }
            else -> {
                val monthFormat = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
                val fullMonthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                val grouped = completed.groupBy {
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.transaction.timestamp))
                }

                if (grouped.isEmpty()) {
                    emptyList()
                } else {
                    grouped.entries.sortedBy { it.key }.takeLast(6).map { entry ->
                        val list = entry.value
                        val firstTs = list.first().transaction.timestamp
                        val rev: Double = list.sumOf { it.transaction.totalAmount }
                        val profit: Double = list.sumOf { tx ->
                            tx.items.sumOf { item -> item.profit }
                        }
                        TrendBucket(
                            dateLabel = monthFormat.format(Date(firstTs)),
                            fullDate = "Bulan ${fullMonthFormat.format(Date(firstTs))}",
                            totalRevenue = rev,
                            totalProfit = profit,
                            transactionCount = list.size
                        )
                    }
                }
            }
        }
    }

    val maxRevenue = remember(trendBuckets) {
        trendBuckets.maxOfOrNull { it.totalRevenue }?.coerceAtLeast(10000.0) ?: 10000.0
    }

    val peakPeriod = remember(trendBuckets) {
        trendBuckets.filter { it.totalRevenue > 0 }.maxByOrNull { it.totalRevenue } ?: trendBuckets.lastOrNull()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val profitColor = PosSuccess
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_sales_trend_dashboard")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (timeGranularity) {
                                "WEEKLY" -> "Tren Penjualan Mingguan"
                                "DAILY" -> "Tren Penjualan Harian"
                                else -> "Tren Penjualan Bulanan"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (chartMode == "BAR") "Grafik Batang (D3/Recharts Bar)" else "Grafik Garis (D3/Recharts Area)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mode Toggle (Bar vs Line)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = surfaceVariantColor.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (chartMode == "BAR") primaryColor else Color.Transparent,
                            modifier = Modifier
                                .clickable { chartMode = "BAR" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Grafik Batang",
                                tint = if (chartMode == "BAR") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (chartMode == "LINE") primaryColor else Color.Transparent,
                            modifier = Modifier
                                .clickable { chartMode = "LINE" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = "Grafik Garis",
                                tint = if (chartMode == "LINE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time Granularity Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "WEEKLY" to "Mingguan (7 Hari)",
                    "DAILY" to "Harian",
                    "MONTHLY" to "Bulanan"
                ).forEach { (gran, label) ->
                    val isSelected = timeGranularity == gran
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) primaryContainerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, primaryColor) else null,
                        modifier = Modifier
                            .clickable {
                                timeGranularity = gran
                                selectedBucketIndex = null
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trendBuckets.isEmpty() || trendBuckets.all { it.totalRevenue <= 0 && it.transactionCount == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Belum ada riwayat transaksi pada rentang ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Interactive Selected Tooltip Box
                val activeBucket = selectedBucketIndex?.let { trendBuckets.getOrNull(it) } ?: peakPeriod
                if (activeBucket != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeBucket.fullDate,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${activeBucket.transactionCount} transaksi struk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Omset: ${FormatUtils.formatRupiah(activeBucket.totalRevenue)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "Laba: +${FormatUtils.formatRupiah(activeBucket.totalProfit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = profitColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Native Chart Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(trendBuckets) {
                                detectTapGestures { offset ->
                                    val bucketCount = trendBuckets.size
                                    if (bucketCount > 0) {
                                        val segmentWidth = size.width / bucketCount
                                        val clickedIndex = (offset.x / segmentWidth).toInt().coerceIn(0, bucketCount - 1)
                                        selectedBucketIndex = clickedIndex
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val bucketCount = trendBuckets.size
                        val bottomPadding = 24.dp.toPx()
                        val chartHeight = canvasHeight - bottomPadding

                        // Draw Grid lines
                        val gridLines = 3
                        for (g in 0..gridLines) {
                            val y = chartHeight * (g.toFloat() / gridLines)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        if (chartMode == "BAR") {
                            // Bar Chart Mode
                            val barSpacing = 8.dp.toPx()
                            val barWidth = ((canvasWidth - (barSpacing * (bucketCount + 1))) / bucketCount).coerceIn(12.dp.toPx(), 40.dp.toPx())

                            trendBuckets.forEachIndexed { index, bucket ->
                                val revRatio = if (maxRevenue > 0) (bucket.totalRevenue / maxRevenue).toFloat().coerceIn(0.04f, 1f) else 0.04f
                                val profitRatio = if (maxRevenue > 0) (bucket.totalProfit.coerceAtLeast(0.0) / maxRevenue).toFloat().coerceIn(0.02f, revRatio) else 0.02f

                                val revBarHeight = if (bucket.totalRevenue > 0) chartHeight * revRatio * 0.9f else 4.dp.toPx()
                                val profitBarHeight = if (bucket.totalProfit > 0) chartHeight * profitRatio * 0.9f else 0f

                                val centerX = barSpacing + index * (barWidth + barSpacing) + (barWidth / 2)
                                val x = centerX - (barWidth / 2)
                                val yRev = chartHeight - revBarHeight
                                val yProfit = chartHeight - profitBarHeight

                                val isSelected = (selectedBucketIndex == index) || (selectedBucketIndex == null && bucket == peakPeriod)

                                // Draw Revenue Bar
                                drawRoundRect(
                                    color = if (isSelected) primaryColor else primaryContainerColor,
                                    topLeft = Offset(x, yRev),
                                    size = Size(barWidth, revBarHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )

                                // Draw Profit overlay
                                if (profitBarHeight > 0) {
                                    drawRoundRect(
                                        color = profitColor.copy(alpha = if (isSelected) 0.85f else 0.45f),
                                        topLeft = Offset(x + (barWidth * 0.15f), yProfit),
                                        size = Size(barWidth * 0.7f, profitBarHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        } else {
                            // Line / Spline Area Chart Mode (Recharts / D3 styling)
                            val points = trendBuckets.mapIndexed { index, bucket ->
                                val x = if (bucketCount > 1) {
                                    (index.toFloat() / (bucketCount - 1)) * (canvasWidth - 32.dp.toPx()) + 16.dp.toPx()
                                } else {
                                    canvasWidth / 2
                                }
                                val ratio = if (maxRevenue > 0) (bucket.totalRevenue / maxRevenue).toFloat().coerceIn(0.05f, 1f) else 0.05f
                                val y = chartHeight - (chartHeight * ratio * 0.85f)
                                Offset(x, y)
                            }

                            // Gradient Area
                            val areaPath = Path().apply {
                                if (points.isNotEmpty()) {
                                    moveTo(points.first().x, chartHeight)
                                    points.forEach { lineTo(it.x, it.y) }
                                    lineTo(points.last().x, chartHeight)
                                    close()
                                }
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.38f),
                                        primaryColor.copy(alpha = 0.02f)
                                    ),
                                    startY = 0f,
                                    endY = chartHeight
                                )
                            )

                            // Stroke Line
                            val linePath = Path().apply {
                                if (points.isNotEmpty()) {
                                    moveTo(points.first().x, points.first().y)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                }
                            }

                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw point markers
                            points.forEachIndexed { i, pt ->
                                val isSelected = (selectedBucketIndex == i) || (selectedBucketIndex == null && trendBuckets[i] == peakPeriod)
                                drawCircle(
                                    color = if (isSelected) primaryColor else Color.White,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = pt,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Horizontal bottom date labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trendBuckets.forEachIndexed { idx, bucket ->
                        val isSelected = (selectedBucketIndex == idx) || (selectedBucketIndex == null && bucket == peakPeriod)
                        Text(
                            text = bucket.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { selectedBucketIndex = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = primaryColor, modifier = Modifier.size(8.dp)) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Total Omset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.width(16.dp))

                    Surface(shape = CircleShape, color = profitColor, modifier = Modifier.size(8.dp)) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Laba Bersih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TopSellingProductsDashboard(
    topProducts: List<TopProductSummary>,
    modifier: Modifier = Modifier
) {
    val totalRevenueAll = remember(topProducts) {
        topProducts.sumOf { it.totalRevenue }.coerceAtLeast(1.0)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_selling_products_dashboard")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Peringkat Produk Terlaris (Top Selling)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (topProducts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Top ${topProducts.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (topProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada produk yang terjual pada periode ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topProducts.forEachIndexed { index, item ->
                        val rank = index + 1
                        val sharePct = ((item.totalRevenue / totalRevenueAll) * 100).toInt()
                        val progress = (item.totalRevenue / totalRevenueAll).toFloat().coerceIn(0.05f, 1f)

                        val medalColor = when (rank) {
                            1 -> Color(0xFFF59E0B) // Gold
                            2 -> Color(0xFF94A3B8) // Silver
                            3 -> Color(0xFFD97706) // Bronze
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = medalColor,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
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
                                                text = "${item.totalSold} unit terjual • ${item.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FormatUtils.formatRupiah(item.totalRevenue),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Untung: +${FormatUtils.formatRupiah(item.totalProfit)} ($sharePct% share)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PosSuccess,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Proportional share progress bar
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (rank == 1) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

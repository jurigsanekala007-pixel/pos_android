package com.example.data.model

import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.dao.PaymentSummary
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems

enum class ReportPeriod(val label: String) {
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    ALL_TIME("Semua")
}

data class DashboardReportState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val transactionCount: Int = 0,
    val totalItemsSold: Int = 0,
    val averageBasketSize: Double = 0.0,
    val lowStockCount: Int = 0,
    val topProducts: List<TopProductSummary> = emptyList(),
    val paymentSummaries: List<PaymentSummary> = emptyList(),
    val categorySummaries: List<CategorySalesSummary> = emptyList(),
    val transactions: List<TransactionWithItems> = emptyList()
)

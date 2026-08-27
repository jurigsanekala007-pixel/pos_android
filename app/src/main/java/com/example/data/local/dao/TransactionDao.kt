package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)

data class PaymentSummary(
    val paymentMethod: String,
    val count: Int,
    val totalAmount: Double
)

data class CategorySalesSummary(
    val category: String,
    val totalQuantity: Int,
    val totalSales: Double
)

data class TopProductSummary(
    val productId: Long,
    val productName: String,
    val category: String,
    val totalSold: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class DailySalesStat(
    val dateString: String,
    val totalSales: Double,
    val totalProfit: Double,
    val transactionCount: Int
)

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsWithItemsSync(): List<TransactionWithItems>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionWithItemsById(id: Long): TransactionWithItems?

    @Transaction
    @Query("SELECT * FROM transactions WHERE transactionNumber = :trxNumber LIMIT 1")
    suspend fun getTransactionByNumber(trxNumber: String): TransactionWithItems?

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>): List<Long>

    @Query("UPDATE transactions SET status = 'VOIDED', voidReason = :reason WHERE id = :transactionId")
    suspend fun voidTransaction(transactionId: Long, reason: String)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteTransactionItems(transactionId: Long)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'COMPLETED' AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    fun getCompletedTransactionCount(startTimestamp: Long, endTimestamp: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM transactions WHERE status = 'COMPLETED' AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    fun getTotalRevenue(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalProfit), 0.0) FROM transactions WHERE status = 'COMPLETED' AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    fun getTotalProfit(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("""
        SELECT ti.productId, ti.productName, ti.category, 
               SUM(ti.quantity) as totalSold, 
               SUM(ti.subtotal - ti.itemDiscount) as totalRevenue, 
               SUM(ti.profit) as totalProfit
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.status = 'COMPLETED' AND t.timestamp >= :startTimestamp AND t.timestamp <= :endTimestamp
        GROUP BY ti.productId, ti.productName
        ORDER BY totalSold DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(startTimestamp: Long, endTimestamp: Long, limit: Int = 10): Flow<List<TopProductSummary>>

    @Query("""
        SELECT t.paymentMethod, COUNT(t.id) as count, SUM(t.totalAmount) as totalAmount
        FROM transactions t
        WHERE t.status = 'COMPLETED' AND t.timestamp >= :startTimestamp AND t.timestamp <= :endTimestamp
        GROUP BY t.paymentMethod
    """)
    fun getPaymentMethodBreakdown(startTimestamp: Long, endTimestamp: Long): Flow<List<PaymentSummary>>

    @Query("""
        SELECT ti.category, SUM(ti.quantity) as totalQuantity, SUM(ti.subtotal - ti.itemDiscount) as totalSales
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.status = 'COMPLETED' AND t.timestamp >= :startTimestamp AND t.timestamp <= :endTimestamp
        GROUP BY ti.category
        ORDER BY totalSales DESC
    """)
    fun getCategorySalesBreakdown(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySalesSummary>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int
}

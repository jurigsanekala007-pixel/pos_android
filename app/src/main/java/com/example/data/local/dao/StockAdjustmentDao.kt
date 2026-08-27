package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StockAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    suspend fun getAllAdjustmentsSync(): List<StockAdjustmentEntity>

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId ORDER BY timestamp DESC")
    fun getAdjustmentsByProduct(productId: Long): Flow<List<StockAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustments(adjustments: List<StockAdjustmentEntity>): List<Long>
}

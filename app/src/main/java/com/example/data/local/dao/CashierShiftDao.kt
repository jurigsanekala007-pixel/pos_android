package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CashierShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashierShiftDao {

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    fun getActiveShift(): Flow<CashierShiftEntity?>

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveShiftSync(): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<CashierShiftEntity>>

    @Query("SELECT * FROM cashier_shifts ORDER BY startTime DESC")
    suspend fun getAllShiftsSync(): List<CashierShiftEntity>

    @Query("SELECT * FROM cashier_shifts WHERE id = :id")
    suspend fun getShiftById(id: Long): CashierShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashierShiftEntity): Long

    @Update
    suspend fun updateShift(shift: CashierShiftEntity)

    @Query("DELETE FROM cashier_shifts")
    suspend fun deleteAllShifts()
}

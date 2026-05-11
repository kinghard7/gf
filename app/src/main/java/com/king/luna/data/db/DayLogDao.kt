package com.king.luna.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.king.luna.data.entity.DayLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayLogDao {

    @Query("SELECT date FROM day_log")
    fun observeDates(): Flow<List<LocalDate>>

    @Query("SELECT * FROM day_log ORDER BY date ASC")
    fun observeAll(): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM day_log WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): DayLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DayLogEntity)

    @Query("DELETE FROM day_log WHERE date = :date")
    suspend fun delete(date: LocalDate)
}

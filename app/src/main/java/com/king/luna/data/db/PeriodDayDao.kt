package com.king.luna.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.king.luna.data.entity.PeriodDayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PeriodDayDao {

    @Query("SELECT * FROM period_day ORDER BY date ASC")
    fun observeAll(): Flow<List<PeriodDayEntity>>

    @Query("SELECT * FROM period_day WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): PeriodDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeriodDayEntity)

    @Query("DELETE FROM period_day WHERE date = :date")
    suspend fun delete(date: LocalDate)
}

package com.king.luna.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity

@Database(
    entities = [PeriodDayEntity::class, DayLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LunaDatabase : RoomDatabase() {

    abstract fun periodDayDao(): PeriodDayDao
    abstract fun dayLogDao(): DayLogDao

    companion object {
        fun build(context: Context): LunaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LunaDatabase::class.java,
                "luna.db"
            ).build()
    }
}

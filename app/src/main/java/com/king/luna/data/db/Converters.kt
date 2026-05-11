package com.king.luna.data.db

import androidx.room.TypeConverter
import com.king.luna.domain.model.FlowLevel
import java.time.LocalDate

// Room 类型转换：LocalDate <-> ISO 字符串；FlowLevel <-> name
class Converters {

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(s: String?): LocalDate? = s?.let(LocalDate::parse)

    @TypeConverter
    fun flowLevelToString(level: FlowLevel?): String? = level?.name

    @TypeConverter
    fun stringToFlowLevel(s: String?): FlowLevel? = s?.let(FlowLevel::valueOf)
}

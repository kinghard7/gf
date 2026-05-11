package com.king.luna.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.king.luna.domain.model.FlowLevel
import java.time.LocalDate

// 一天的经期状态；date 为主键，不存日历空白日
@Entity(tableName = "period_day")
data class PeriodDayEntity(
    @PrimaryKey val date: LocalDate,
    val flow: FlowLevel
)

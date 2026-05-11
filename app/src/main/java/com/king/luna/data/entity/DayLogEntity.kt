package com.king.luna.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// 当日心情/症状/笔记；moods、symptoms 用 CSV，简单粗暴避免引入关联表
@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey val date: LocalDate,
    val moods: String,       // 逗号分隔的 Mood.name；空字符串表示无
    val symptoms: String,    // 逗号分隔的 Symptom.name；空字符串表示无
    val note: String         // 用户备注；可为空字符串
)

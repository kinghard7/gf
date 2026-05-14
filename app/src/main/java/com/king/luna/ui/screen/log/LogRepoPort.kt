package com.king.luna.ui.screen.log

import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import java.time.LocalDate

// LogViewModel 仅需要的最小仓库面;便于单元测试替换
interface LogRepoPort {
    suspend fun getPeriodDay(date: LocalDate): PeriodDayEntity?
    suspend fun getDayLog(date: LocalDate): DayLogEntity?
    suspend fun setPeriod(date: LocalDate, flow: FlowLevel)
    suspend fun setLog(date: LocalDate, moods: Set<Mood>, symptoms: Set<Symptom>, note: String)
}

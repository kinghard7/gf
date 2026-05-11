package com.king.luna.domain.model

import java.time.LocalDate

// CyclePredictor.predict() 的输出，纯值对象
data class CyclePrediction(
    val cycleStart: LocalDate?,
    val cycleDay: Int?,                    // 1-based；null 表示无历史
    val avgCycleLength: Int,               // 默认 28
    val avgPeriodLength: Int,              // 默认 5
    val nextPeriodStart: LocalDate?,
    val ovulationDay: LocalDate?,
    val fertileWindow: ClosedRange<LocalDate>?,
    val phase: Phase
)

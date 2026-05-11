package com.king.luna.domain.insight

import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

// V2.1 周期洞察的数据。纯派生，不写表。
data class InsightStats(
    val cycleBars: List<CycleBar>,        // 每段周期的长度（最多 12 段，最近的在前）
    val periodBars: List<PeriodBar>,      // 每段经期的长度
    val flowDays: Map<FlowLevel, Int>,    // 各档流量天数；不含 NONE
    val avgCycle: Int,
    val avgPeriod: Int,
    val cycleStdDev: Int,                 // 周期波动（天，整数）；< 0 表示样本不足
    val sampleSize: Int                   // 有几段完整周期可统计
) {
    val hasEnoughData: Boolean get() = sampleSize >= 1

    companion object {
        private const val WINDOW = 12
        private const val DEFAULT_CYCLE = 28
        private const val DEFAULT_PERIOD = 5

        fun from(periods: List<PeriodDayEntity>): InsightStats {
            val sortedDates = periods.map { it.date }.distinct().sorted()
            val segments = splitSegments(sortedDates)
            val starts = segments.map { it.first() }

            // 周期长度 = 相邻 start 的天数差
            val cycleLens = starts.zipWithNext { a, b ->
                ChronoUnit.DAYS.between(a, b).toInt()
            }
            // 经期长度 = 段首末差 + 1
            val periodLens = segments.map {
                ChronoUnit.DAYS.between(it.first(), it.last()).toInt() + 1
            }

            // 取最近 WINDOW 段
            val cycleBars = cycleLens.takeLast(WINDOW)
                .mapIndexed { i, len -> CycleBar(start = starts[i], lengthDays = len) }
                .reversed()  // UI 习惯：最近的在最前
            val periodBars = periodLens.takeLast(WINDOW)
                .mapIndexed { i, len -> PeriodBar(start = starts[i], lengthDays = len) }
                .reversed()

            val avgCycle = cycleLens.takeLast(WINDOW).averageOr(DEFAULT_CYCLE)
            val avgPeriod = periodLens.takeLast(WINDOW).averageOr(DEFAULT_PERIOD)
            val stdDev = cycleLens.takeLast(WINDOW).stdDevOrNegOne()

            // 各档流量天数；NONE 不计
            val flowDays = FlowLevel.values()
                .filter { it != FlowLevel.NONE }
                .associateWith { lv -> periods.count { it.flow == lv } }

            return InsightStats(
                cycleBars = cycleBars,
                periodBars = periodBars,
                flowDays = flowDays,
                avgCycle = avgCycle,
                avgPeriod = avgPeriod,
                cycleStdDev = stdDev,
                sampleSize = cycleLens.size
            )
        }

        // 与 CyclePredictor 同口径：相邻日差 ≤ 2 算同段
        private fun splitSegments(sorted: List<LocalDate>): List<List<LocalDate>> {
            val out = mutableListOf<MutableList<LocalDate>>()
            for (d in sorted) {
                val last = out.lastOrNull()?.lastOrNull()
                if (last == null || ChronoUnit.DAYS.between(last, d) > 2) {
                    out.add(mutableListOf(d))
                } else {
                    out.last().add(d)
                }
            }
            return out
        }

        private fun List<Int>.averageOr(default: Int): Int =
            if (isEmpty()) default else (sum().toDouble() / size).roundToInt()

        // 样本不足返回 -1，让 UI 显示"数据不足"而不是骗人的 0
        private fun List<Int>.stdDevOrNegOne(): Int {
            if (size < 2) return -1
            val mean = sum().toDouble() / size
            val variance = sumOf { (it - mean) * (it - mean) } / size
            return sqrt(variance).roundToInt()
        }
    }
}

data class CycleBar(val start: LocalDate, val lengthDays: Int)
data class PeriodBar(val start: LocalDate, val lengthDays: Int)

package com.king.luna.domain.cycle

import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.Phase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CyclePredictor {

    private const val DEFAULT_CYCLE = 28
    private const val DEFAULT_PERIOD = 5
    private const val WINDOW = 6           // 取最近 N 次
    private const val OVULATION_OFFSET = 14
    private const val FERTILE_PAD = 2

    // todayIsPeriod: 今天是否有经期记录，由调用方从 periodDayDao 判断
    fun predict(periodDays: List<LocalDate>, today: LocalDate, todayIsPeriod: Boolean = false): CyclePrediction {
        if (periodDays.isEmpty()) {
            return CyclePrediction(
                cycleStart = null,
                cycleDay = null,
                avgCycleLength = DEFAULT_CYCLE,
                avgPeriodLength = DEFAULT_PERIOD,
                nextPeriodStart = null,
                ovulationDay = null,
                fertileWindow = null,
                phase = Phase.UNKNOWN
            )
        }

        val sorted = periodDays.distinct().sorted()
        val segments = splitIntoSegments(sorted)
        val starts = segments.map { it.first() }
        val periodLengths = segments.map { segmentLength(it) }

        val cycleIntervals = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        val avgCycle = cycleIntervals.takeLast(WINDOW).averageOrDefault(DEFAULT_CYCLE)
        val avgPeriod = periodLengths.takeLast(WINDOW).averageOrDefault(DEFAULT_PERIOD)

        val cycleStart = starts.last()
        val nextStart = cycleStart.plusDays(avgCycle.toLong())
        val ovulation = nextStart.minusDays(OVULATION_OFFSET.toLong())
        val fertile = ovulation.minusDays(FERTILE_PAD.toLong())..ovulation.plusDays(FERTILE_PAD.toLong())

        val cycleDay = (ChronoUnit.DAYS.between(cycleStart, today).toInt() + 1)
            .takeIf { it >= 1 }
        val hasPeriodRecordToday = todayIsPeriod || today in sorted
        val phase = derivePhase(today, hasPeriodRecordToday, segments.last(), ovulation, fertile, nextStart)

        return CyclePrediction(
            cycleStart = cycleStart,
            cycleDay = cycleDay,
            avgCycleLength = avgCycle,
            avgPeriodLength = avgPeriod,
            nextPeriodStart = nextStart,
            ovulationDay = ovulation,
            fertileWindow = fertile,
            phase = phase
        )
    }

    // 中间最多空 1 天（相邻天数差 ≤ 2）算同段；段长度 = 末日 - 首日 + 1
    private fun splitIntoSegments(sorted: List<LocalDate>): List<List<LocalDate>> {
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

    private fun segmentLength(seg: List<LocalDate>): Int =
        (ChronoUnit.DAYS.between(seg.first(), seg.last()).toInt() + 1)

    private fun List<Int>.averageOrDefault(default: Int): Int =
        if (isEmpty()) default else (sum() / size).coerceAtLeast(1)

    private fun derivePhase(
        today: LocalDate,
        todayIsPeriod: Boolean,
        currentSegment: List<LocalDate>,
        ovulation: LocalDate,
        fertile: ClosedRange<LocalDate>,
        nextStart: LocalDate
    ): Phase {
        // 今天有经期记录才算经期，不能只靠 segment 日期范围
        if (todayIsPeriod) return Phase.PERIOD
        // 排卵窗口：先判排卵日，再判前后 2 天
        if (today == ovulation) return Phase.OVULATION
        if (today in fertile) return Phase.OVULATION
        // 经期结束 ~ 排卵窗口前：卵泡期
        if (today > currentSegment.last() && today < fertile.start) return Phase.FOLLICULAR
        // 排卵窗口后 ~ 下次经期前：黄体期
        if (today > fertile.endInclusive && today < nextStart) return Phase.LUTEAL
        // 越界（today 在历史之前 / 越过下次预测）：黄体期兜底
        return Phase.LUTEAL
    }
}

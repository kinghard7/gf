package com.king.luna.domain.insight

import com.google.common.truth.Truth.assertThat
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import org.junit.Test
import java.time.LocalDate

class InsightStatsTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun pd(date: String, flow: FlowLevel = FlowLevel.MEDIUM) =
        PeriodDayEntity(d(date), flow)

    @Test
    fun `空记录返回默认值且 sampleSize 为 0`() {
        val s = InsightStats.from(emptyList())
        assertThat(s.avgCycle).isEqualTo(28)
        assertThat(s.avgPeriod).isEqualTo(5)
        assertThat(s.sampleSize).isEqualTo(0)
        assertThat(s.cycleStdDev).isEqualTo(-1)  // 样本不足
        assertThat(s.hasEnoughData).isFalse()
    }

    @Test
    fun `单段经期没有完整周期 sampleSize 为 0 但 periodBar 有 1 条`() {
        val periods = listOf("2026-05-01", "2026-05-02", "2026-05-03").map { pd(it) }
        val s = InsightStats.from(periods)
        assertThat(s.sampleSize).isEqualTo(0)
        assertThat(s.periodBars).hasSize(1)
        assertThat(s.periodBars[0].lengthDays).isEqualTo(3)
        assertThat(s.cycleBars).isEmpty()
    }

    @Test
    fun `三段经期产生两个周期 计算平均与波动`() {
        // 段开始：5-01、5-29（28）、6-26（28）
        val dates = buildList {
            addAll(listOf("2026-05-01", "2026-05-02", "2026-05-03"))
            addAll(listOf("2026-05-29", "2026-05-30", "2026-05-31"))
            addAll(listOf("2026-06-26", "2026-06-27", "2026-06-28"))
        }
        val s = InsightStats.from(dates.map { pd(it) })
        assertThat(s.sampleSize).isEqualTo(2)
        assertThat(s.avgCycle).isEqualTo(28)
        assertThat(s.avgPeriod).isEqualTo(3)
        assertThat(s.cycleBars).hasSize(2)
        assertThat(s.cycleStdDev).isEqualTo(0)  // 两段都是 28
    }

    @Test
    fun `周期波动 不为 0 时正确计算`() {
        // 周期间隔 26、30 → 平均 28，波动 = sqrt((4+4)/2) = 2
        val dates = buildList {
            addAll(listOf("2026-01-01"))
            addAll(listOf("2026-01-27"))         // +26
            addAll(listOf("2026-02-26"))         // +30
        }
        val s = InsightStats.from(dates.map { pd(it) })
        assertThat(s.cycleStdDev).isEqualTo(2)
    }

    @Test
    fun `cycleBars 倒序 最近周期在最前`() {
        val dates = listOf("2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26")
        val s = InsightStats.from(dates.map { pd(it) })
        // 三段周期：1-29 - 1-1 = 28, 2-26 - 1-29 = 28, 3-26 - 2-26 = 28
        // 倒序：最近的"第三段周期"对应起点 2-26
        assertThat(s.cycleBars).hasSize(3)
        assertThat(s.cycleBars[0].start).isEqualTo(d("2026-02-26"))
    }

    @Test
    fun `flowDays 按等级正确计数 不含 NONE`() {
        val periods = listOf(
            pd("2026-05-01", FlowLevel.LIGHT),
            pd("2026-05-02", FlowLevel.MEDIUM),
            pd("2026-05-03", FlowLevel.MEDIUM),
            pd("2026-05-04", FlowLevel.HEAVY)
        )
        val s = InsightStats.from(periods)
        assertThat(s.flowDays[FlowLevel.LIGHT]).isEqualTo(1)
        assertThat(s.flowDays[FlowLevel.MEDIUM]).isEqualTo(2)
        assertThat(s.flowDays[FlowLevel.HEAVY]).isEqualTo(1)
        assertThat(s.flowDays).doesNotContainKey(FlowLevel.NONE)
    }

    @Test
    fun `仅取最近 12 段`() {
        // 构造 14 段（每段 1 天），周期 28
        val starts = (0 until 14).map { LocalDate.of(2025, 1, 1).plusDays(it * 28L) }
        val periods = starts.map { PeriodDayEntity(it, FlowLevel.MEDIUM) }
        val s = InsightStats.from(periods)
        // 14 段 → 13 个周期 → 取最近 12
        assertThat(s.cycleBars).hasSize(12)
        assertThat(s.periodBars).hasSize(12)
    }
}

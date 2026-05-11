package com.king.luna.domain.cycle

import com.google.common.truth.Truth.assertThat
import com.king.luna.domain.model.Phase
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `空记录使用默认 28 5 且 phase 为 UNKNOWN`() {
        val r = CyclePredictor.predict(emptyList(), today = d("2026-05-05"))
        assertThat(r.avgCycleLength).isEqualTo(28)
        assertThat(r.avgPeriodLength).isEqualTo(5)
        assertThat(r.cycleStart).isNull()
        assertThat(r.cycleDay).isNull()
        assertThat(r.nextPeriodStart).isNull()
        assertThat(r.phase).isEqualTo(Phase.UNKNOWN)
    }

    @Test
    fun `单段经期 5 天，今天是经期开始日，phase 为 PERIOD`() {
        val period = listOf("2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-01"))
        assertThat(r.cycleStart).isEqualTo(d("2026-05-01"))
        assertThat(r.cycleDay).isEqualTo(1)
        assertThat(r.avgPeriodLength).isEqualTo(5)
        assertThat(r.phase).isEqualTo(Phase.PERIOD)
        assertThat(r.nextPeriodStart).isEqualTo(d("2026-05-29"))
    }

    @Test
    fun `经期内最后一天 phase 仍为 PERIOD`() {
        val period = listOf("2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-05"))
        assertThat(r.cycleDay).isEqualTo(5)
        assertThat(r.phase).isEqualTo(Phase.PERIOD)
    }

    @Test
    fun `经期结束次日进入卵泡期`() {
        val period = listOf("2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-06"))
        assertThat(r.phase).isEqualTo(Phase.FOLLICULAR)
    }

    @Test
    fun `下次经期前 14 天为排卵日，phase 为 OVULATION`() {
        val period = listOf("2026-04-01", "2026-04-02", "2026-04-03", "2026-04-04", "2026-04-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-04-15"))
        assertThat(r.ovulationDay).isEqualTo(d("2026-04-15"))
        assertThat(r.phase).isEqualTo(Phase.OVULATION)
        assertThat(r.fertileWindow!!.start).isEqualTo(d("2026-04-13"))
        assertThat(r.fertileWindow!!.endInclusive).isEqualTo(d("2026-04-17"))
    }

    @Test
    fun `排卵日之后到下次经期之间为黄体期`() {
        val period = listOf("2026-04-01", "2026-04-02", "2026-04-03", "2026-04-04", "2026-04-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-04-22"))
        assertThat(r.phase).isEqualTo(Phase.LUTEAL)
    }

    @Test
    fun `多段经期取最近 6 段平均周期长度`() {
        val starts = listOf(
            "2026-01-01", "2026-01-26", "2026-02-21", "2026-03-20",
            "2026-04-17", "2026-05-16", "2026-06-15", "2026-07-16"
        )
        val period = starts.map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-07-16"))
        assertThat(r.avgCycleLength).isEqualTo(28)
    }

    @Test
    fun `经期日不连续超过 1 天则视为新一段`() {
        val period = listOf("2026-05-01", "2026-05-02", "2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-05"))
        assertThat(r.cycleStart).isEqualTo(d("2026-05-05"))
    }

    @Test
    fun `跨年边界 12-28 + 28 = 1-25`() {
        val period = listOf("2025-12-28", "2025-12-29", "2025-12-30").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2025-12-28"))
        assertThat(r.nextPeriodStart).isEqualTo(d("2026-01-25"))
    }

    @Test
    fun `经期段间隔恰为 1 天视为同段`() {
        val period = listOf("2026-05-01", "2026-05-02", "2026-05-04").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-04"))
        assertThat(r.cycleStart).isEqualTo(d("2026-05-01"))
        assertThat(r.avgPeriodLength).isEqualTo(4)
    }
}

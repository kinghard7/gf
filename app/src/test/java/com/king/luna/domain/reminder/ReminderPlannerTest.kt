package com.king.luna.domain.reminder

import com.google.common.truth.Truth.assertThat
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.Phase
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlannerTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun dt(s: String) = LocalDateTime.parse(s)

    private val basePrediction = CyclePrediction(
        cycleStart = d("2026-05-01"),
        cycleDay = 5,
        avgCycleLength = 28,
        avgPeriodLength = 5,
        nextPeriodStart = d("2026-05-29"),
        ovulationDay = d("2026-05-15"),
        fertileWindow = d("2026-05-13")..d("2026-05-17"),
        phase = Phase.PERIOD
    )

    @Test
    fun `全关返回空列表`() {
        val s = ReminderSettings(
            periodReminderEnabled = false,
            ovulationEnabled = false
        )
        val jobs = ReminderPlanner.plan(basePrediction, s, dt("2026-05-05T08:00"))
        assertThat(jobs).isEmpty()
    }

    @Test
    fun `默认提前0天即预测经期首日一条提醒`() {
        val s = ReminderSettings()
        val jobs = ReminderPlanner.plan(basePrediction, s, dt("2026-05-05T08:00"))
        assertThat(jobs.map { it.tag }).containsExactly("period_reminder")
        assertThat(jobs.single().triggerAt).isEqualTo(dt("2026-05-29T09:00"))
    }

    @Test
    fun `提前 2 天仅一条 period_reminder`() {
        val s = ReminderSettings(
            periodReminderEnabled = true,
            periodLeadDays = 2
        )
        val jobs = ReminderPlanner.plan(basePrediction, s, dt("2026-05-05T08:00"))
        assertThat(jobs).hasSize(1)
        val j = jobs.single()
        assertThat(j.tag).isEqualTo("period_reminder")
        assertThat(j.triggerAt).isEqualTo(dt("2026-05-27T09:00"))
    }

    @Test
    fun `经期与排卵都开排出两条`() {
        val s = ReminderSettings(
            periodReminderEnabled = true,
            ovulationEnabled = true,
            hourOfDay = 8,
            minuteOfHour = 30
        )
        val jobs = ReminderPlanner.plan(basePrediction, s, dt("2026-05-05T08:00"))
        assertThat(jobs.map { it.tag }).containsExactly("period_reminder", "ovulation")
        assertThat(jobs.first { it.tag == "period_reminder" }.triggerAt)
            .isEqualTo(dt("2026-05-29T08:30"))
        assertThat(jobs.first { it.tag == "ovulation" }.triggerAt)
            .isEqualTo(dt("2026-05-15T08:30"))
    }

    @Test
    fun `过去时间点被过滤`() {
        val s = ReminderSettings(ovulationEnabled = true, periodReminderEnabled = true)
        val jobs = ReminderPlanner.plan(basePrediction, s, dt("2026-05-20T10:00"))
        assertThat(jobs.map { it.tag }).containsExactly("period_reminder")
    }

    @Test
    fun `预测无 nextPeriodStart 不排经期任务`() {
        val empty = basePrediction.copy(nextPeriodStart = null, ovulationDay = null)
        val s = ReminderSettings(
            periodReminderEnabled = true,
            ovulationEnabled = true
        )
        val jobs = ReminderPlanner.plan(empty, s, dt("2026-05-05T08:00"))
        assertThat(jobs).isEmpty()
    }

    @Test
    fun `提前天数 clamp 到 0 到 7`() {
        val s = ReminderSettings(
            periodReminderEnabled = true,
            periodLeadDays = 99
        )
        val jobs = ReminderPlanner.plan(basePrediction, s, dt("2026-05-05T08:00"))
        assertThat(jobs.single().triggerAt).isEqualTo(dt("2026-05-22T09:00"))
    }
}

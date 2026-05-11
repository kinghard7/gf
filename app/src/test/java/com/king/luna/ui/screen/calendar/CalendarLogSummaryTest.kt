package com.king.luna.ui.screen.calendar

import com.google.common.truth.Truth.assertThat
import com.king.luna.data.entity.DayLogEntity
import org.junit.Test
import java.time.LocalDate

class CalendarLogSummaryTest {

    @Test
    fun `日志摘要显示当天心情症状和笔记内容`() {
        val entity = DayLogEntity(
            date = LocalDate.parse("2026-05-05"),
            moods = "HAPPY,TIRED",
            symptoms = "CRAMPS,HEADACHE",
            note = "今天肚子有点痛，晚上早点休息"
        )

        val summary = CalendarLogSummary.from(entity)

        assertThat(summary.date).isEqualTo(LocalDate.parse("2026-05-05"))
        assertThat(summary.moodText).isEqualTo("😊、😴")
        assertThat(summary.symptomText).isEqualTo("腹痛、头痛")
        assertThat(summary.noteText).isEqualTo("今天肚子有点痛，晚上早点休息")
    }
}

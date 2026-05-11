package com.king.luna.ui.screen.calendar

import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.repo.toMoodSet
import com.king.luna.data.repo.toSymptomSet
import java.time.LocalDate

data class CalendarLogSummary(
    val date: LocalDate,
    val moodText: String,
    val symptomText: String,
    val noteText: String
) {
    companion object {
        fun from(entity: DayLogEntity): CalendarLogSummary {
            val moods = entity.moods.toMoodSet().joinToString("、") { it.emoji }
            val symptoms = entity.symptoms.toSymptomSet().joinToString("、") { it.label }
            return CalendarLogSummary(
                date = entity.date,
                moodText = moods,
                symptomText = symptoms,
                noteText = entity.note
            )
        }
    }
}

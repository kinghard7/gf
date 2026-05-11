package com.king.luna.domain.reminder

import com.king.luna.domain.model.CyclePrediction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// 一条要在未来某刻触发的本地通知任务。
data class ReminderJob(
    val tag: String,
    val triggerAt: LocalDateTime,
    val title: String,
    val body: String
)

object ReminderPlanner {

    fun plan(
        prediction: CyclePrediction,
        settings: ReminderSettings,
        now: LocalDateTime
    ): List<ReminderJob> {
        if (!settings.anyEnabled) return emptyList()
        val time = LocalTime.of(
            settings.hourOfDay.coerceIn(0, 23),
            settings.minuteOfHour.coerceIn(0, 59)
        )
        val jobs = mutableListOf<ReminderJob>()

        if (settings.periodReminderEnabled) {
            prediction.nextPeriodStart?.let { nextStart ->
                val lead = settings.periodLeadDays.coerceIn(0, 7)
                val date = nextStart.minusDays(lead.toLong())
                val title = resolvedPeriodTitle(settings)
                val body = resolvedPeriodBody(settings)
                jobs += ReminderJob(
                    tag = "period_reminder",
                    triggerAt = LocalDateTime.of(date, time),
                    title = title,
                    body = body
                )
            }
        }

        prediction.ovulationDay?.let { ovu ->
            if (settings.ovulationEnabled) {
                jobs += ReminderJob(
                    tag = "ovulation",
                    triggerAt = LocalDateTime.of(ovu, time),
                    title = resolvedOvulationTitle(settings),
                    body = resolvedOvulationBody(settings)
                )
            }
        }

        return jobs.filter { it.triggerAt.isAfter(now) }
    }

    fun resolvedPeriodTitle(settings: ReminderSettings): String =
        settings.periodTitle.ifBlank {
            if (settings.periodLeadDays == 0) "今天可能来经期" else "经期临近"
        }

    fun resolvedPeriodBody(settings: ReminderSettings): String =
        settings.periodBody.ifBlank {
            val lead = settings.periodLeadDays.coerceIn(0, 7)
            if (lead == 0) "记得记录流量，多喝温水。"
            else "预计 $lead 天后开始，记得准备好。"
        }

    fun resolvedOvulationTitle(settings: ReminderSettings): String =
        settings.ovulationTitle.ifBlank { "排卵日提醒" }

    fun resolvedOvulationBody(settings: ReminderSettings): String =
        settings.ovulationBody.ifBlank {
            "今天是预计排卵日，处于高生育力窗口。"
        }

    @Suppress("unused")
    fun emptyDate(): LocalDate = LocalDate.MIN
}

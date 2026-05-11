package com.king.luna.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.king.luna.domain.reminder.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "luna_settings")

class SettingsRepository(private val context: Context) {

    private object K {
        val PER_REM_EN = booleanPreferencesKey("period_reminder_enabled")
        val PERIOD_LEAD = intPreferencesKey("period_lead_days")
        val PER_TITLE = stringPreferencesKey("period_title")
        val PER_BODY = stringPreferencesKey("period_body")

        val LEGACY_PRE_EN = booleanPreferencesKey("period_pre_enabled")
        val LEGACY_PRE_LEAD = intPreferencesKey("period_pre_lead_days")
        val LEGACY_START_EN = booleanPreferencesKey("period_start_enabled")
        val LEGACY_PRE_TITLE = stringPreferencesKey("period_pre_title")
        val LEGACY_PRE_BODY = stringPreferencesKey("period_pre_body")
        val LEGACY_START_TITLE = stringPreferencesKey("period_start_title")
        val LEGACY_START_BODY = stringPreferencesKey("period_start_body")

        val OVU_ENABLED = booleanPreferencesKey("ovulation_enabled")
        val HOUR = intPreferencesKey("hour_of_day")
        val MINUTE = intPreferencesKey("minute_of_hour")
        val OVU_TITLE = stringPreferencesKey("ovulation_title")
        val OVU_BODY = stringPreferencesKey("ovulation_body")
    }

    val flow: Flow<ReminderSettings> = context.dataStore.data.map { p ->
        val legacyMergedEnabled =
            (p[K.LEGACY_START_EN] ?: true) || (p[K.LEGACY_PRE_EN] ?: false)
        val periodReminderEnabled = p[K.PER_REM_EN] ?: legacyMergedEnabled

        val periodLeadDays = (p[K.PERIOD_LEAD] ?: run {
            if (p[K.LEGACY_PRE_EN] == true) {
                (p[K.LEGACY_PRE_LEAD] ?: 2).coerceIn(1, 7)
            } else {
                0
            }
        }).coerceIn(0, 7)

        val periodTitle = (p[K.PER_TITLE] ?: "").ifBlank {
            (p[K.LEGACY_START_TITLE] ?: "").ifBlank { p[K.LEGACY_PRE_TITLE] ?: "" }
        }
        val periodBody = (p[K.PER_BODY] ?: "").ifBlank {
            (p[K.LEGACY_START_BODY] ?: "").ifBlank { p[K.LEGACY_PRE_BODY] ?: "" }
        }

        ReminderSettings(
            periodReminderEnabled = periodReminderEnabled,
            periodLeadDays = periodLeadDays,
            ovulationEnabled = p[K.OVU_ENABLED] ?: false,
            hourOfDay = (p[K.HOUR] ?: 9).coerceIn(0, 23),
            minuteOfHour = (p[K.MINUTE] ?: 0).coerceIn(0, 59),
            periodTitle = periodTitle,
            periodBody = periodBody,
            ovulationTitle = p[K.OVU_TITLE] ?: "",
            ovulationBody = p[K.OVU_BODY] ?: ""
        )
    }

    suspend fun update(transform: (ReminderSettings) -> ReminderSettings) {
        context.dataStore.edit { p ->
            val cur = ReminderSettings(
                periodReminderEnabled = p[K.PER_REM_EN]
                    ?: ((p[K.LEGACY_START_EN] ?: true) || (p[K.LEGACY_PRE_EN] ?: false)),
                periodLeadDays = (p[K.PERIOD_LEAD] ?: run {
                    if (p[K.LEGACY_PRE_EN] == true) {
                        (p[K.LEGACY_PRE_LEAD] ?: 2).coerceIn(1, 7)
                    } else {
                        0
                    }
                }).coerceIn(0, 7),
                ovulationEnabled = p[K.OVU_ENABLED] ?: false,
                hourOfDay = (p[K.HOUR] ?: 9).coerceIn(0, 23),
                minuteOfHour = (p[K.MINUTE] ?: 0).coerceIn(0, 59),
                periodTitle = (p[K.PER_TITLE] ?: "").ifBlank {
                    (p[K.LEGACY_START_TITLE] ?: "").ifBlank { p[K.LEGACY_PRE_TITLE] ?: "" }
                },
                periodBody = (p[K.PER_BODY] ?: "").ifBlank {
                    (p[K.LEGACY_START_BODY] ?: "").ifBlank { p[K.LEGACY_PRE_BODY] ?: "" }
                },
                ovulationTitle = p[K.OVU_TITLE] ?: "",
                ovulationBody = p[K.OVU_BODY] ?: ""
            )
            val next = transform(cur)
            p[K.PER_REM_EN] = next.periodReminderEnabled
            p[K.PERIOD_LEAD] = next.periodLeadDays.coerceIn(0, 7)
            p[K.PER_TITLE] = next.periodTitle
            p[K.PER_BODY] = next.periodBody
            p[K.OVU_ENABLED] = next.ovulationEnabled
            p[K.HOUR] = next.hourOfDay.coerceIn(0, 23)
            p[K.MINUTE] = next.minuteOfHour.coerceIn(0, 59)
            p[K.OVU_TITLE] = next.ovulationTitle
            p[K.OVU_BODY] = next.ovulationBody
        }
    }
}

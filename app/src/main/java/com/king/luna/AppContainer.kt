package com.king.luna

import android.content.Context
import com.king.luna.data.db.LunaDatabase
import com.king.luna.data.repo.CycleRepository
import com.king.luna.data.settings.SettingsRepository
import com.king.luna.notification.ReminderScheduler

// 手撸 DI 容器：避免 Hilt 的注解开销
class AppContainer(context: Context) {

    private val appCtx = context.applicationContext
    private val db: LunaDatabase = LunaDatabase.build(appCtx)

    val cycleRepository: CycleRepository =
        CycleRepository(db.periodDayDao(), db.dayLogDao())

    val settingsRepository: SettingsRepository = SettingsRepository(appCtx)

    val reminderScheduler: ReminderScheduler =
        ReminderScheduler(appCtx, cycleRepository, settingsRepository)
}

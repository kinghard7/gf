package com.king.luna

import android.app.Application
import com.king.luna.notification.LunaNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LunaApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        LunaNotificationChannel.ensure(this)
        container.reminderScheduler.start(applicationScope)
    }
}

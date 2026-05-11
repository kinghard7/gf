package com.king.luna.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object LunaNotificationChannel {

    const val CHANNEL_ID = "luna_reminders"
    private const val CHANNEL_NAME = "周期提醒"
    private const val CHANNEL_DESC = "经期 / 排卵相关的本地提醒"

    /** 震动节奏：delay、震动(ms)、pause、震动…（与系统默认相近的短震两下） */
    val vibrationPattern: LongArray = longArrayOf(0, 280, 120, 280)

    // App 启动时调用一次。重复调用幂等（系统侧 createNotificationChannel 是 upsert）。
    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pattern = vibrationPattern
        val ch = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            vibrationPattern = pattern
        }
        mgr.createNotificationChannel(ch)
    }
}

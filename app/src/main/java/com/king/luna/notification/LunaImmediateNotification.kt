package com.king.luna.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.king.luna.MainActivity
import com.king.luna.R

/** 立即弹出一条通知（不走 WorkManager）。 */
object LunaImmediateNotification {

    private const val TAG_IMMEDIATE = "luna_immediate"

    /**
     * @return 是否已成功调用 [NotificationManagerCompat.notify]（未授权时返回 false）
     */
    fun show(context: Context, title: String, body: String): Boolean {
        val ctx = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        LunaNotificationChannel.ensure(ctx)

        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx,
            TAG_IMMEDIATE.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val t = title.trim().ifEmpty { "Luna" }
        val b = body.trim().ifEmpty { " " }

        val notif = NotificationCompat.Builder(ctx, LunaNotificationChannel.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(t)
            .setContentText(b)
            .setStyle(NotificationCompat.BigTextStyle().bigText(b))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    setVibrate(LunaNotificationChannel.vibrationPattern)
                }
            }
            .build()

        NotificationManagerCompat.from(ctx).notify(TAG_IMMEDIATE.hashCode(), notif)
        return true
    }
}

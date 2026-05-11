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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.king.luna.MainActivity
import com.king.luna.R

// 通知 Worker。被 WorkManager 在 setInitialDelay 到点后调用。
// 入参从 inputData 取（tag/title/body）；不依赖 App 当时的内存状态。
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val body = inputData.getString(KEY_BODY) ?: return Result.success()
        val tag = inputData.getString(KEY_TAG) ?: "luna"

        val ctx = applicationContext

        // Android 13+ 没授权就直接放弃；不抛异常、不重试
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, tag.hashCode(), openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(ctx, LunaNotificationChannel.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    setVibrate(LunaNotificationChannel.vibrationPattern)
                }
            }
            .build()

        // tag.hashCode() 做 notificationId，不同 tag 不会互相覆盖
        NotificationManagerCompat.from(ctx).notify(tag.hashCode(), notif)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_TAG = "tag"
    }
}

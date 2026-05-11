package com.king.luna.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.king.luna.data.repo.CycleRepository
import com.king.luna.data.settings.SettingsRepository
import com.king.luna.domain.reminder.ReminderJob
import com.king.luna.domain.reminder.ReminderPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

// 监听 prediction × settings 变化；每次变化全删全建。
// 状态最少：WorkManager 自己维护排队，本类零内部状态。
class ReminderScheduler(
    private val context: Context,
    private val cycleRepo: CycleRepository,
    private val settingsRepo: SettingsRepository
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                cycleRepo.observePrediction(),
                settingsRepo.flow
            ) { prediction, settings ->
                ReminderPlanner.plan(prediction, settings, LocalDateTime.now())
            }
                .distinctUntilChanged()
                .collect { jobs -> reschedule(jobs) }
        }
    }

    private fun reschedule(jobs: List<ReminderJob>) {
        val wm = WorkManager.getInstance(context)
        // 用唯一 workName 替换：旧的自动取消，新的入队
        ALL_TAGS.forEach { wm.cancelUniqueWork(workName(it)) }
        val now = LocalDateTime.now()
        jobs.forEach { job ->
            val delay = Duration.between(now, job.triggerAt).toMillis()
            if (delay <= 0) return@forEach
            val data = Data.Builder()
                .putString(ReminderWorker.KEY_TAG, job.tag)
                .putString(ReminderWorker.KEY_TITLE, job.title)
                .putString(ReminderWorker.KEY_BODY, job.body)
                .build()
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(job.tag)
                .build()
            wm.enqueueUniqueWork(workName(job.tag), ExistingWorkPolicy.REPLACE, req)
        }
    }

    private fun workName(tag: String) = "luna_reminder_$tag"

    companion object {
        // 与 ReminderPlanner 输出的 tag 一一对应；改这里要同步改 Planner
        // 含旧 tag，升级后取消残留 WorkRequest
        private val ALL_TAGS = listOf(
            "period_reminder", "ovulation",
            "period_pre", "period_start"
        )
    }
}

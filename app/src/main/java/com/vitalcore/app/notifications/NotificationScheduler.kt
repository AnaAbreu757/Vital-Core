package com.vitalcore.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the four notification types via WorkManager. High-strain and
 * low-recovery alerts are event-driven (triggered from HomeViewModel after a
 * fresh calculation) rather than time-scheduled here; morning summary and
 * sleep reminder are daily periodic work.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun scheduleMorningSummary(enabled: Boolean, at: LocalTime = LocalTime.of(7, 30)) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_MORNING_SUMMARY)
            return
        }
        val initialDelay = delayUntil(at)
        val request = PeriodicWorkRequestBuilder<MorningSummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_MORNING_SUMMARY, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelAll() = workManager.cancelAllWorkByTag(TAG)

    private fun delayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(time)
        if (target.isBefore(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    companion object {
        private const val WORK_MORNING_SUMMARY = "morning_summary_work"
        private const val TAG = "vitalcore_notifications"
    }
}

package com.vitalcore.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vitalcore.app.R
import com.vitalcore.app.data.repositories.ScoreRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Posts the "Morning summary" notification (Recovery/Sleep for today) once
 * scheduled by [NotificationScheduler]. One worker class per notification
 * type keeps each concern small and independently testable/schedulable.
 */
@HiltWorker
class MorningSummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scoreRepository: ScoreRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val recovery = scoreRepository.recoveryFor(today)
        val sleep = scoreRepository.sleepFor(today)

        val text = buildString {
            append("Recovery ${recovery?.score ?: "--"}")
            append(" · Sleep ${sleep?.score ?: "--"}")
        }

        postNotification(
            channelId = NotificationChannels.MORNING_SUMMARY,
            notificationId = 1001,
            title = "Good morning",
            text = text,
        )
        return Result.success()
    }

    private fun postNotification(channelId: String, notificationId: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }
}

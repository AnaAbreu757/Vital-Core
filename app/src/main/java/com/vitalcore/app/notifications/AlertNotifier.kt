package com.vitalcore.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vitalcore.app.R
import com.vitalcore.app.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fires the two event-driven alerts (high strain, low recovery) right after
 * a fresh calculation, respecting the user's toggles in Settings. Unlike the
 * morning summary (time-scheduled via WorkManager), these are triggered
 * directly from [com.vitalcore.app.ui.screens.home.HomeViewModel] since they
 * depend on that calculation's result, not the clock.
 */
@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun maybeNotifyLowRecovery(score: Int) {
        val settings = settingsRepository.notificationSettings.first()
        if (!settings.lowRecovery || score >= 33) return
        notify(NotificationChannels.LOW_RECOVERY, 2001, "Low recovery today", "Your recovery is $score. Consider taking it easy.")
    }

    suspend fun maybeNotifyHighStrain(score: Double) {
        val settings = settingsRepository.notificationSettings.first()
        if (!settings.highStrain || score < 16.0) return
        notify(NotificationChannels.HIGH_STRAIN, 2002, "High strain today", "Your strain has reached %.1f.".format(score))
    }

    private fun notify(channelId: String, id: Int, title: String, text: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

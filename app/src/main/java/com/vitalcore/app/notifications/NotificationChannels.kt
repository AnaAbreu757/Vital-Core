package com.vitalcore.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val MORNING_SUMMARY = "morning_summary"
    const val SLEEP_REMINDER = "sleep_reminder"
    const val HIGH_STRAIN = "high_strain"
    const val LOW_RECOVERY = "low_recovery"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(MORNING_SUMMARY, "Morning summary", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(SLEEP_REMINDER, "Sleep reminder", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(HIGH_STRAIN, "High strain alerts", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(LOW_RECOVERY, "Low recovery alerts", NotificationManager.IMPORTANCE_HIGH),
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }
}

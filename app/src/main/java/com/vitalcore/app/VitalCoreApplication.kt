package com.vitalcore.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.vitalcore.app.auth.FirebaseInit
import com.vitalcore.app.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Annotated with @HiltAndroidApp so Hilt generates
 * the dependency graph used across the whole app (data, domain, ui).
 * Also implements Configuration.Provider so WorkManager workers (see
 * notifications/) get their dependencies injected via Hilt.
 */
@HiltAndroidApp
class VitalCoreApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        FirebaseInit.initialize(this)
    }
}

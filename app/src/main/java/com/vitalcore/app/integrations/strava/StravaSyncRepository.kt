package com.vitalcore.app.integrations.strava

import com.vitalcore.app.data.database.VitalCoreDatabase
import com.vitalcore.app.data.database.entities.ExerciseSessionEntity
import javax.inject.Inject
import javax.inject.Singleton

sealed class StravaSyncResult {
    data class Success(val importedCount: Int) : StravaSyncResult()
    data object NotConnected : StravaSyncResult()
    data class Error(val message: String) : StravaSyncResult()
}

/**
 * Pulls recent Strava activities and writes them into the same
 * `exercise_sessions` table Health Connect data lands in, so they flow into
 * StrainCalculator identically regardless of source. De-duplicated by start
 * timestamp (IGNORE conflict), same as every other data source in the app.
 */
@Singleton
class StravaSyncRepository @Inject constructor(
    private val authManager: StravaAuthManager,
    private val apiClient: StravaApiClient,
    private val settingsRepository: StravaSettingsRepository,
    private val db: VitalCoreDatabase,
) {
    suspend fun syncRecentActivities(daysBack: Long = 30): StravaSyncResult {
        val refreshResult = authManager.refreshIfNeeded()
        if (refreshResult is StravaAuthResult.Error) {
            return StravaSyncResult.Error(refreshResult.message)
        }
        val tokens = settingsRepository.currentTokens() ?: return StravaSyncResult.NotConnected

        return try {
            val after = System.currentTimeMillis() / 1000 - (daysBack * 86_400)
            val activities = apiClient.fetchRecentActivities(tokens.accessToken, after)

            val entities = activities.map { activity ->
                ExerciseSessionEntity(
                    startEpochMillis = activity.startDate.toEpochMilli(),
                    endEpochMillis = activity.startDate.plusSeconds(activity.elapsedSeconds).toEpochMilli(),
                    activityType = "STRAVA_${activity.type.uppercase()}",
                    avgHeartRate = activity.averageHeartRate,
                    maxHeartRate = activity.maxHeartRate,
                    calories = activity.calories,
                    distanceMeters = activity.distanceMeters,
                )
            }
            db.exerciseDao().insertAll(entities)
            settingsRepository.setLastSync(System.currentTimeMillis())

            StravaSyncResult.Success(entities.size)
        } catch (t: Throwable) {
            StravaSyncResult.Error(t.message ?: "Sync failed.")
        }
    }
}

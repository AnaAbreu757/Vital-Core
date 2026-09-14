package com.vitalcore.app.settings

import com.vitalcore.app.data.database.VitalCoreDatabase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ExportedRecoveryScore(val dateEpochDay: Long, val score: Int, val confidence: String)

@Serializable
data class ExportedSleepScore(val dateEpochDay: Long, val score: Int, val durationMinutes: Long)

@Serializable
data class ExportedStrainScore(val dateEpochDay: Long, val score: Double)

@Serializable
data class ExportBundle(
    val recovery: List<ExportedRecoveryScore>,
    val sleep: List<ExportedSleepScore>,
    val strain: List<ExportedStrainScore>,
)

/**
 * Privacy-first data controls: JSON/CSV export of everything VitalCore has
 * stored, and a full local wipe. No network call is ever made by this class —
 * both operations are 100% local, matching PRIVACY.md.
 */
@Singleton
class PrivacyExportManager @Inject constructor(
    private val db: VitalCoreDatabase,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun exportAsJson(): String {
        val bundle = buildExportBundle()
        return Json { prettyPrint = true }.encodeToString(ExportBundle.serializer(), bundle)
    }

    suspend fun exportAsCsv(): String {
        val bundle = buildExportBundle()
        val sb = StringBuilder()
        sb.appendLine("type,date_epoch_day,value_1,value_2")
        bundle.recovery.forEach { sb.appendLine("recovery,${it.dateEpochDay},${it.score},${it.confidence}") }
        bundle.sleep.forEach { sb.appendLine("sleep,${it.dateEpochDay},${it.score},${it.durationMinutes}") }
        bundle.strain.forEach { sb.appendLine("strain,${it.dateEpochDay},${it.score},") }
        return sb.toString()
    }

    suspend fun writeExportFile(content: String, file: File) {
        file.writeText(content)
    }

    /** Irreversible: wipes every Room table and all DataStore preferences. */
    suspend fun deleteAllData() {
        db.clearAllTables()
        settingsRepository.clearAll()
    }

    private suspend fun buildExportBundle(): ExportBundle {
        val farPast = -365_000L
        val farFuture = 365_000L
        val recovery = db.scoresDao().observeRecoveryRange(farPast, farFuture)
        val sleep = db.scoresDao().observeSleepRange(farPast, farFuture)
        val strain = db.scoresDao().observeStrainRange(farPast, farFuture)

        return ExportBundle(
            recovery = recovery.first().map { ExportedRecoveryScore(it.dateEpochDay, it.score, it.confidence) },
            sleep = sleep.first().map { ExportedSleepScore(it.dateEpochDay, it.score, it.durationMinutes) },
            strain = strain.first().map { ExportedStrainScore(it.dateEpochDay, it.score) },
        )
    }
}

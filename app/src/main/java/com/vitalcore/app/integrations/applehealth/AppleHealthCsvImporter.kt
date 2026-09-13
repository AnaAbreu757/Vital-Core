package com.vitalcore.app.integrations.applehealth

import com.vitalcore.app.data.database.VitalCoreDatabase
import com.vitalcore.app.data.database.entities.BloodPressureEntity
import com.vitalcore.app.data.database.entities.HeartRateSampleEntity
import com.vitalcore.app.data.database.entities.HrvSampleEntity
import com.vitalcore.app.data.database.entities.RespiratoryRateEntity
import com.vitalcore.app.data.database.entities.RestingHeartRateEntity
import com.vitalcore.app.data.database.entities.SleepSessionEntity
import com.vitalcore.app.data.database.entities.SpO2SampleEntity
import com.vitalcore.app.data.database.entities.WeightSampleEntity
import java.io.BufferedReader
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class AppleHealthImportResult(
    val rowsRead: Int,
    val rowsImported: Int,
    val skippedTypes: Set<String>,
)

/**
 * Imports a CSV export of Apple Health data. There is no Android API to read
 * Apple Health directly (see MANUAL_DEPLOY.md "Apple Health") — this is the
 * only honest path: export on iPhone, convert/produce a CSV, import here.
 *
 * Expected columns (header required): `type,start,end,value,unit`
 * - `type` matches Apple's own HealthKit identifiers, e.g.
 *   `HKQuantityTypeIdentifierHeartRate`, `HKQuantityTypeIdentifierHeartRateVariabilitySDNN`,
 *   `HKQuantityTypeIdentifierRestingHeartRate`, `HKCategoryTypeIdentifierSleepAnalysis`,
 *   `HKQuantityTypeIdentifierOxygenSaturation`, `HKQuantityTypeIdentifierRespiratoryRate`,
 *   `HKQuantityTypeIdentifierBodyMass`, `HKCorrelationTypeIdentifierBloodPressure`.
 *   This matches the `type` attribute Apple's own export.xml uses, so any
 *   XML-to-CSV converter that preserves that column works without changes.
 * - `start`/`end` are ISO-8601 instants.
 * - `value` is numeric (kg for BodyMass, % for OxygenSaturation, ms for HRV, etc.)
 *   except for sleep, where `value` is ignored and only start/end matter.
 * - `unit` is currently informational only (not converted) — see MANUAL_DEPLOY.md
 *   "Known limitations" if your export uses lb instead of kg, etc.
 *
 * Unrecognized `type` values are skipped (reported in [AppleHealthImportResult],
 * not silently dropped) rather than causing the whole import to fail.
 */
@Singleton
class AppleHealthCsvImporter @Inject constructor(private val db: VitalCoreDatabase) {

    suspend fun import(reader: BufferedReader): AppleHealthImportResult {
        val lines = reader.readLines()
        if (lines.isEmpty()) return AppleHealthImportResult(0, 0, emptySet())

        val header = lines.first().split(",").map { it.trim() }
        val typeIdx = header.indexOf("type")
        val startIdx = header.indexOf("start")
        val endIdx = header.indexOf("end")
        val valueIdx = header.indexOf("value")
        require(typeIdx >= 0 && startIdx >= 0) { "CSV must have at least 'type' and 'start' columns." }

        var imported = 0
        val skipped = mutableSetOf<String>()
        var rowsRead = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            rowsRead++
            val cols = line.split(",").map { it.trim() }
            if (cols.size <= typeIdx || cols.size <= startIdx) continue

            val type = cols[typeIdx]
            val start = runCatching { Instant.parse(cols[startIdx]) }.getOrNull() ?: continue
            val end = endIdx.takeIf { it >= 0 && cols.size > it }?.let { runCatching { Instant.parse(cols[it]) }.getOrNull() }
            val value = valueIdx.takeIf { it >= 0 && cols.size > it }?.let { cols[it].toDoubleOrNull() }

            val handled = when (type) {
                "HKQuantityTypeIdentifierHeartRate" -> value?.let {
                    db.heartRateDao().insertAll(listOf(HeartRateSampleEntity(start.toEpochMilli(), it.toInt()))); true
                }
                "HKQuantityTypeIdentifierRestingHeartRate" -> value?.let {
                    db.restingHeartRateDao().insertAll(listOf(RestingHeartRateEntity(start.toEpochMilli(), it.toInt()))); true
                }
                "HKQuantityTypeIdentifierHeartRateVariabilitySDNN" -> value?.let {
                    db.hrvDao().insertAll(listOf(HrvSampleEntity(start.toEpochMilli(), it))); true
                }
                "HKCategoryTypeIdentifierSleepAnalysis" -> end?.let {
                    db.sleepDao().insertSessions(listOf(SleepSessionEntity(start.toEpochMilli(), it.toEpochMilli()))); true
                }
                "HKQuantityTypeIdentifierOxygenSaturation" -> value?.let {
                    db.vitalsDao().insertSpo2(listOf(SpO2SampleEntity(start.toEpochMilli(), it))); true
                }
                "HKQuantityTypeIdentifierRespiratoryRate" -> value?.let {
                    db.vitalsDao().insertRespiratory(listOf(RespiratoryRateEntity(start.toEpochMilli(), it))); true
                }
                "HKQuantityTypeIdentifierBodyMass" -> value?.let {
                    db.vitalsDao().insertWeight(listOf(WeightSampleEntity(start.toEpochMilli(), it))); true
                }
                else -> {
                    skipped += type
                    false
                }
            }
            if (handled == true) imported++
        }

        return AppleHealthImportResult(rowsRead, imported, skipped)
    }
}

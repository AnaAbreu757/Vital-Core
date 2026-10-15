package com.vitalcore.app.ui.screens.settings

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.healthconnect.HealthConnectAvailability
import com.vitalcore.app.data.healthconnect.HealthConnectManager
import com.vitalcore.app.data.healthconnect.HealthConnectPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class PermissionStatus(val label: String, val granted: Boolean)

data class DiagnosticsState(
    val running: Boolean = false,
    val availability: HealthConnectAvailability? = null,
    val permissions: List<PermissionStatus> = emptyList(),
    val heartRateSamples7d: Int? = null,
    val sleepSessions7d: Int? = null,
    val stepsRecords7d: Int? = null,
)

/**
 * Turns "why is my data empty" from guesswork into direct visibility: for
 * every permission VitalCore can use, whether it's actually granted right
 * now, plus a live count of what Health Connect actually returns for a few
 * key record types over the last 7 days. See Settings > Health Connect
 * diagnostics.
 */
@HiltViewModel
class HealthConnectDiagnosticsViewModel @Inject constructor(
    private val healthConnect: HealthConnectManager,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    fun run() {
        viewModelScope.launch {
            _state.value = DiagnosticsState(running = true)

            val availability = healthConnect.availability()
            val granted = healthConnect.grantedPermissions()
            val permissionLabels = mapOf(
                HealthPermission.getReadPermission(HeartRateRecord::class) to "Heart rate (raw)",
                HealthPermission.getReadPermission(RestingHeartRateRecord::class) to "Resting heart rate",
                HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) to "Heart rate variability",
                HealthPermission.getReadPermission(SleepSessionRecord::class) to "Sleep",
                HealthPermission.getReadPermission(StepsRecord::class) to "Steps",
            )
            val permissions = HealthConnectPermissions.ALL.map { permission ->
                PermissionStatus(
                    label = permissionLabels[permission] ?: permission.substringAfterLast("."),
                    granted = permission in granted,
                )
            }.sortedBy { it.label }

            val end = Instant.now()
            val start = end.minus(7, ChronoUnit.DAYS)
            val heartRate = healthConnect.readHeartRate(start, end)
            val sleep = healthConnect.readSleep(start, end)

            _state.value = DiagnosticsState(
                running = false,
                availability = availability,
                permissions = permissions,
                heartRateSamples7d = heartRate.size,
                sleepSessions7d = sleep.size,
                stepsRecords7d = null, // readDailyActivity aggregates rather than listing records; omitted here
            )
        }
    }
}

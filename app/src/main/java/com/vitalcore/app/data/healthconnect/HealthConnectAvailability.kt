package com.vitalcore.app.data.healthconnect

/** Result of checking whether Health Connect can be used on this device right now. */
sealed class HealthConnectAvailability {
    data object Available : HealthConnectAvailability()
    data object NotInstalled : HealthConnectAvailability()
    data object UpdateRequired : HealthConnectAvailability()
}

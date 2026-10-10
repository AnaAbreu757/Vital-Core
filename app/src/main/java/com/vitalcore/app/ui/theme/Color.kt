package com.vitalcore.app.ui.theme

import androidx.compose.ui.graphics.Color

// WHOOP-style dark-first palette, shared 1:1 with the web dashboard
// (docs/index.html :root tokens). Keep these two in sync.
val RecoveryGreen = Color(0xFF16EC06)   // Recovery/Sleep high zone (67-100%)
val RecoveryYellow = Color(0xFFFFDE00)  // Recovery/Sleep medium zone (34-66%)
val RecoveryRed = Color(0xFFFF3B30)     // Recovery/Sleep low zone (0-33%)
val StrainBlue = Color(0xFF00A3FF)      // Strain accent
val SleepPurple = Color(0xFF9B8AFB)     // Sleep secondary accent (stage chart)

// Back-compat aliases used by a few older screens.
val VitalGreen = RecoveryGreen
val VitalAmber = RecoveryYellow
val VitalRed = RecoveryRed
val VitalBlue = StrainBlue
val VitalViolet = SleepPurple

val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF14161A)
val SurfaceDarkElevated = Color(0xFF1E2127)
val OnSurfaceDark = Color(0xFFF2F3F5)
val OnSurfaceMutedDark = Color(0xFF9AA1AD)

// Light theme kept only as a fallback — the app defaults to dark (see Theme.kt).
val BackgroundLight = Color(0xFFF7F8FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceLightElevated = Color(0xFFF0F1F3)
val OnSurfaceLight = Color(0xFF16181C)
val OnSurfaceMutedLight = Color(0xFF5C6167)

/** Returns the recovery/sleep zone color for a 0-100 score, WHOOP-style thresholds. */
fun zoneColor(score: Int): Color = when {
    score >= 67 -> RecoveryGreen
    score >= 34 -> RecoveryYellow
    else -> RecoveryRed
}

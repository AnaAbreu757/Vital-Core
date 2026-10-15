package com.vitalcore.app.ui.theme

import androidx.compose.ui.graphics.Color

// Dark-navy + blue palette, shared 1:1 with the web dashboard
// (docs/index.html :root tokens). Keep these two in sync.
val RecoveryGreen = Color(0xFF4FC3F7)    // Recovery/Sleep high zone (67-100%) — light blue
val RecoveryYellow = Color(0xFF2E6BE6)   // Recovery/Sleep medium zone (34-66%) — mid blue
val RecoveryRed = Color(0xFFFF5C5C)      // Recovery/Sleep low zone (0-33%) — kept red, the one clear warning color
val StrainBlue = Color(0xFF38BDF8)       // Strain accent — bright light blue
val SleepPurple = Color(0xFF7C9CFF)      // Sleep secondary accent (stage chart) — periwinkle-blue

// Back-compat aliases used by a few older screens.
val VitalGreen = RecoveryGreen
val VitalAmber = RecoveryYellow
val VitalRed = RecoveryRed
val VitalBlue = StrainBlue
val VitalViolet = SleepPurple

val BackgroundDark = Color(0xFF070B14)       // dark navy, not pure black
val SurfaceDark = Color(0xFF111729)
val SurfaceDarkElevated = Color(0xFF1B2440)
val OnSurfaceDark = Color(0xFFEAF1FB)
val OnSurfaceMutedDark = Color(0xFF8CA0C4)

// Light theme kept only as a fallback — the app defaults to dark (see Theme.kt).
val BackgroundLight = Color(0xFFF2F6FC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceLightElevated = Color(0xFFE7EEF9)
val OnSurfaceLight = Color(0xFF0E1526)
val OnSurfaceMutedLight = Color(0xFF51617E)

/** Returns the recovery/sleep zone color for a 0-100 score, same thresholds as before. */
fun zoneColor(score: Int): Color = when {
    score >= 67 -> RecoveryGreen
    score >= 34 -> RecoveryYellow
    else -> RecoveryRed
}

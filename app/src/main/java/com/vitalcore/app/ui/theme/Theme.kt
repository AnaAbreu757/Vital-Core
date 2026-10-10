package com.vitalcore.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = RecoveryGreen,
    secondary = StrainBlue,
    tertiary = SleepPurple,
    error = RecoveryRed,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkElevated,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMutedDark,
)

private val LightColors = lightColorScheme(
    primary = RecoveryGreen,
    secondary = StrainBlue,
    tertiary = SleepPurple,
    error = RecoveryRed,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLightElevated,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMutedLight,
)

/**
 * App-wide theme. Defaults to the WHOOP-style dark palette regardless of the
 * system setting (darkTheme defaults to true, not isSystemInDarkTheme()) to
 * match the web dashboard's always-dark design; still overridable, and still
 * supports Material You dynamic color on Android 12+ if ever enabled.
 */
@Composable
fun VitalCoreTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VitalCoreTypography,
        content = content,
    )
}

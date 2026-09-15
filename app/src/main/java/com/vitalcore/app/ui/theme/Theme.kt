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
    primary = VitalGreen,
    secondary = VitalBlue,
    tertiary = VitalViolet,
    error = VitalRed,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkElevated,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMutedDark,
)

private val LightColors = lightColorScheme(
    primary = VitalGreen,
    secondary = VitalBlue,
    tertiary = VitalViolet,
    error = VitalRed,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLightElevated,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMutedLight,
)

/**
 * App-wide theme. Supports light/dark and, on Android 12+, dynamic color
 * (Material You) while always falling back to the VitalCore brand palette.
 */
@Composable
fun VitalCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

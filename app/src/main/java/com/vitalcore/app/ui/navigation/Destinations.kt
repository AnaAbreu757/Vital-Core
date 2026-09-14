package com.vitalcore.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every top-level, bottom-nav-reachable destination in the app.
 * Kept as a sealed class (not an enum) so each destination can carry
 * its own route args in the future (e.g. Recovery for a specific date).
 */
sealed class VitalCoreDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : VitalCoreDestination("home", "Home", Icons.Filled.Home)
    data object Recovery : VitalCoreDestination("recovery", "Recovery", Icons.Filled.Favorite)
    data object Sleep : VitalCoreDestination("sleep", "Sleep", Icons.Filled.Nightlight)
    data object Activity : VitalCoreDestination("activity", "Activity", Icons.Outlined.DirectionsRun)
    data object Health : VitalCoreDestination("health", "Health", Icons.Filled.MonitorHeart)
    data object Settings : VitalCoreDestination("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Recovery, Sleep, Activity, Health, Settings)
    }
}

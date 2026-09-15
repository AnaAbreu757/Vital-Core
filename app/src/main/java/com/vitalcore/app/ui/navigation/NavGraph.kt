package com.vitalcore.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalcore.app.ui.screens.activity.ActivityScreen
import com.vitalcore.app.ui.screens.aicoach.AiCoachScreen
import com.vitalcore.app.ui.screens.friends.FriendsScreen
import com.vitalcore.app.ui.screens.health.HealthScreen
import com.vitalcore.app.ui.screens.home.HomeScreen
import com.vitalcore.app.ui.screens.importdata.ImportHealthDataScreen
import com.vitalcore.app.ui.screens.nutrition.NutritionScreen
import com.vitalcore.app.ui.screens.onboarding.OnboardingScreen
import com.vitalcore.app.ui.screens.recovery.RecoveryScreen
import com.vitalcore.app.ui.screens.settings.SettingsScreen
import com.vitalcore.app.ui.screens.sleep.SleepScreen
import com.vitalcore.app.ui.screens.streak.StreakCalendarScreen

private const val ROUTE_NUTRITION = "nutrition"
private const val ROUTE_AI_COACH = "ai_coach"
private const val ROUTE_IMPORT_APPLE_HEALTH = "import_apple_health"
private const val ROUTE_STREAK = "streak"
private const val ROUTE_FRIENDS = "friends"

/**
 * Root navigation host. Gates the six-tab main experience behind onboarding:
 * first-ever launch shows [OnboardingScreen]; once complete (persisted via
 * SettingsRepository/DataStore) subsequent launches go straight to Home.
 * This gate is a simple conditional composition (not a NavHost route) since
 * there's no need to navigate "back" into onboarding once it's done.
 */
@Composable
fun VitalCoreNavGraph() {
    val gateViewModel: NavigationGateViewModel = hiltViewModel()
    val onboardingComplete by gateViewModel.onboardingComplete.collectAsState(initial = null)
    var forceMain by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    when {
        forceMain || onboardingComplete == true -> MainScaffold()
        onboardingComplete == false -> OnboardingScreen(onComplete = { forceMain = true })
        else -> Unit // still loading the preference; render nothing briefly
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hiddenBottomBarRoutes = setOf(ROUTE_NUTRITION, ROUTE_AI_COACH, ROUTE_IMPORT_APPLE_HEALTH, ROUTE_STREAK, ROUTE_FRIENDS)
    val showBottomBar = currentRoute !in hiddenBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    VitalCoreDestination.bottomNavItems.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = VitalCoreDestination.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable(VitalCoreDestination.Home.route) {
                HomeScreen(
                    onOpenAiCoach = { navController.navigate(ROUTE_AI_COACH) },
                    onOpenNutrition = { navController.navigate(ROUTE_NUTRITION) },
                    onOpenStreak = { navController.navigate(ROUTE_STREAK) },
                    onOpenFriends = { navController.navigate(ROUTE_FRIENDS) },
                )
            }
            composable(VitalCoreDestination.Recovery.route) { RecoveryScreen() }
            composable(VitalCoreDestination.Sleep.route) { SleepScreen() }
            composable(VitalCoreDestination.Activity.route) { ActivityScreen() }
            composable(VitalCoreDestination.Health.route) {
                HealthScreen(onOpenNutrition = { navController.navigate(ROUTE_NUTRITION) })
            }
            composable(VitalCoreDestination.Settings.route) {
                SettingsScreen(onOpenAppleHealthImport = { navController.navigate(ROUTE_IMPORT_APPLE_HEALTH) })
            }
            composable(ROUTE_NUTRITION) { NutritionScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_AI_COACH) { AiCoachScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_IMPORT_APPLE_HEALTH) { ImportHealthDataScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_STREAK) { StreakCalendarScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_FRIENDS) { FriendsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

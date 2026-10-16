package com.vitalcore.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vitalcore.app.ui.components.DashboardCard
import com.vitalcore.app.ui.components.ProfileAvatar
import com.vitalcore.app.ui.components.RingGauge
import com.vitalcore.app.ui.components.StatTile
import com.vitalcore.app.ui.screens.account.ProfileViewModel
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.RecoveryGreen
import com.vitalcore.app.ui.theme.RecoveryRed
import com.vitalcore.app.ui.theme.StrainBlue
import com.vitalcore.app.ui.theme.SurfaceDarkElevated
import com.vitalcore.app.ui.theme.zoneColor
import kotlin.math.roundToInt

/**
 * Dashboard: a WHOOP/Bevel-style triple-ring overview (Strain / Recovery /
 * Sleep), a coaching-style insight banner, a Health Monitor stat grid, and
 * quick actions — all backed by [HomeViewModel], which runs the real
 * calculation engine over synced Health Connect data. Uses the same shared
 * DashboardCard/RingGauge/StatTile widgets as Recovery/Sleep/Activity/Health
 * so every screen reads as the same product.
 *
 * Re-syncs on every app resume and via the manual refresh button, since
 * there's no periodic background worker yet — see HomeViewModel's kdoc.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onOpenAiCoach: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenStreak: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val profile by profileViewModel.profile.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume = rememberUpdatedState(viewModel::refresh)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume.value()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.loading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TopBar(
                photoPath = profile.photoPath,
                fallbackPhotoUrl = profileViewModel.googlePhotoUrl,
                onAvatarClick = onOpenSettings,
                refreshing = state.refreshing,
                onRefresh = viewModel::refresh,
            )
        }

        state.error?.let { err ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RecoveryRed.copy(alpha = 0.15f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Refresh failed", style = MaterialTheme.typography.labelMedium, color = RecoveryRed)
                        Text(err, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RingGauge(
                    label = "Strain",
                    display = state.strainScore?.let { "%.1f".format(it) } ?: "--",
                    fraction = ((state.strainScore ?: 0.0) / 21.0).toFloat(),
                    color = StrainBlue,
                )
                RingGauge(
                    label = "Recovery",
                    display = state.recoveryScore?.let { "$it%" } ?: "--",
                    fraction = (state.recoveryScore ?: 0) / 100f,
                    color = state.recoveryScore?.let { zoneColor(it) } ?: SurfaceDarkElevated,
                )
                RingGauge(
                    label = "Sleep",
                    display = state.sleepScore?.let { "$it%" } ?: "--",
                    fraction = (state.sleepScore ?: 0) / 100f,
                    color = state.sleepScore?.let { zoneColor(it) } ?: SurfaceDarkElevated,
                )
            }
        }

        item {
            DashboardCard {
                Text("COACHING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                Text(
                    state.topInsight ?: "Not enough data yet to generate a personalized insight.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            DashboardCard {
                Text("HEALTH MONITOR", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        listOf(
                            Triple("HRV", state.hrvMillis?.let { "%.0f".format(it) } ?: "--", "ms"),
                            Triple("RHR", state.restingHeartRateBpm?.toString() ?: "--", "bpm"),
                            Triple("Steps", state.steps?.toString() ?: "--", ""),
                            Triple("Calories", state.calories?.roundToInt()?.toString() ?: "--", "kcal"),
                        )
                    ) { (label, value, unit) -> StatTile(label, value, unit, dotColor = RecoveryGreen) }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenAiCoach, modifier = Modifier.weight(1f)) { Text("AI Coach") }
                OutlinedButton(onClick = onOpenNutrition, modifier = Modifier.weight(1f)) { Text("Nutrition") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenStreak, modifier = Modifier.weight(1f)) { Text("Streak") }
                OutlinedButton(onClick = onOpenFriends, modifier = Modifier.weight(1f)) { Text("Friends") }
            }
        }

        item {
            DashboardCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Data quality", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMutedDark)
                    Text("${state.dataQualityPercent}%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    photoPath: String?,
    fallbackPhotoUrl: String?,
    onAvatarClick: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            photoPath = photoPath,
            fallbackUrl = fallbackPhotoUrl,
            size = 34.dp,
            onClick = onAvatarClick,
        )
        Text("TODAY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        if (refreshing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync Health Connect now")
            }
        }
    }
}

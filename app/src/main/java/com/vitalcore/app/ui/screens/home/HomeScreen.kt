package com.vitalcore.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.RecoveryGreen
import com.vitalcore.app.ui.theme.RecoveryRed
import com.vitalcore.app.ui.theme.RecoveryYellow
import com.vitalcore.app.ui.theme.StrainBlue
import com.vitalcore.app.ui.theme.SurfaceDark
import com.vitalcore.app.ui.theme.SurfaceDarkElevated
import com.vitalcore.app.ui.theme.zoneColor
import kotlin.math.roundToInt

/**
 * Dashboard: a WHOOP/Bevel-style triple-ring overview (Strain / Recovery /
 * Sleep), a coaching-style insight banner, a Health Monitor stat grid, and
 * quick actions — all backed by [HomeViewModel], which runs the real
 * calculation engine over synced Health Connect data. The ring colors and
 * zoneColor() thresholds are the same ones the web dashboard uses (see
 * ui/theme/Color.kt), so both read as the same product.
 *
 * Re-syncs on every app resume and via the manual refresh button, since
 * there's no periodic background worker yet — see HomeViewModel's kdoc.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenAiCoach: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenStreak: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

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
        item { TopBar(refreshing = state.refreshing, onRefresh = viewModel::refresh) }

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
                    ) { (label, value, unit) -> VitalTile(label, value, unit) }
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
private fun TopBar(refreshing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceDarkElevated),
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

@Composable
private fun DashboardCard(content: @Composable Column.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun RingGauge(label: String, display: String, fraction: Float, color: Color) {
    val clamped = fraction.coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            Canvas(modifier = Modifier.size(84.dp)) {
                val stroke = 8.dp.toPx()
                val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)

                // background track
                drawArc(
                    color = SurfaceDarkElevated,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                // soft glow: a few wider, fainter passes behind the main arc
                for (i in 3 downTo 1) {
                    drawArc(
                        color = color.copy(alpha = 0.08f * i),
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(topLeft.x - i * 2.dp.toPx(), topLeft.y - i * 2.dp.toPx()),
                        size = Size(arcSize.width + i * 4.dp.toPx(), arcSize.height + i * 4.dp.toPx()),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(display, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceMutedDark,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun VitalTile(label: String, value: String, unit: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDarkElevated)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(RecoveryGreen))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
        }
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
    }
}

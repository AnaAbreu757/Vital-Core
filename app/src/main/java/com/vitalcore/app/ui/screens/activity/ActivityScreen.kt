package com.vitalcore.app.ui.screens.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalcore.app.ui.components.DashboardCard
import com.vitalcore.app.ui.components.RingGauge
import com.vitalcore.app.ui.components.StatTile
import com.vitalcore.app.ui.components.TrendChartWithPeriodSelector
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.StrainBlue
import kotlin.math.roundToInt

@Composable
fun ActivityScreen(viewModel: ActivityViewModel = hiltViewModel()) {
    val history by viewModel.last90DaysStrain.collectAsState()
    val today by viewModel.todayState.collectAsState()
    val latest = history.lastOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Activity", style = MaterialTheme.typography.headlineMedium) }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RingGauge(
                    label = "Strain",
                    display = latest?.let { "%.1f".format(it) } ?: "--",
                    fraction = (latest ?: 0f) / 21f,
                    color = StrainBlue,
                    size = 170.dp,
                )
            }
        }

        item {
            DashboardCard {
                Text("TODAY", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        listOf(
                            Triple("Steps", today.steps?.toString() ?: "--", ""),
                            Triple("Calories", today.calories?.roundToInt()?.toString() ?: "--", "kcal"),
                            Triple("Workouts", today.exerciseCount.toString(), ""),
                        )
                    ) { (label, value, unit) -> StatTile(label, value, unit) }
                }
            }
        }

        item {
            DashboardCard {
                Text("STRAIN TREND", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                TrendChartWithPeriodSelector(
                    valuesForPeriod = { period -> history.takeLast(period.days.coerceAtMost(history.size)) },
                    lineColor = StrainBlue,
                )
            }
        }
    }
}

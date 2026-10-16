package com.vitalcore.app.ui.screens.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.vitalcore.app.ui.components.TrendChartWithPeriodSelector
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.SurfaceDarkElevated
import com.vitalcore.app.ui.theme.zoneColor

@Composable
fun SleepScreen(viewModel: SleepViewModel = hiltViewModel()) {
    val history by viewModel.last90DaysScores.collectAsState()
    val latest = history.lastOrNull()?.toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Sleep", style = MaterialTheme.typography.headlineMedium) }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RingGauge(
                    label = "Sleep",
                    display = latest?.let { "$it%" } ?: "--",
                    fraction = (latest ?: 0) / 100f,
                    color = latest?.let { zoneColor(it) } ?: SurfaceDarkElevated,
                    size = 170.dp,
                )
            }
        }

        item {
            DashboardCard {
                Text("TREND", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                TrendChartWithPeriodSelector(
                    valuesForPeriod = { period -> history.takeLast(period.days.coerceAtMost(history.size)) },
                    lineColor = latest?.let { zoneColor(it) } ?: MaterialTheme.colorScheme.primary,
                )
            }
        }

        item {
            DashboardCard {
                Text("WHAT THIS MEANS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                Text(
                    sleepExplanation(latest),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun sleepExplanation(score: Int?): String = when {
    score == null -> "Not enough sleep data synced yet from Health Connect to score last night."
    score >= 67 -> "Good sleep last night relative to your usual pattern."
    score >= 34 -> "Average sleep — close to your normal range."
    else -> "Shorter or more disrupted sleep than usual — this often shows up in today's Recovery too."
}

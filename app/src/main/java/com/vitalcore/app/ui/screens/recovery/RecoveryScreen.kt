package com.vitalcore.app.ui.screens.recovery

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
fun RecoveryScreen(viewModel: RecoveryViewModel = hiltViewModel()) {
    val history by viewModel.last90DaysScores.collectAsState()
    val latest = history.lastOrNull()?.toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Recovery", style = MaterialTheme.typography.headlineMedium) }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RingGauge(
                    label = "Recovery",
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
                    recoveryExplanation(latest),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun recoveryExplanation(score: Int?): String = when {
    score == null -> "Not enough data yet — Recovery compares today against your personal baseline, which needs a few days of history to build."
    score >= 67 -> "Your body is well recovered. HRV and resting heart rate are in a good range relative to your own baseline — a good day to push harder."
    score >= 34 -> "Moderate recovery. Your signals are close to your usual range — train at your normal intensity and watch how you feel."
    else -> "Low recovery. HRV and/or resting heart rate suggest your body hasn't fully bounced back — consider an easier day or extra rest."
}

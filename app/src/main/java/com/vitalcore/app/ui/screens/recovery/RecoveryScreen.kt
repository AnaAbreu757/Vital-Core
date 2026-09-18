package com.vitalcore.app.ui.screens.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalcore.app.ui.components.ChartPeriod
import com.vitalcore.app.ui.components.TrendChartWithPeriodSelector

@Composable
fun RecoveryScreen(viewModel: RecoveryViewModel = hiltViewModel()) {
    val history by viewModel.last90DaysScores.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Recovery", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(history.lastOrNull()?.toInt()?.toString() ?: "--", style = MaterialTheme.typography.displayLarge)
                Text("Trend", style = MaterialTheme.typography.titleMedium)
                TrendChartWithPeriodSelector(
                    valuesForPeriod = { period ->
                        history.takeLast(period.days.coerceAtMost(history.size))
                    },
                )
            }
        }
    }
}

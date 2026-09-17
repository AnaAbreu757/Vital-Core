package com.vitalcore.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Dashboard: Good morning header, Recovery/Sleep/Strain cards, today's
 * insight, and vitals — all backed by [HomeViewModel], which runs the real
 * calculation engine over synced Health Connect data.
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Good morning", style = MaterialTheme.typography.headlineMedium) }

        item {
            ScoreCard("Recovery", state.recoveryScore?.toString() ?: "--", state.recoveryConfidence)
        }
        item { ScoreCard("Sleep", state.sleepScore?.toString() ?: "--", null) }
        item { ScoreCard("Strain", state.strainScore?.let { "%.1f".format(it) } ?: "--", null) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Today's insight", style = MaterialTheme.typography.titleMedium)
                    Text(state.topInsight ?: "Not enough data yet to generate a personalized insight.")
                }
            }
        }

        item {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.OutlinedButton(onClick = onOpenAiCoach, modifier = Modifier.weight(1f)) {
                    Text("Ask AI Coach")
                }
                androidx.compose.material3.OutlinedButton(onClick = onOpenNutrition, modifier = Modifier.weight(1f)) {
                    Text("Log food / water")
                }
            }
        }

        item {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.OutlinedButton(onClick = onOpenStreak, modifier = Modifier.weight(1f)) {
                    Text("View streak")
                }
                androidx.compose.material3.OutlinedButton(onClick = onOpenFriends, modifier = Modifier.weight(1f)) {
                    Text("Friends")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vitals", style = MaterialTheme.typography.titleMedium)
                    Text("HRV: ${state.hrvMillis?.let { "%.0f ms".format(it) } ?: "no data"}")
                    Text("Resting HR: ${state.restingHeartRateBpm?.let { "$it bpm" } ?: "no data"}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Activity", style = MaterialTheme.typography.titleMedium)
                    Text("Steps: ${state.steps ?: "no data"}")
                    Text("Calories: ${state.calories?.let { "%.0f".format(it) } ?: "no data"}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data quality", style = MaterialTheme.typography.titleMedium)
                    Text("${state.dataQualityPercent}%")
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(label: String, value: String, confidence: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.displayLarge)
            confidence?.let { Text("Confidence: $it", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

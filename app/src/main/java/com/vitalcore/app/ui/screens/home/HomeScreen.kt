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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Dashboard: Good morning header, Recovery/Sleep/Strain cards, today's
 * insight, and vitals — all backed by [HomeViewModel], which runs the real
 * calculation engine over synced Health Connect data.
 *
 * Re-syncs on every app resume (see the LifecycleEventObserver below) and
 * via the manual refresh button, since there's no periodic background
 * worker yet — see HomeViewModel's kdoc.
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Good morning", style = MaterialTheme.typography.headlineMedium)
                if (state.refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                } else {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Health Connect now")
                    }
                }
            }
        }

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = onOpenAiCoach, modifier = Modifier.weight(1f)) {
                    Text("Ask AI Coach")
                }
                OutlinedButton(onClick = onOpenNutrition, modifier = Modifier.weight(1f)) {
                    Text("Log food / water")
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = onOpenStreak, modifier = Modifier.weight(1f)) {
                    Text("View streak")
                }
                OutlinedButton(onClick = onOpenFriends, modifier = Modifier.weight(1f)) {
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

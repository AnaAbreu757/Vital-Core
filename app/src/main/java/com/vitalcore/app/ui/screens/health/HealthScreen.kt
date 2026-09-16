package com.vitalcore.app.ui.screens.health

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

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel(), onOpenNutrition: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Health", style = MaterialTheme.typography.headlineMedium)
        androidx.compose.material3.OutlinedButton(onClick = onOpenNutrition, modifier = Modifier.fillMaxWidth()) {
            Text("Nutrition — log food & water")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("HRV: ${state.hrvMillis?.let { "%.0f ms".format(it) } ?: "no data"}")
                Text("Resting HR: ${state.restingHeartRateBpm?.let { "$it bpm" } ?: "no data"}")
                Text("Respiratory rate: ${state.respiratoryRateBpm?.let { "%.1f br/min".format(it) } ?: "no data"}")
                Text("SpO2: ${state.spo2Percentage?.let { "%.0f%%".format(it) } ?: "no data"}")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Data quality", style = MaterialTheme.typography.titleMedium)
                Text("${state.dataQualityPercent}%")
                if (state.missingSignals.isNotEmpty()) {
                    Text("Gaps: ${state.missingSignals.joinToString()}")
                }
            }
        }
    }
}

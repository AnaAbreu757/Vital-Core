package com.vitalcore.app.ui.screens.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import com.vitalcore.app.domain.calculations.BiologicalAgeResult
import com.vitalcore.app.ui.components.DashboardCard
import com.vitalcore.app.ui.components.RingGauge
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.RecoveryGreen
import com.vitalcore.app.ui.theme.RecoveryRed
import com.vitalcore.app.ui.theme.RecoveryYellow

/**
 * Combines the AI Coach entry point with the Biological Age estimate —
 * this is the "Coach" bottom-nav tab. The actual chat UI stays in the
 * existing AiCoachScreen (reached via [onOpenAiCoach]); this screen is the
 * tab's landing page.
 */
@Composable
fun CoachScreen(viewModel: CoachViewModel = hiltViewModel(), onOpenAiCoach: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Coach", style = MaterialTheme.typography.headlineMedium) }

        item {
            DashboardCard {
                Text("BIOLOGICAL AGE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                if (state.loading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    BiologicalAgeContent(state.biologicalAge)
                }
            }
        }

        item {
            DashboardCard {
                Text("AI COACH", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
                Text(
                    "Ask questions about today's Recovery, Sleep, Strain, and nutrition in plain language.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                Button(onClick = onOpenAiCoach, modifier = Modifier.fillMaxWidth()) { Text("Open AI Coach chat") }
            }
        }
    }
}

@Composable
private fun BiologicalAgeContent(result: BiologicalAgeResult?) {
    if (result == null || !result.hasEnoughData) {
        Text(
            "Add your birth year in Settings > Profile, and keep using the app for a few days, " +
                "to get an estimated biological age.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        return
    }
    val diff = result.estimatedBiologicalAge - result.chronologicalAge
    val color = when {
        diff <= -3 -> RecoveryGreen
        diff >= 3 -> RecoveryRed
        else -> RecoveryYellow
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        RingGauge(
            label = "Biological age",
            display = "${result.estimatedBiologicalAge}",
            fraction = 0.7f,
            color = color,
            size = 140.dp,
        )
    }
    Text(
        "Chronological age: ${result.chronologicalAge} · Pace of aging: ${"%.2f".format(result.paceOfAging)}×",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 10.dp),
    )
    Text(
        "Estimate only, based on your recent Recovery, sleep, and activity — not a clinical or lab measurement. " +
            "True biological age (e.g. via DNA methylation) can't be measured by any wearable.",
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceMutedDark,
        modifier = Modifier.padding(top = 6.dp),
    )
}

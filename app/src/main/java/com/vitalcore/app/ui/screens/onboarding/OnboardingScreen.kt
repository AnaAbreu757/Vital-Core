package com.vitalcore.app.ui.screens.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalcore.app.data.healthconnect.HealthConnectPermissions
import com.vitalcore.app.settings.OnboardingGoal

/**
 * Two-step onboarding: pick a goal, then connect Health Connect. Collects
 * only what's needed for personalization (the goal) — no account creation,
 * no unnecessary personal data, per the product spec.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit,
) {
    var selectedGoal by remember { mutableStateOf<OnboardingGoal?>(null) }
    var step by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        // Whatever subset was granted, proceed — the app is designed to work
        // gracefully with partial permissions (see ALGORITHMS.md / data quality).
        selectedGoal?.let { goal -> viewModel.completeOnboarding(goal) { onComplete() } }
    }

    when (step) {
        0 -> GoalStep(
            selected = selectedGoal,
            onSelect = { selectedGoal = it },
            onNext = { step = 1 },
        )
        else -> ConnectStep(
            onConnect = { permissionLauncher.launch(HealthConnectPermissions.ALL) },
            onSkip = { selectedGoal?.let { goal -> viewModel.completeOnboarding(goal) { onComplete() } } },
        )
    }
}

@Composable
private fun GoalStep(selected: OnboardingGoal?, onSelect: (OnboardingGoal) -> Unit, onNext: () -> Unit) {
    val goals = OnboardingGoal.entries
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("What's your main goal?", style = MaterialTheme.typography.headlineSmall) }
        items(goals) { goal ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = goal == selected, onClick = { onSelect(goal) })
                Text(goal.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
            }
        }
        item {
            Button(onClick = onNext, enabled = selected != null, modifier = Modifier.fillMaxWidth()) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ConnectStep(onConnect: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Connect Health Connect", style = MaterialTheme.typography.headlineSmall)
        Text(
            "VitalCore reads heart rate, HRV, sleep, activity, and other vitals through " +
                "Android Health Connect. Nothing is shared automatically — see Settings > Privacy.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("Connect") }
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
    }
}

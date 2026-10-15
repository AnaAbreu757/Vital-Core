package com.vitalcore.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalcore.app.ai.AiConfig
import com.vitalcore.app.ui.components.ProfileAvatar
import com.vitalcore.app.ui.screens.account.AccountViewModel
import com.vitalcore.app.ui.screens.account.ProfileViewModel
import com.vitalcore.app.ui.screens.connections.ConnectionsSection
import com.vitalcore.app.ui.theme.RecoveryGreen
import com.vitalcore.app.ui.theme.RecoveryRed
import java.io.File

/**
 * Real Settings/Privacy screen (Milestone 12): notification toggles wired to
 * DataStore + WorkManager, JSON/CSV export, and delete-all-data with a
 * confirmation dialog (destructive action, see PRIVACY.md).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenAppleHealthImport: () -> Unit = {},
) {
    val notifications by viewModel.notificationSettings.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        item { AccountSection() }

        item { ProfileCard() }

        item { DiagnosticsCard() }

        item { ConnectionsSection(onOpenAppleHealthImport = onOpenAppleHealthImport) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    ToggleRow("Morning summary", notifications.morningSummary, viewModel::setMorningSummary)
                    ToggleRow("Sleep reminder", notifications.sleepReminder, viewModel::setSleepReminder)
                    ToggleRow("High strain alert", notifications.highStrain, viewModel::setHighStrain)
                    ToggleRow("Low recovery alert", notifications.lowRecovery, viewModel::setLowRecovery)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI Coach", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Optional. Bring your own Anthropic-compatible API endpoint and key — " +
                            "VitalCore never ships a key inside the app. Stored only on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AiCoachConfigForm(
                        current = aiConfig,
                        onSave = { enabled, endpoint, key, model -> viewModel.saveAiConfig(enabled, endpoint, key, model) },
                        onClear = { viewModel.clearAiConfig() },
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your data stays on this device. No ads, no data sale, no automatic sharing.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            viewModel.exportJson { file -> lastMessage = "Exported JSON to ${file.name}" }
                        }) { Text("Export JSON") }
                        OutlinedButton(onClick = {
                            viewModel.exportCsv { file -> lastMessage = "Exported CSV to ${file.name}" }
                        }) { Text("Export CSV") }
                    }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("Delete all data", color = MaterialTheme.colorScheme.error)
                    }
                    lastMessage?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all data?") },
            text = { Text("This permanently deletes everything VitalCore has stored on this device. This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAllData { lastMessage = "All data deleted." }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AccountSection(viewModel: AccountViewModel = hiltViewModel()) {
    val user by viewModel.currentUser.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    // Credential Manager's sign-in UI requires an Activity context; inside
    // this app's single Activity, LocalContext.current already is that
    // Activity context (see AuthRepository.signInWithGoogle kdoc).
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Account & cloud backup", style = MaterialTheme.typography.titleMedium)
            if (user != null) {
                Text("Signed in as ${user!!.email ?: user!!.displayName ?: user!!.uid}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::backupNow, enabled = !busy) { Text("Back up now") }
                    TextButton(onClick = viewModel::signOut) { Text("Sign out") }
                }
            } else {
                Text(
                    "Sign in with Google to back up your scores to the cloud (Firebase) and " +
                        "restore them on another device. Everything works fully without an account too.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { viewModel.signIn(context) }, enabled = !busy) { Text("Sign in with Google") }
            }
            message?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/**
 * Profile photo (local pick, overrides the Google account photo) + height,
 * weight, and birth year. These measurements aren't fed into the
 * calculation engine yet (e.g. a real max-HR for Strain still uses a
 * neutral default) — they're captured here ready for that follow-up.
 */
@Composable
private fun ProfileCard(viewModel: ProfileViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    var height by remember(profile.heightCm) { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var weight by remember(profile.weightKg) { mutableStateOf(profile.weightKg?.toString() ?: "") }
    var birthYear by remember(profile.birthYear) { mutableStateOf(profile.birthYear?.toString() ?: "") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val dest = File(context.filesDir, "profile_photo.jpg")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        viewModel.setPhotoPath(dest.absolutePath)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileAvatar(
                    photoPath = profile.photoPath,
                    fallbackUrl = viewModel.googlePhotoUrl,
                    size = 60.dp,
                    onClick = { photoPicker.launch("image/*") },
                )
                Text("Tap the photo to change it", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = birthYear,
                onValueChange = { birthYear = it },
                label = { Text("Birth year") },
                modifier = Modifier.fillMaxWidth(),
            )
            profile.estimatedMaxHeartRate?.let {
                Text("Estimated max heart rate: $it bpm (220 − age)", style = MaterialTheme.typography.labelMedium)
            }
            Button(onClick = {
                viewModel.setMeasurements(height.toFloatOrNull(), weight.toFloatOrNull(), birthYear.toIntOrNull())
            }) { Text("Save profile") }
        }
    }
}

/**
 * Shows exactly which Health Connect permissions are granted right now, plus
 * live counts of raw records actually read — see
 * HealthConnectDiagnosticsViewModel's kdoc for why this exists.
 */
@Composable
private fun DiagnosticsCard(viewModel: HealthConnectDiagnosticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.run() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Health Connect diagnostics", style = MaterialTheme.typography.titleMedium)
                if (state.running) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else TextButton(onClick = viewModel::run) { Text("Refresh") }
            }
            Text("Availability: ${state.availability ?: "—"}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Heart rate samples, last 7 days: ${state.heartRateSamples7d ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Sleep sessions, last 7 days: ${state.sleepSessions7d ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Permissions", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            state.permissions.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (p.granted) "✓" else "✗",
                        color = if (p.granted) RecoveryGreen else RecoveryRed,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(p.label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AiCoachConfigForm(
    current: AiConfig,
    onSave: (enabled: Boolean, endpoint: String, key: String, model: String) -> Unit,
    onClear: () -> Unit,
) {
    var enabled by remember(current) { mutableStateOf(current.enabled) }
    var endpoint by remember(current) { mutableStateOf(current.endpointUrl.ifBlank { "https://api.anthropic.com/v1/messages" }) }
    var apiKey by remember(current) { mutableStateOf(current.apiKey) }
    var model by remember(current) { mutableStateOf(current.model) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleRow("Enable AI Coach", enabled) { enabled = it }
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            label = { Text("Endpoint URL") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(enabled, endpoint, apiKey, model) }) { Text("Save") }
            TextButton(onClick = {
                enabled = false; endpoint = ""; apiKey = ""; model = "claude-sonnet-4-6"
                onClear()
            }) { Text("Clear") }
        }
    }
}

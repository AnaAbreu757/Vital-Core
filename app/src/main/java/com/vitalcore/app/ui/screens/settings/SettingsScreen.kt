package com.vitalcore.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalcore.app.ai.AiConfig
import com.vitalcore.app.ui.screens.account.AccountViewModel
import com.vitalcore.app.ui.screens.connections.ConnectionsSection

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
                Button(onClick = viewModel::signIn, enabled = !busy) { Text("Sign in with Google") }
            }
            message?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
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

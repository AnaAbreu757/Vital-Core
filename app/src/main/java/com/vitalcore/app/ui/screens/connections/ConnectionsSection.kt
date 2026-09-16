package com.vitalcore.app.ui.screens.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * "Connections" section of Settings: every data source VitalCore can pull
 * from, honestly labeled.
 *
 * - Health Connect: the primary path (Xiaomi/Mi Fit/Xiaomi Wear, Samsung
 *   Health, Garmin, and most other wearables sync into Health Connect on
 *   Android — see MANUAL_DEPLOY.md "Why there's no separate Samsung/Xiaomi
 *   connector" for why VitalCore doesn't and can't have its own connectors
 *   for these).
 * - Strava: a real OAuth2 connection, since Strava has a public API.
 * - Apple Health: import-only, since no Android app can read Apple Health
 *   directly — see the Import screen.
 */
@Composable
fun ConnectionsSection(
    viewModel: ConnectionsViewModel = hiltViewModel(),
    onOpenAppleHealthImport: () -> Unit = {},
) {
    val stravaTokens by viewModel.stravaTokens.collectAsState()
    val message by viewModel.message.collectAsState()
    val syncing by viewModel.syncing.collectAsState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Connections", style = MaterialTheme.typography.titleMedium)

            ConnectionRow(
                title = "Xiaomi / Mi Fit / Xiaomi Wear / Q Watch Pro",
                subtitle = "Already connected — synced automatically via Health Connect. No separate setup needed.",
            )
            ConnectionRow(
                title = "Samsung Health",
                subtitle = "Already connected — synced automatically via Health Connect. No separate setup needed.",
            )
            ConnectionRow(
                title = "Garmin, Fitbit, Pixel Watch, others",
                subtitle = "Covered the same way, as long as the device's own app syncs into Health Connect.",
            )

            androidx.compose.material3.HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Strava", style = MaterialTheme.typography.bodyLarge)
                if (stravaTokens != null) {
                    Text(
                        "Connected${stravaTokens?.athleteName?.let { " as $it" } ?: ""}.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::syncStrava, enabled = !syncing) { Text("Sync now") }
                        TextButton(onClick = viewModel::disconnectStrava) { Text("Disconnect") }
                    }
                } else {
                    Text(
                        "Real OAuth2 connection — imports your runs/rides as exercise sessions feeding Strain.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = viewModel::connectStrava) { Text("Connect Strava") }
                }
            }

            androidx.compose.material3.HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Apple Health", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Android apps can't read Apple Health directly — export your data on " +
                        "iPhone, then import the file here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenAppleHealthImport) { Text("Import from file") }
            }

            message?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun ConnectionRow(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}

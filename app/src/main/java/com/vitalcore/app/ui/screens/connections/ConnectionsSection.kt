package com.vitalcore.app.ui.screens.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * "Connections" section of Settings: every data source VitalCore can pull
 * from, honestly labeled.
 *
 * - Health Connect: the primary path (Xiaomi/Mi Fit/Xiaomi Wear, Samsung
 *   Health, Garmin, and most other wearables sync into Health Connect on
 *   Android — see MANUAL_DEPLOY.md for why VitalCore doesn't and can't have
 *   its own connectors for these).
 * - Apple Health: import-only, since no Android app can read Apple Health
 *   directly — see the Import screen.
 */
@Composable
fun ConnectionsSection(
    onOpenAppleHealthImport: () -> Unit = {},
) {
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
                Text("Apple Health", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Android apps can't read Apple Health directly — export your data on " +
                        "iPhone, then import the file here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenAppleHealthImport) { Text("Import from file") }
            }
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

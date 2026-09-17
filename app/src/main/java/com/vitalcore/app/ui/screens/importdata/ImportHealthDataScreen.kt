package com.vitalcore.app.ui.screens.importdata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Apple Health import screen. Since Android has no API to read Apple Health
 * directly, this is a file import: export health data on iPhone (or use a
 * converter that outputs the documented CSV format — see
 * AppleHealthCsvImporter's doc comment), share/copy the file to this device,
 * and pick it here.
 */
@Composable
fun ImportHealthDataScreen(viewModel: ImportHealthDataViewModel = hiltViewModel(), onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importFile(context.contentResolver, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Apple Health") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Why a file, not a direct connection?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Apple doesn't expose Health data to Android apps — there is no API for " +
                            "that on either side. Export your data on iPhone (Health app > profile " +
                            "picture > Export All Health Data), convert the export.xml to CSV with " +
                            "columns type,start,end,value,unit (several free converter tools do this), " +
                            "then pick the file below.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Button(onClick = { filePicker.launch("text/*") }, enabled = !state.importing) {
                Text("Choose CSV file")
            }

            if (state.importing) {
                CircularProgressIndicator()
            }

            state.result?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Import complete", style = MaterialTheme.typography.titleMedium)
                        Text("Rows read: ${result.rowsRead}")
                        Text("Rows imported: ${result.rowsImported}")
                        if (result.skippedTypes.isNotEmpty()) {
                            Text("Skipped (unrecognized) types: ${result.skippedTypes.joinToString()}")
                        }
                    }
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

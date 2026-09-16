package com.vitalcore.app.ui.screens.aicoach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Chat-style AI Coach screen. Reachable from Home's "Ask AI Coach" action
 * rather than a bottom-nav tab (keeping the six-tab structure intact). Fully
 * functional with no provider configured — it just returns a clear message
 * pointing to Settings > AI Coach (see NoOpAiProvider/RemoteAiProvider).
 */
@Composable
fun AiCoachScreen(viewModel: AiCoachViewModel = hiltViewModel(), onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Coach") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SuggestionChip(onClick = viewModel::explainRecovery, label = { Text("Recovery") })
                SuggestionChip(onClick = viewModel::explainSleep, label = { Text("Sleep") })
                SuggestionChip(onClick = viewModel::explainStrain, label = { Text("Strain") })
                SuggestionChip(onClick = viewModel::summarizeDay, label = { Text("Summarize day") })
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { message -> ChatBubble(message) }
                if (state.loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your recovery, sleep, strain…") },
                )
                IconButton(onClick = {
                    viewModel.send(input)
                    input = ""
                }) { Icon(Icons.Filled.Send, contentDescription = "Send") }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
            Text(message.text, modifier = Modifier.padding(12.dp))
        }
    }
}

package com.vitalcore.app.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Friends screen: your own "friend code" (your Firebase uid) to share, an
 * "add friend by code" field, and the list of friends you've added with
 * their public streak stats. See FriendsRepository's doc comment for why
 * this is a one-directional follow model rather than mutual friendship.
 */
@Composable
fun FriendsScreen(viewModel: FriendsViewModel = hiltViewModel(), onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var codeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your friend code", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.myUid ?: "Sign in first (Settings > Account) to get a code.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Share this with someone so they can add you. Whoever has your code can " +
                                "see your current and longest streak — nothing else.",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add a friend", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            label = { Text("Friend code (their uid)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { viewModel.addFriend(codeInput); codeInput = "" }) { Text("Add") }
                        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            item { Text("Your friends", style = MaterialTheme.typography.titleMedium) }

            items(state.friends) { friend ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(friend.displayName ?: friend.uid, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Streak: ${friend.currentStreak} days (best: ${friend.longestStreak})",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        IconButton(onClick = { viewModel.removeFriend(friend.uid) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }

            if (!state.loading && state.friends.isEmpty()) {
                item { Text("No friends added yet.", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

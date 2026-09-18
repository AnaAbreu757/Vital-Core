package com.vitalcore.app.ui.screens.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vitalcore.app.domain.model.MealType

/**
 * Food and water logging (Milestone: Nutrition). Reachable from the Health
 * tab rather than a new bottom-nav item, keeping the six-tab structure
 * described in the product spec intact.
 */
@Composable
fun NutritionScreen(viewModel: NutritionViewModel = hiltViewModel(), onBack: () -> Unit) {
    val summary by viewModel.today.collectAsState()
    var showAddFood by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Calories today", style = MaterialTheme.typography.titleMedium)
                        Text("${summary.totalCalories} kcal", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "P ${summary.totalProteinGrams.toInt()}g · C ${summary.totalCarbsGrams.toInt()}g · F ${summary.totalFatGrams.toInt()}g",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Water today", style = MaterialTheme.typography.titleMedium)
                        val progress = (summary.totalWaterMilliliters.toFloat() / summary.waterGoalMilliliters).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("${summary.totalWaterMilliliters} / ${summary.waterGoalMilliliters} ml")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.addWater(250) }) { Text("+250 ml") }
                            OutlinedButton(onClick = { viewModel.addWater(500) }) { Text("+500 ml") }
                            OutlinedButton(onClick = { viewModel.addWater(750) }) { Text("+750 ml") }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's food", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddFood = true }) { Text("+ Add") }
                }
            }

            items(summary.foodEntries) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(entry.name, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.calories} kcal · ${entry.mealType.name.lowercase()}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.deleteFood(entry.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }

            if (summary.foodEntries.isEmpty()) {
                item { Text("No food logged yet today.", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }

    if (showAddFood) {
        AddFoodDialog(
            onDismiss = { showAddFood = false },
            onConfirm = { name, calories, protein, carbs, fat, meal ->
                viewModel.addFood(name, calories, protein, carbs, fat, meal)
                showAddFood = false
            },
        )
    }
}

@Composable
private fun AddFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double?, Double?, Double?, MealType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(MealType.SNACK) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(
                    value = calories, onValueChange = { calories = it.filter { c -> c.isDigit() } },
                    label = { Text("Calories") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein, onValueChange = { protein = it }, label = { Text("Protein g") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs g") })
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat g") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MealType.entries.forEach { m ->
                        TextButton(onClick = { meal = m }) {
                            Text(m.name.lowercase() + if (m == meal) " ✓" else "")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = calories.toIntOrNull() ?: 0
                    if (name.isNotBlank() && cal > 0) {
                        onConfirm(name, cal, protein.toDoubleOrNull(), carbs.toDoubleOrNull(), fat.toDoubleOrNull(), meal)
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

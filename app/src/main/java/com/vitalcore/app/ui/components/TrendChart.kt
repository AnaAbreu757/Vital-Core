package com.vitalcore.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

enum class ChartPeriod(val label: String, val days: Int) {
    ONE_DAY("1D", 1),
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
}

/**
 * Lightweight Canvas-based line chart — no third-party charting dependency
 * needed for the MVP. Used for every trend in the app (HRV, RHR, Sleep,
 * Recovery, Strain, Steps, Calories, SpO2, Respiratory rate) via the shared
 * [ChartPeriod] selector.
 */
@Composable
fun TrendChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp).padding(8.dp)) {
        if (values.size < 2) return@Canvas
        val max = values.max()
        val min = values.min()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            Offset(x = index * stepX, y = size.height - ((value - min) / range) * size.height)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Row of tappable period chips (1D/7D/30D/90D) that drives a [TrendChart]'s data window. */
@Composable
fun PeriodSelector(
    selected: ChartPeriod,
    onSelect: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        ChartPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label) },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

/** Convenience wrapper combining the selector and chart, remembering the current period. */
@Composable
fun TrendChartWithPeriodSelector(
    valuesForPeriod: (ChartPeriod) -> List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    var period by remember { mutableStateOf(ChartPeriod.SEVEN_DAYS) }
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        PeriodSelector(selected = period, onSelect = { period = it })
        TrendChart(values = valuesForPeriod(period), lineColor = lineColor)
    }
}

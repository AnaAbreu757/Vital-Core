package com.vitalcore.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vitalcore.app.ui.theme.SurfaceDarkElevated

enum class ChartPeriod(val label: String, val days: Int) {
    ONE_DAY("1D", 1),
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
}

/**
 * Canvas-based line chart: smooth (quadratic-bezier) curve, a soft gradient
 * fill under the line, a faint horizontal grid, a subtle glow behind the
 * line, and a highlighted dot at the most recent value. No third-party
 * charting dependency — used for every trend in the app (HRV, RHR, Sleep,
 * Recovery, Strain, Steps, Calories, SpO2, Respiratory rate) via the shared
 * [ChartPeriod] selector.
 */
@Composable
fun TrendChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
        if (values.size < 2) return@Canvas
        val max = values.max()
        val min = values.min()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)

        // faint horizontal grid (25/50/75%)
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = size.height * frac
            drawLine(
                color = SurfaceDarkElevated,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f,
            )
        }

        val points = values.mapIndexed { index, value ->
            Offset(x = index * stepX, y = size.height - ((value - min) / range) * size.height)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2
                val midY = (p0.y + p1.y) / 2
                quadraticBezierTo(p0.x, p0.y, midX, midY)
            }
            lineTo(points.last().x, points.last().y)
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.32f), Color.Transparent)),
        )

        // soft glow behind the line
        for (i in 3 downTo 1) {
            drawPath(
                linePath,
                color = lineColor.copy(alpha = 0.10f * i),
                style = Stroke(width = (4 + i * 2).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val last = points.last()
        drawCircle(color = lineColor, radius = 7.dp.toPx(), center = last)
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = last)
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
    initialPeriod: ChartPeriod = ChartPeriod.SEVEN_DAYS,
) {
    var period by remember { mutableStateOf(initialPeriod) }
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        PeriodSelector(selected = period, onSelect = { period = it })
        TrendChart(values = valuesForPeriod(period), lineColor = lineColor)
    }
}

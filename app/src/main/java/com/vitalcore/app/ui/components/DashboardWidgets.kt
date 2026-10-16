package com.vitalcore.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.SurfaceDark
import com.vitalcore.app.ui.theme.SurfaceDarkElevated

/**
 * The shared card shell used by every screen's sections (Home, Recovery,
 * Sleep, Activity, Health): a rounded, dark-surface card with consistent
 * padding. Keeping this in one place is what makes every screen read as
 * the same product instead of each one inventing its own card style.
 */
@Composable
fun DashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * A single circular gauge with a soft glow — the same visual language used
 * for Strain/Recovery/Sleep on Home, reused anywhere a screen needs one big
 * "here's today's number" ring (e.g. the top of Recovery, Sleep, Activity).
 */
@Composable
fun RingGauge(label: String, display: String, fraction: Float, color: Color, size: androidx.compose.ui.unit.Dp = 84.dp) {
    val clamped = fraction.coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            Canvas(modifier = Modifier.size(size)) {
                val stroke = this.size.minDimension * 0.095f
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)

                drawArc(
                    color = SurfaceDarkElevated,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                for (i in 3 downTo 1) {
                    drawArc(
                        color = color.copy(alpha = 0.08f * i),
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        useCenter = false,
                        topLeft = Offset(topLeft.x - i * 2.dp.toPx(), topLeft.y - i * 2.dp.toPx()),
                        size = Size(arcSize.width + i * 4.dp.toPx(), arcSize.height + i * 4.dp.toPx()),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(display, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceMutedDark,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** A small stat chip (label + value + unit), optionally with a colored status dot. */
@Composable
fun StatTile(label: String, value: String, unit: String = "", dotColor: Color? = null) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDarkElevated)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            if (dotColor != null) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            }
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
        }
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMutedDark)
    }
}

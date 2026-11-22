package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.GoalProgress
import com.csymvoul.expenses.ui.theme.WarningColor

/** A donut (ring-shaped pie) chart. Slices are drawn clockwise from the top. */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    thickness: Dp = 26.dp,
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val diameter = size.minDimension - stroke
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val visible = slices.filter { it.second > 0f }
            val total = visible.sumOf { it.second.toDouble() }.toFloat()
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            val gap = if (visible.size > 1) 2f else 0f
            var start = -90f
            visible.forEach { (color, value) ->
                val sweep = value / total * 360f
                drawArc(
                    color = color,
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke),
                )
                start += sweep
            }
        }
        center()
    }
}

@Composable
fun GoalBar(goal: GoalProgress, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    LinearProgressIndicator(
        progress = { goal.fraction.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(height),
        color = goalColor(goal),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun goalColor(goal: GoalProgress): Color = when {
    goal.isOver -> MaterialTheme.colorScheme.error
    goal.percent >= 80 -> WarningColor
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

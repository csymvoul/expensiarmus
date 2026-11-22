package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.GoalProgress
import com.csymvoul.expenses.ui.theme.WarningColor

/**
 * A donut (ring-shaped pie) chart. Slices are drawn clockwise from the top. Tapping a slice
 * reports its index through [onSliceTap]; tapping the hole or outside the ring reports null.
 * The [selected] slice is drawn thicker and the others fade.
 */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    thickness: Dp = 26.dp,
    selected: Int? = null,
    onSliceTap: (Int?) -> Unit = {},
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val values = slices.map { it.second }
    Box(
        modifier.pointerInput(values) {
            detectTapGestures { tap ->
                onSliceTap(DonutMath.sliceAt(values, tap.x, tap.y, size.width.toFloat(), size.height.toFloat(), thickness.toPx()))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val diameter = size.minDimension - stroke * 1.4f
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val total = values.filter { it > 0f }.sum()
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            val gap = if (values.count { it > 0f } > 1) 2f else 0f
            var start = -90f
            slices.forEachIndexed { i, (color, value) ->
                if (value <= 0f) return@forEachIndexed
                val sweep = value / total * 360f
                val isSelected = i == selected
                drawArc(
                    color = if (selected == null || isSelected) color else color.copy(alpha = 0.3f),
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(if (isSelected) stroke * 1.4f else stroke),
                )
                start += sweep
            }
        }
        center()
    }
}

/** Progress towards a goal, drawn as a gold-rimmed potion level. */
@Composable
fun GoalBar(goal: GoalProgress, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val shape = RoundedCornerShape(50)
    LinearProgressIndicator(
        progress = { goal.fraction.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.secondary, shape),
        color = goalColor(goal),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Butt,
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

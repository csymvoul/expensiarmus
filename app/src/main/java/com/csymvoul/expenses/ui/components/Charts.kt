package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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

/**
 * Vertical bars with a label under each. Tapping a bar reports its index; the [selected] bar
 * stays bright and the others fade.
 */
@Composable
fun BarChart(
    values: List<Long>,
    labels: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val ink = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(values.size) {
                    detectTapGestures { tap ->
                        BarMath.barAt(tap.x, size.width.toFloat(), values.size).takeIf { it >= 0 }?.let(onSelect)
                    }
                },
        ) {
            val n = values.size
            if (n == 0) return@Canvas
            val slot = size.width / n
            val barWidth = slot * 0.62f
            val top = 4.dp.toPx()
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val h = v.toFloat() / max * (size.height - top)
                val topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h)
                val color = if (selected == null || selected == i) ink else ink.copy(alpha = 0.3f)
                drawRoundRect(color, topLeft, Size(barWidth, h), CornerRadius(3.dp.toPx()))
                if (selected == i) {
                    drawRoundRect(gold, topLeft, Size(barWidth, h), CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                }
            }
            drawLine(gold, Offset(0f, size.height), Offset(size.width, size.height), 1.5.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEachIndexed { i, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
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

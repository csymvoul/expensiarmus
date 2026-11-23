package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun isNight() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * A sheet of old parchment in an ornate frame: cut corners, a double gold border, a star in
 * each corner and diamonds at the middle of the top and bottom edges.
 */
@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    spacing: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val paperCenter = if (isNight()) c.surfaceContainerHigh else c.surfaceContainerLowest
    val paperEdge = if (isNight()) c.surfaceContainerLow else c.surfaceContainerHigh
    val gold = c.secondary
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind { drawFrame(paperCenter, paperEdge, gold) }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = spacing,
        content = content,
    )
}

internal fun DrawScope.chamfered(inset: Float, cut: Float): Path = Path().apply {
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    val k = (cut - inset * 0.4f).coerceAtLeast(0f)
    moveTo(l + k, t)
    lineTo(r - k, t)
    lineTo(r, t + k)
    lineTo(r, b - k)
    lineTo(r - k, b)
    lineTo(l + k, b)
    lineTo(l, b - k)
    lineTo(l, t + k)
    close()
}

/** Four-pointed sparkle centred on [at]. */
fun DrawScope.drawSparkle(at: Offset, radius: Float, color: Color) {
    val w = radius * 0.3f
    val path = Path().apply {
        moveTo(at.x, at.y - radius)
        lineTo(at.x + w, at.y - w)
        lineTo(at.x + radius, at.y)
        lineTo(at.x + w, at.y + w)
        lineTo(at.x, at.y + radius)
        lineTo(at.x - w, at.y + w)
        lineTo(at.x - radius, at.y)
        lineTo(at.x - w, at.y - w)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawFrame(paperCenter: Color, paperEdge: Color, gold: Color) {
    val cut = 14.dp.toPx()
    drawPath(
        chamfered(0f, cut),
        Brush.radialGradient(
            listOf(paperCenter, paperEdge),
            center = center,
            radius = maxOf(size.width, size.height) * 0.75f,
        ),
    )
    drawPath(chamfered(1.dp.toPx(), cut), gold, style = Stroke(1.6.dp.toPx()))
    drawPath(chamfered(5.dp.toPx(), cut), gold.copy(alpha = 0.55f), style = Stroke(0.8.dp.toPx()))
    val s = 11.dp.toPx()
    listOf(Offset(s, s), Offset(size.width - s, s), Offset(s, size.height - s), Offset(size.width - s, size.height - s))
        .forEach { drawSparkle(it, 4.dp.toPx(), gold) }
    // Diamonds sitting on the middle of the top and bottom borders.
    listOf(Offset(center.x, 1.dp.toPx()), Offset(center.x, size.height - 1.dp.toPx())).forEach {
        drawSparkle(it, 5.dp.toPx(), paperEdge)
        drawSparkle(it, 3.5.dp.toPx(), gold)
    }
}

/** Two thin gold lines with a sparkle in the middle. */
@Composable
fun GoldRule(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.secondary
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val y1 = 3.dp.toPx()
        val y2 = 6.dp.toPx()
        drawLine(gold, Offset(0f, y1), Offset(size.width, y1), 1.2.dp.toPx())
        drawLine(gold.copy(alpha = 0.5f), Offset(0f, y2), Offset(size.width, y2), 0.8.dp.toPx())
        drawSparkle(Offset(center.x, (y1 + y2) / 2), 5.dp.toPx(), gold)
    }
}

/** App bar on a parchment band, underlined with a [GoldRule]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        GoldRule()
    }
}

/** Fills the screen with parchment that darkens towards the edges, like an old page. */
@Composable
fun WizardBackdrop(content: @Composable BoxScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    val edge = if (isNight()) c.surfaceContainerLowest else c.surfaceContainerHighest
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(c.background, edge),
                        center = center,
                        radius = maxOf(size.width, size.height) * 0.7f,
                    ),
                )
            },
        content = content,
    )
}

/** A gem-coloured round badge with an emoji emblem inside. */
@Composable
fun EmblemBadge(emoji: String, color: Color, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Box(
        modifier
            .size(size)
            .background(color.copy(alpha = 0.22f), CircleShape)
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp, textAlign = TextAlign.Center)
    }
}

/** A section heading flanked by gold rules:  ——— ✦ The Ledger ✦ ——— */
@Composable
fun OrnamentHeading(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        GoldRule(Modifier.weight(1f))
        Text(
            "  $text  ",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        GoldRule(Modifier.weight(1f))
    }
}

/**
 * A main button: a burgundy seal in the same frame as the cards (cut corners, double gold border,
 * corner sparkles), floating on a shadow. Used for Record expense and every other main button.
 */
@Composable
fun WizardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cut: Dp = 12.dp,
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val gold = MaterialTheme.colorScheme.secondary
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else 0.45f),
        shape = CutCornerShape(cut),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(2.dp, gold),
        shadowElevation = if (enabled) elevation else 0.dp,
    ) {
        Row(
            Modifier
                .drawBehind {
                    drawPath(chamfered(5.dp.toPx(), cut.toPx()), gold.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
                    val s = 8.dp.toPx()
                    listOf(Offset(s, s), Offset(size.width - s, s), Offset(s, size.height - s), Offset(size.width - s, size.height - s))
                        .forEach { drawSparkle(it, 3.dp.toPx(), gold) }
                }
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)) {
                content()
            }
        }
    }
}

/** The "Record expense" button: the framed seal, larger, on a deeper shadow. */
@Composable
fun WizardFab(text: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    WizardButton(onClick = onClick, cut = 14.dp, elevation = 12.dp, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
}

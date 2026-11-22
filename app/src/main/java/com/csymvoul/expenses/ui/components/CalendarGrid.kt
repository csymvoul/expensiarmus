package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.domain.Money
import java.time.LocalDate
import java.time.YearMonth

private val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

/**
 * Month grid, weeks starting on Monday. Each day shows what I spent; the more spent,
 * the deeper the ink.
 */
@Composable
fun CalendarGrid(
    month: YearMonth,
    totals: Map<LocalDate, Long>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val cells = remember(month) { ExpenseQueries.calendarCells(month) }
    val max = totals.values.maxOrNull() ?: 0L
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekdays.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(0.85f)) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                spent = totals[day] ?: 0L,
                                ratio = if (max > 0) (totals[day] ?: 0L).toFloat() / max else 0f,
                                isToday = day == today,
                                isSelected = day == selected,
                                onClick = { onSelect(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, spent: Long, ratio: Float, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(6.dp)
    val fill = if (spent > 0) colors.primary.copy(alpha = 0.15f + 0.6f * ratio) else colors.surfaceVariant.copy(alpha = 0.45f)
    val ink = if (spent > 0 && ratio > 0.5f) colors.onPrimary else colors.onSurface
    val outline = when {
        isSelected -> Modifier.border(2.dp, colors.secondary, shape)
        isToday -> Modifier.border(1.dp, colors.primary, shape)
        else -> Modifier
    }
    Column(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(fill)
            .then(outline)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = ink,
        )
        if (spent > 0) {
            Text(Money.compact(spent), fontSize = 10.sp, maxLines = 1, color = ink)
        }
    }
}

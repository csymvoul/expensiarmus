package com.csymvoul.expenses.ui.components

import kotlin.math.atan2
import kotlin.math.hypot

/** Geometry for [DonutChart], kept free of Android so it can be unit-tested. */
object DonutMath {
    /**
     * Index (into [values]) of the slice under a tap at ([x], [y]) on a chart of [width] x [height]
     * whose ring is [thickness] wide, or null when the tap is in the hole, outside the ring or the
     * chart is empty. Slices run clockwise from 12 o'clock; zero values take no space.
     */
    fun sliceAt(values: List<Float>, x: Float, y: Float, width: Float, height: Float, thickness: Float): Int? {
        val total = values.filter { it > 0f }.sum()
        if (total <= 0f) return null
        val radius = (minOf(width, height) - thickness) / 2f
        val distance = hypot(x - width / 2f, y - height / 2f)
        // Accept taps on the ring, with a little slack for fingers.
        if (distance < radius - thickness * 0.8f || distance > radius + thickness * 0.8f) return null
        val fromTop = (Math.toDegrees(atan2((y - height / 2f).toDouble(), (x - width / 2f).toDouble())) + 90.0 + 360.0) % 360.0
        var start = 0.0
        values.forEachIndexed { i, v ->
            if (v <= 0f) return@forEachIndexed
            val sweep = v / total * 360.0
            if (fromTop < start + sweep) return i
            start += sweep
        }
        return values.indexOfLast { it > 0f }
    }
}

/** Geometry for [BarChart]. */
object BarMath {
    /** Index of the bar under a tap at [x] on a chart [width] wide with [count] equal slots. */
    fun barAt(x: Float, width: Float, count: Int): Int =
        if (count <= 0 || width <= 0f) -1 else (x / (width / count)).toInt().coerceIn(0, count - 1)
}

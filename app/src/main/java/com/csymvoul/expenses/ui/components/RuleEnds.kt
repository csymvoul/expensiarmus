package com.csymvoul.expenses.ui.components

/** One star (or a speck when [speck]) of the sparkle clusters at the ends of a gold rule, in dp. */
data class Spark(val x: Float, val dy: Float, val radius: Float, val speck: Boolean)

/**
 * The sparkle clusters at both ends of the header and tab-bar rules: a big star, a small one and
 * two specks at each end, the right end mirroring the left. [dy] is measured from the rule's
 * middle line; [flip] turns the clusters upside down (for the tab bar, which sits under its rule).
 */
fun ruleEndSparks(widthDp: Float, flip: Boolean = false): List<Spark> {
    val left = listOf(
        Spark(16f, 0f, 4.5f, speck = false),
        Spark(30f, -3.5f, 2f, speck = false),
        Spark(38f, 3f, 0.8f, speck = true),
        Spark(5f, -3f, 0.7f, speck = true),
    ).map { if (flip) it.copy(dy = -it.dy) else it }
    return left + left.map { it.copy(x = widthDp - it.x) }
}

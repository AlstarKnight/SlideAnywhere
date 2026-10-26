package io.github.alstarknight.slideanywhere

import kotlin.math.abs

enum class Side { LEFT, RIGHT }

/** True when a drag that started on the edge handle has moved far enough into the screen, mostly horizontally. */
fun isOpenSwipe(side: Side, dx: Float, dy: Float, thresholdPx: Float): Boolean {
    val inward = if (side == Side.LEFT) dx else -dx
    return inward >= thresholdPx && inward > abs(dy)
}

/** Top of the handle so its center sits at [positionPercent] of the screen height, kept fully on screen. */
fun handleTop(screenHeight: Int, handleHeight: Int, positionPercent: Int): Int {
    val center = screenHeight * positionPercent / 100
    return (center - handleHeight / 2).coerceIn(0, (screenHeight - handleHeight).coerceAtLeast(0))
}

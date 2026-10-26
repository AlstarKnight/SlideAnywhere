package io.github.alstarknight.slidex

import kotlin.math.abs
import kotlin.math.roundToInt

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

/** The side edge nearer to a floating icon whose center is at [centerX]. */
fun nearestSide(centerX: Float, screenWidth: Int): Side = if (centerX < screenWidth / 2f) Side.LEFT else Side.RIGHT

/** Left of a floating icon of [size] resting on [side], [margin] away from that edge. */
fun ballLeft(side: Side, screenWidth: Int, size: Int, margin: Int): Int =
    if (side == Side.LEFT) margin else screenWidth - size - margin

/** Top of a floating icon whose center sits at [position] (0 to 1) of the screen height, kept fully on screen. */
fun ballTop(screenHeight: Int, size: Int, position: Float): Int =
    (screenHeight * position - size / 2f).roundToInt().coerceIn(0, (screenHeight - size).coerceAtLeast(0))

/** Inverse of [ballTop]: the fraction of the screen height where the icon's center is. */
fun ballPosition(screenHeight: Int, size: Int, top: Int): Float =
    if (screenHeight <= 0) 0.5f else ((top + size / 2f) / screenHeight).coerceIn(0f, 1f)

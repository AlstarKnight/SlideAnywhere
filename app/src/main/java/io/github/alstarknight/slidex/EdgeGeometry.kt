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

/** Inverse of [handleTop]: where the handle's center is, in percent of the screen height. */
fun handlePositionPercent(screenHeight: Int, handleHeight: Int, top: Int): Int =
    if (screenHeight <= 0) 50 else ((top + handleHeight / 2f) * 100 / screenHeight).roundToInt().coerceIn(0, 100)

/** The side edge nearer to a point at [x], e.g. the center of the floating icon or a finger dragging the handle. */
fun nearestSide(x: Float, screenWidth: Int): Side = if (x < screenWidth / 2f) Side.LEFT else Side.RIGHT

/** Offset of something [size] long whose center sits at [fraction] (0 to 1) of [length], kept fully inside it. */
fun centeredOffset(length: Int, size: Int, fraction: Float): Int =
    (length * fraction - size / 2f).roundToInt().coerceIn(0, (length - size).coerceAtLeast(0))

/** Inverse of [centeredOffset]: where the center is, as a fraction of [length]. */
fun centerFraction(length: Int, size: Int, offset: Int): Float =
    if (length <= 0) 0.5f else ((offset + size / 2f) / length).coerceIn(0f, 1f)

/**
 * Top of something [size] tall placed at [top], moved up to end [gap] above a keyboard whose top edge is at
 * [keyboardTop] when it would otherwise overlap it. Null [keyboardTop] means no keyboard is shown.
 */
fun aboveKeyboard(top: Int, size: Int, keyboardTop: Int?, gap: Int): Int =
    if (keyboardTop == null || top + size + gap <= keyboardTop) top else (keyboardTop - size - gap).coerceAtLeast(0)

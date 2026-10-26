package io.github.alstarknight.slidex

import kotlin.math.roundToInt

/**
 * The cell an icon lands in when it is dragged from cell [from] by ([dx], [dy]) pixels, in a grid of [count] icons
 * laid out [columns] to a row in cells of [cellWidth] by [cellHeight]. Drops past the last icon land on the last cell.
 */
fun gridDropIndex(from: Int, dx: Float, dy: Float, cellWidth: Float, cellHeight: Float, columns: Int, count: Int): Int {
    if (cellWidth <= 0f || cellHeight <= 0f || count <= 0) return from
    val column = (from % columns + (dx / cellWidth).roundToInt()).coerceIn(0, columns - 1)
    val row = (from / columns + (dy / cellHeight).roundToInt()).coerceAtLeast(0)
    return (row * columns + column).coerceIn(0, count - 1)
}

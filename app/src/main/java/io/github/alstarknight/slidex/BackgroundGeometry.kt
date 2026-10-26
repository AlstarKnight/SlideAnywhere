package io.github.alstarknight.slidex

import kotlin.math.max
import kotlin.math.roundToInt

/** A part of a picture, in the picture's own pixels. */
data class PictureArea(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * The middle part of a [pictureWidth] × [pictureHeight] picture that has the shape of a [width] × [height] area, so
 * the picture can fill the area without being stretched.
 */
fun centerCrop(pictureWidth: Int, pictureHeight: Int, width: Float, height: Float): PictureArea {
    if (width <= 0f || height <= 0f) return PictureArea(0, 0, pictureWidth, pictureHeight)
    val scale = max(width / pictureWidth, height / pictureHeight)
    val cropWidth = (width / scale).roundToInt().coerceIn(1, pictureWidth)
    val cropHeight = (height / scale).roundToInt().coerceIn(1, pictureHeight)
    return PictureArea((pictureWidth - cropWidth) / 2, (pictureHeight - cropHeight) / 2, cropWidth, cropHeight)
}

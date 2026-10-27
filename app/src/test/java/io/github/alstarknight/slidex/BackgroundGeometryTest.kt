package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundGeometryTest {

    @Test
    fun widePictureLosesItsSides() {
        // A landscape photo behind a tall panel keeps its middle strip.
        assertEquals(PictureArea(750, 0, 900, 2000), centerCrop(2400, 2000, 900f, 2000f))
    }

    @Test
    fun tallPictureLosesTopAndBottom() {
        assertEquals(PictureArea(0, 500, 1000, 1000), centerCrop(1000, 2000, 500f, 500f))
    }

    @Test
    fun pictureOfTheSameShapeIsKeptWhole() {
        assertEquals(PictureArea(0, 0, 1080, 2400), centerCrop(1080, 2400, 540f, 1200f))
    }

    @Test
    fun emptyAreaKeepsThePictureWhole() {
        assertEquals(PictureArea(0, 0, 300, 200), centerCrop(300, 200, 0f, 100f))
    }
}

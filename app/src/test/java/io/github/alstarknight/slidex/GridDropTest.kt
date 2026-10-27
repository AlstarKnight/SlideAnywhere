package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Test

class GridDropTest {

    // Ten icons, four to a row, in 100 x 120 cells.
    private fun drop(from: Int, dx: Float, dy: Float) = gridDropIndex(from, dx, dy, 100f, 120f, 4, 10)

    @Test
    fun iconsLandInTheNearestCell() {
        assertEquals(2, drop(0, 210f, 0f))
        assertEquals(0, drop(0, 40f, 0f))
        assertEquals(5, drop(1, 0f, 130f))
        assertEquals(4, drop(5, -500f, 0f))
    }

    @Test
    fun dropsOutsideTheGridStayInside() {
        assertEquals(9, drop(0, 0f, 1000f))
        assertEquals(1, drop(9, 0f, -1000f))
        assertEquals(3, drop(0, 1000f, 0f))
    }

    @Test
    fun unmeasuredGridKeepsTheIconInPlace() {
        assertEquals(3, gridDropIndex(3, 50f, 50f, 0f, 0f, 4, 10))
    }
}

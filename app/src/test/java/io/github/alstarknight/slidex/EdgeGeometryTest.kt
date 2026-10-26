package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeGeometryTest {

    @Test
    fun rightHandleOpensOnLeftwardSwipe() {
        assertTrue(isOpenSwipe(Side.RIGHT, dx = -60f, dy = 10f, thresholdPx = 48f))
    }

    @Test
    fun leftHandleOpensOnRightwardSwipe() {
        assertTrue(isOpenSwipe(Side.LEFT, dx = 60f, dy = -10f, thresholdPx = 48f))
    }

    @Test
    fun swipeTowardTheEdgeDoesNotOpen() {
        assertFalse(isOpenSwipe(Side.RIGHT, dx = 60f, dy = 0f, thresholdPx = 48f))
        assertFalse(isOpenSwipe(Side.LEFT, dx = -60f, dy = 0f, thresholdPx = 48f))
    }

    @Test
    fun shortSwipeDoesNotOpen() {
        assertFalse(isOpenSwipe(Side.LEFT, dx = 30f, dy = 0f, thresholdPx = 48f))
    }

    @Test
    fun mostlyVerticalDragDoesNotOpen() {
        assertFalse(isOpenSwipe(Side.LEFT, dx = 60f, dy = 90f, thresholdPx = 48f))
    }

    @Test
    fun handleIsCenteredOnPosition() {
        assertEquals(400, handleTop(screenHeight = 2000, handleHeight = 200, positionPercent = 25))
    }

    @Test
    fun handleStaysOnScreen() {
        assertEquals(0, handleTop(screenHeight = 2000, handleHeight = 200, positionPercent = 0))
        assertEquals(1800, handleTop(screenHeight = 2000, handleHeight = 200, positionPercent = 100))
    }

    @Test
    fun handleTallerThanScreenSticksToTop() {
        assertEquals(0, handleTop(screenHeight = 100, handleHeight = 200, positionPercent = 50))
    }

    @Test
    fun ballSnapsToNearerSide() {
        assertEquals(Side.LEFT, nearestSide(centerX = 400f, screenWidth = 1000))
        assertEquals(Side.RIGHT, nearestSide(centerX = 600f, screenWidth = 1000))
    }

    @Test
    fun ballRestsAgainstItsEdge() {
        assertEquals(10, ballLeft(Side.LEFT, screenWidth = 1000, size = 100, margin = 10))
        assertEquals(890, ballLeft(Side.RIGHT, screenWidth = 1000, size = 100, margin = 10))
    }

    @Test
    fun ballPositionRoundTripsAndStaysOnScreen() {
        assertEquals(950, ballTop(screenHeight = 2000, size = 100, position = 0.5f))
        assertEquals(0.5f, ballPosition(screenHeight = 2000, size = 100, top = 950), 0.0001f)
        assertEquals(0, ballTop(screenHeight = 2000, size = 100, position = 0f))
        assertEquals(1900, ballTop(screenHeight = 2000, size = 100, position = 1f))
    }
}

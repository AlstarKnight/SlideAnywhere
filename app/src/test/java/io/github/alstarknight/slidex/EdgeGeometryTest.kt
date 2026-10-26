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
}

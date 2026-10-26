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
    fun handlePositionIsTheInverseOfHandleTop() {
        assertEquals(25, handlePositionPercent(screenHeight = 2000, handleHeight = 200, top = 400))
        assertEquals(100, handlePositionPercent(screenHeight = 2000, handleHeight = 200, top = 2000))
    }

    @Test
    fun nearestSideSplitsTheScreenInHalf() {
        assertEquals(Side.LEFT, nearestSide(x = 400f, screenWidth = 1000))
        assertEquals(Side.RIGHT, nearestSide(x = 600f, screenWidth = 1000))
    }

    @Test
    fun ballPositionRoundTripsAndStaysOnScreen() {
        assertEquals(950, centeredOffset(length = 2000, size = 100, fraction = 0.5f))
        assertEquals(0.5f, centerFraction(length = 2000, size = 100, offset = 950), 0.0001f)
        assertEquals(0, centeredOffset(length = 2000, size = 100, fraction = 0f))
        assertEquals(1900, centeredOffset(length = 2000, size = 100, fraction = 1f))
    }

    @Test
    fun iconMovesAboveTheKeyboardOnlyWhenItWouldOverlap() {
        assertEquals(1000, aboveKeyboard(top = 1000, size = 100, keyboardTop = null, gap = 20))
        assertEquals(500, aboveKeyboard(top = 500, size = 100, keyboardTop = 1500, gap = 20))
        assertEquals(1380, aboveKeyboard(top = 1600, size = 100, keyboardTop = 1500, gap = 20))
        assertEquals(0, aboveKeyboard(top = 100, size = 100, keyboardTop = 50, gap = 20))
    }
}

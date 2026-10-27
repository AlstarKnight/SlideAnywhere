package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageRankingTest {

    private val usage = listOf(
        AppUsage("chat", lastUsed = 300, foregroundTime = 10),
        AppUsage("video", lastUsed = 100, foregroundTime = 90),
        AppUsage("maps", lastUsed = 200, foregroundTime = 40),
        AppUsage("updater", lastUsed = 400, foregroundTime = 0),
    )

    @Test
    fun recentSortsByLastUse() {
        assertEquals(listOf("chat", "maps", "video"), rankUsage(usage, UsageKind.RECENT, emptySet(), 10))
    }

    @Test
    fun frequentSortsByTimeOnScreen() {
        assertEquals(listOf("video", "maps", "chat"), rankUsage(usage, UsageKind.FREQUENT, emptySet(), 10))
    }

    @Test
    fun excludedAppsAndTheCountAreRespected() {
        assertEquals(listOf("chat", "video"), rankUsage(usage, UsageKind.RECENT, setOf("maps"), 2))
        assertEquals(listOf("video"), rankUsage(usage, UsageKind.FREQUENT, emptySet(), 1))
    }
}

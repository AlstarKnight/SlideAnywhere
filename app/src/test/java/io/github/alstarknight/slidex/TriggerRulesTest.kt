package io.github.alstarknight.slidex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerRulesTest {

    private val listed = setOf("com.tencent.mm")
    private val own = "io.github.alstarknight.slidex"

    @Test
    fun blacklistHidesOnlyInListedApps() {
        assertFalse(triggerAllowedIn("com.tencent.mm", AppListMode.BLACKLIST, listed, own))
        assertTrue(triggerAllowedIn("com.android.chrome", AppListMode.BLACKLIST, listed, own))
    }

    @Test
    fun whitelistShowsOnlyInListedAppsAndSlideX() {
        assertTrue(triggerAllowedIn("com.tencent.mm", AppListMode.WHITELIST, listed, own))
        assertFalse(triggerAllowedIn("com.android.chrome", AppListMode.WHITELIST, listed, own))
        assertTrue(triggerAllowedIn(own, AppListMode.WHITELIST, listed, own))
    }

    @Test
    fun unknownAppShowsTheTrigger() {
        assertTrue(triggerAllowedIn(null, AppListMode.WHITELIST, emptySet(), own))
        assertTrue(triggerAllowedIn(null, AppListMode.BLACKLIST, setOf("x"), own))
    }
}

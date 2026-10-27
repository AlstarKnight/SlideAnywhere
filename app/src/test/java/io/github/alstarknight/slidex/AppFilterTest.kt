package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFilterTest {

    @Test
    fun fullComponentIsSplitIntoPackageAndActivity() {
        assertEquals(
            "com.tencent.mm" to "com.tencent.mm.ui.LauncherUI",
            appFilterComponent("ComponentInfo{com.tencent.mm/com.tencent.mm.ui.LauncherUI}"),
        )
    }

    @Test
    fun relativeActivityGetsThePackagePrefix() {
        assertEquals("com.example" to "com.example.Main", appFilterComponent(" ComponentInfo{com.example/.Main} "))
    }

    @Test
    fun otherEntriesAreIgnored() {
        assertNull(appFilterComponent(":LAUNCHER_ACTION_APP_DRAWER"))
        assertNull(appFilterComponent("ComponentInfo{}"))
        assertNull(appFilterComponent("ComponentInfo{com.example}"))
    }
}

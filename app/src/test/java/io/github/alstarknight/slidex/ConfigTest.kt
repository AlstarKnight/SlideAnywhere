package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigTest {

    private val note = NoteCard(id = "n", title = "便签", text = "hello")
    private val grid = GridCard(
        id = "g",
        title = "应用",
        items = listOf(
            AppItem("微信", "com.tencent.mm", "com.tencent.mm.ui.LauncherUI"),
            LinkItem("扫一扫", "alipays://platformapi/startapp?saId=10000007"),
            ActionItem("锁屏", Action.LOCK_SCREEN),
        ),
    )
    private val config = PanelConfig(pages = listOf(Page(listOf(grid, note)), Page()))

    @Test
    fun jsonRoundTripKeepsEverything() {
        assertEquals(config, parseConfig(config.toJson()))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val text = """{"version":1,"pages":[{"cards":[{"type":"note","id":"n","title":"t","text":"x","color":"red"}]}],"extra":1}"""
        assertEquals(PanelConfig(pages = listOf(Page(listOf(NoteCard("n", "t", "x"))))), parseConfig(text))
    }

    @Test
    fun configWithoutPagesGetsOneEmptyPage() {
        assertEquals(listOf(Page()), parseConfig("""{"pages":[]}""").pages)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidJsonIsRejected() {
        parseConfig("not json")
    }

    @Test
    fun pagesCanBeAddedRemovedAndMoved() {
        val three = config.addPage()
        assertEquals(3, three.pages.size)
        assertEquals(listOf(Page(), Page(listOf(grid, note)), Page()), three.movePage(0, 1).pages)
        assertEquals(three.pages, three.movePage(2, 1).pages)
        assertEquals(listOf(Page(), Page()), three.removePage(0).pages)
    }

    @Test
    fun lastPageIsNeverRemoved() {
        val single = PanelConfig()
        assertEquals(single, single.removePage(0))
    }

    @Test
    fun cardsCanBeAddedMovedRenamedAndRemoved() {
        val added = config.addCard(1, NoteCard("m", "新"))
        assertEquals(listOf(NoteCard("m", "新")), added.pages[1].cards)
        assertEquals(listOf(note, grid), config.moveCard("g", 1).pages[0].cards)
        assertEquals(config, config.moveCard("g", -1))
        assertEquals("工具", config.renameCard("g", "工具").pages[0].cards[0].title)
        assertEquals(listOf(note), config.removeCard("g").pages[0].cards)
    }

    @Test
    fun gridItemsCanBeAddedAndRemoved() {
        val link = LinkItem("百度", "https://www.baidu.com")
        val withLink = config.addItem("g", link)
        assertEquals(link, (withLink.pages[0].cards[0] as GridCard).items.last())
        val removed = config.removeItem("g", 0)
        assertEquals(grid.items.drop(1), (removed.pages[0].cards[0] as GridCard).items)
        assertEquals(config, config.addItem("n", link))
    }

    @Test
    fun noteTextCanBeChanged() {
        assertEquals("bye", (config.setNoteText("n", "bye").pages[0].cards[1] as NoteCard).text)
    }
}

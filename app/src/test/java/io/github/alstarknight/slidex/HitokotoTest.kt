package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HitokotoTest {

    @Test
    fun answerOfTheServiceIsRead() {
        val text = """{"id":1,"hitokoto":"人生若只如初见。","type":"i","from":"木兰花令","from_who":"纳兰性德","length":8}"""
        assertEquals(Hitokoto("人生若只如初见。", "木兰花令", "纳兰性德"), parseHitokoto(text))
    }

    @Test
    fun missingOrNullSourcesAreAllowed() {
        assertEquals(Hitokoto("你好"), parseHitokoto("""{"hitokoto":"你好","from_who":null}"""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun anythingElseIsRejected() {
        parseHitokoto("<html>502 Bad Gateway</html>")
    }

    @Test
    fun attributionNamesWhoAndTheWork() {
        assertEquals("—— 纳兰性德「木兰花令」", Hitokoto("x", "木兰花令", "纳兰性德").attribution())
        assertEquals("——「木兰花令」", Hitokoto("x", "木兰花令", null).attribution())
        assertEquals("—— 纳兰性德", Hitokoto("x", " ", "纳兰性德").attribution())
        assertNull(Hitokoto("x", "", " ").attribution())
    }

    @Test
    fun cacheAsksAgainAfterAMinute() {
        val cache = HitokotoCache
        cache.put(Hitokoto("x"), now = 1_000L)
        assertFalse(cache.isStale(now = 60_999L))
        assertTrue(cache.isStale(now = 61_000L))
    }
}

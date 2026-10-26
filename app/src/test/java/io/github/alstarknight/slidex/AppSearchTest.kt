package io.github.alstarknight.slidex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSearchTest {

    private val syllables = mapOf('微' to "wei", '信' to "xin", '音' to "yin", '乐' to "yue", '地' to "di", '图' to "tu")
    private fun key(label: String, packageName: String = "com.example") = searchKey(label, packageName) { syllables[it] }

    private val wechat = key("微信", "com.tencent.mm")
    private val music = key("QQ音乐", "com.tencent.qqmusic")
    private val maps = key("Google 地图", "com.google.android.apps.maps")

    @Test
    fun keysHoldPinyinAndInitials() {
        assertEquals(SearchKey("微信", "weixin", "wx", "com.tencent.mm"), wechat)
        assertEquals(SearchKey("qq音乐", "qqyinyue", "qqyy", "com.tencent.qqmusic"), music)
        assertEquals("googleditu", maps.pinyin)
    }

    @Test
    fun chineseNamesAreFoundByPinyinAndInitials() {
        assertEquals(4, searchScore("wx", wechat))
        assertEquals(4, searchScore("Wei Xin", wechat))
        assertEquals(3, searchScore("xin", wechat))
        assertEquals(3, searchScore("yy", music))
        assertEquals(4, searchScore("微", wechat))
    }

    @Test
    fun packagesAndLettersInOrderAlsoMatch() {
        assertEquals(2, searchScore("tencent", wechat))
        assertEquals(1, searchScore("ggdt", maps))
        assertNull(searchScore("zfb", wechat))
    }

    @Test
    fun emptyQueryKeepsEverythingInOrder() {
        val apps = listOf(maps, music, wechat)
        assertEquals(apps, searchApps(apps, " ") { it })
    }

    @Test
    fun betterMatchesComeFirst() {
        val weather = key("天气", "com.weixin.weather")
        assertEquals(listOf(wechat, weather), searchApps(listOf(weather, music, wechat), "weixin") { it })
    }
}

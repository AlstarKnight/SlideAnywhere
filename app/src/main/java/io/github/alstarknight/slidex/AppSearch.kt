package io.github.alstarknight.slidex

/** What an app can be found by: its name, the name in pinyin and as initials, and its package. All lowercase. */
data class SearchKey(val label: String, val pinyin: String, val initials: String, val packageName: String)

/**
 * Builds an app's search key. [syllable] gives a Chinese character's pinyin, or null when it is not one or the
 * system cannot tell; other letters and digits are kept as they are, so "QQ音乐" becomes "qqyinyue" and "qqyy".
 */
fun searchKey(label: String, packageName: String, syllable: (Char) -> String?): SearchKey {
    val pinyin = StringBuilder()
    val initials = StringBuilder()
    for (char in label) {
        val part = syllable(char)?.lowercase() ?: if (char.isLetterOrDigit()) char.lowercase() else ""
        pinyin.append(part)
        part.firstOrNull()?.let(initials::append)
    }
    return SearchKey(
        label = label.lowercase().filterNot { it.isWhitespace() },
        pinyin = pinyin.toString(),
        initials = initials.toString(),
        packageName = packageName.lowercase(),
    )
}

/**
 * How well [query] finds an app, higher is better, or null when it does not: a name, pinyin or initials that start
 * with it, then contain it, then a package that contains it, then a name or pinyin that has its letters in order.
 */
fun searchScore(query: String, key: SearchKey): Int? {
    val q = query.lowercase().filterNot { it.isWhitespace() }
    if (q.isEmpty()) return 0
    val texts = listOf(key.label, key.pinyin, key.initials)
    return when {
        texts.any { it.startsWith(q) } -> 4
        texts.any { it.contains(q) } -> 3
        key.packageName.contains(q) -> 2
        isSubsequence(q, key.label) || isSubsequence(q, key.pinyin) -> 1
        else -> null
    }
}

/** The items [query] finds, best match first; equally good matches keep their order. */
fun <T> searchApps(items: List<T>, query: String, key: (T) -> SearchKey): List<T> =
    items
        .mapNotNull { item -> searchScore(query, key(item))?.let { item to it } }
        .sortedByDescending { it.second }
        .map { it.first }

/** Whether every character of [part] appears in [text] in the same order, e.g. "wct" in "wechat". */
private fun isSubsequence(part: String, text: String): Boolean {
    var next = 0
    for (char in text) {
        if (next < part.length && char == part[next]) next++
    }
    return next == part.length
}

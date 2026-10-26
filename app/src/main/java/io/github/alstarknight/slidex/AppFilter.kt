package io.github.alstarknight.slidex

private val COMPONENT = Regex("""ComponentInfo\{([^/\}]+)/([^/\}]+)\}""")

/**
 * Package and activity named by an icon pack's appfilter entry, such as `ComponentInfo{com.example/com.example.Main}`.
 * An activity starting with "." is relative to the package. Returns null for anything else.
 */
fun appFilterComponent(value: String): Pair<String, String>? {
    val (packageName, activity) = COMPONENT.matchEntire(value.trim())?.destructured ?: return null
    return packageName to if (activity.startsWith(".")) packageName + activity else activity
}

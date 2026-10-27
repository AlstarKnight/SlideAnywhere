package io.github.alstarknight.slidex

/** One app's usage over the queried period. */
data class AppUsage(val packageName: String, val lastUsed: Long, val foregroundTime: Long)

/**
 * Packages for a usage card, best first: by last use for [UsageKind.RECENT], by time on screen for
 * [UsageKind.FREQUENT]. Apps that were never on screen and [exclude]d ones are skipped.
 */
fun rankUsage(usage: List<AppUsage>, kind: UsageKind, exclude: Set<String>, count: Int): List<String> =
    usage
        .filter { it.foregroundTime > 0 && it.packageName !in exclude }
        .sortedByDescending { if (kind == UsageKind.RECENT) it.lastUsed else it.foregroundTime }
        .map { it.packageName }
        .distinct()
        .take(count)

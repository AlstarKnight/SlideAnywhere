package io.github.alstarknight.slidex

/** Whether the trigger hides in the listed apps, or shows only in them. */
enum class AppListMode { BLACKLIST, WHITELIST }

/**
 * Whether the trigger may show while [front] is the app in front. A whitelist still shows it in SlideX itself, so
 * its settings can be tried out, and while the app in front is unknown because the accessibility service is off.
 */
fun triggerAllowedIn(front: String?, mode: AppListMode, apps: Set<String>, ownPackage: String): Boolean = when {
    front == null -> true
    mode == AppListMode.BLACKLIST -> front !in apps
    else -> front in apps || front == ownPackage
}

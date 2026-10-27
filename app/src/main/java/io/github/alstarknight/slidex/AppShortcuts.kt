package io.github.alstarknight.slidex

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.text.Collator

/** The shortcuts of one app, as links that open them. */
class ShortcutGroup(val packageName: String, val appLabel: String, val shortcuts: List<LinkItem>)

/**
 * Shortcuts the installed apps declare in their packages, the fixed part of what a long press on their icons shows,
 * that other apps may open. Shortcuts an app adds while it runs are only given to the home screen app, so they are
 * not here. Call off the main thread.
 */
fun installedAppShortcuts(context: Context): List<ShortcutGroup> {
    val pm = context.packageManager
    val collator = Collator.getInstance()
    return pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        PackageManager.GET_META_DATA,
    )
        .filter { it.activityInfo.packageName != context.packageName }
        .filter { it.activityInfo.metaData?.containsKey(SHORTCUTS_META_DATA) == true }
        .distinctBy { it.activityInfo.packageName }
        .mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            val shortcuts = declaredShortcuts(pm, info.activityInfo)
                // An intent that names no app is meant for the app that declares it.
                .onEach { (_, intent) ->
                    if (intent.component == null && intent.`package` == null) intent.setPackage(packageName)
                }
                .filter { (_, intent) -> pm.canOpen(intent) }
                .map { (label, intent) ->
                    // The flags the system starts a shortcut with.
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_TASK_ON_HOME,
                    )
                    LinkItem(label, intent.toUri(Intent.URI_INTENT_SCHEME))
                }
                .distinctBy { it.uri }
            if (shortcuts.isEmpty()) {
                null
            } else {
                ShortcutGroup(packageName, info.loadLabel(pm).toString(), shortcuts)
            }
        }
        .sortedWith { a, b -> collator.compare(a.appLabel, b.appLabel) }
}

private const val SHORTCUTS_META_DATA = "android.app.shortcuts"

// obtainAttributes needs the attribute ids in ascending order.
private val SHORTCUT_ATTRS = intArrayOf(android.R.attr.enabled, android.R.attr.shortcutShortLabel).apply { sort() }

/** The enabled shortcuts in [activity]'s shortcut file with the intent each opens; empty when it cannot be read. */
private fun declaredShortcuts(pm: PackageManager, activity: ActivityInfo): List<Pair<String, Intent>> = try {
    val res = pm.getResourcesForApplication(activity.applicationInfo)
    res.getXml(activity.metaData.getInt(SHORTCUTS_META_DATA)).use { parseShortcuts(res, it) }
} catch (e: PackageManager.NameNotFoundException) {
    emptyList()
} catch (e: XmlPullParserException) {
    emptyList()
} catch (e: IOException) {
    emptyList()
} catch (e: RuntimeException) {
    // Another app's file, so anything can be wrong with it; one bad app must not hide the others.
    emptyList()
}

/** Reads a shortcut file like the system does: each enabled `<shortcut>` opens the last `<intent>` in it. */
private fun parseShortcuts(res: Resources, parser: XmlResourceParser): List<Pair<String, Intent>> {
    val attrs = Xml.asAttributeSet(parser)
    val shortcuts = mutableListOf<Pair<String, Intent>>()
    var inShortcut = false
    var label: String? = null
    var intent: Intent? = null
    while (true) {
        when (parser.next()) {
            XmlPullParser.END_DOCUMENT -> return shortcuts
            XmlPullParser.START_TAG -> when (parser.name) {
                "shortcut" -> {
                    val values = res.obtainAttributes(attrs, SHORTCUT_ATTRS)
                    val enabled = values.getBoolean(SHORTCUT_ATTRS.indexOf(android.R.attr.enabled), true)
                    val shortLabel = values.getString(SHORTCUT_ATTRS.indexOf(android.R.attr.shortcutShortLabel))
                    label = shortLabel.takeIf { enabled }
                    values.recycle()
                    inShortcut = true
                    intent = null
                }
                "intent" -> if (inShortcut) intent = Intent.parseIntent(res, parser, attrs)
            }
            XmlPullParser.END_TAG -> if (parser.name == "shortcut") {
                val shortcutLabel = label
                // Like the system, a shortcut whose intent has no action is left out.
                val shortcutIntent = intent?.takeIf { !it.action.isNullOrEmpty() }
                if (!shortcutLabel.isNullOrBlank() && shortcutIntent != null) {
                    shortcuts += shortcutLabel to shortcutIntent
                }
                inShortcut = false
            }
        }
    }
}

/** Whether another app may start [intent]: it reaches an exported activity that asks for no permission. */
private fun PackageManager.canOpen(intent: Intent): Boolean {
    val activity = resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo ?: return false
    return activity.exported && activity.permission == null
}

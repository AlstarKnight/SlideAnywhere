package io.github.alstarknight.slidex

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.text.Collator

data class IconPackEntry(val packageName: String, val label: String)

/** Third-party icon packs in the common launcher format (an `appfilter.xml` mapping activities to drawables). */
object IconPacks {
    private val PACK_ACTIONS = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")

    private class LoadedPack(val packageName: String, val resources: Resources, val drawables: Map<String, String>)

    @Volatile
    private var loaded: LoadedPack? = null

    /** Installed icon packs, sorted by name. Call off the main thread. */
    fun installed(context: Context): List<IconPackEntry> {
        val pm = context.packageManager
        val collator = Collator.getInstance()
        return PACK_ACTIONS
            .flatMap { pm.queryIntentActivities(Intent(it), 0) }
            .distinctBy { it.activityInfo.packageName }
            .map { IconPackEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** The chosen pack's icon for an activity (or else for its app), or null. Call off the main thread. */
    fun icon(context: Context, packageName: String, activityName: String): Drawable? {
        val pack = pack(context) ?: return null
        val name = pack.drawables["$packageName/$activityName"] ?: pack.drawables[packageName] ?: return null
        // Icon packs are looked up by drawable name, which is what appfilter.xml gives.
        @Suppress("DiscouragedApi")
        val id = pack.resources.getIdentifier(name, "drawable", pack.packageName)
        if (id == 0) return null
        return try {
            ResourcesCompat.getDrawable(pack.resources, id, null)
        } catch (e: Resources.NotFoundException) {
            null
        }
    }

    @Synchronized
    private fun pack(context: Context): LoadedPack? {
        val chosen = Prefs(context).iconPack ?: return null
        loaded?.takeIf { it.packageName == chosen }?.let { return it }
        return try {
            val resources = context.packageManager.getResourcesForApplication(chosen)
            LoadedPack(chosen, resources, readAppFilter(resources, chosen)).also { loaded = it }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Drawable names by "package/activity", plus by package for the first activity of each app. */
    private fun readAppFilter(resources: Resources, packageName: String): Map<String, String> {
        @Suppress("DiscouragedApi")
        val id = resources.getIdentifier("appfilter", "xml", packageName)
        return try {
            if (id != 0) {
                resources.getXml(id).use { parseAppFilter(it) }
            } else {
                resources.assets.open("appfilter.xml").use { stream ->
                    parseAppFilter(Xml.newPullParser().apply { setInput(stream, null) })
                }
            }
        } catch (e: IOException) {
            emptyMap()
        } catch (e: XmlPullParserException) {
            emptyMap()
        }
    }

    private fun parseAppFilter(parser: XmlPullParser): Map<String, String> {
        val drawables = HashMap<String, String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
            val component = parser.getAttributeValue(null, "component")?.let(::appFilterComponent) ?: continue
            val drawable = parser.getAttributeValue(null, "drawable") ?: continue
            drawables.putIfAbsent("${component.first}/${component.second}", drawable)
            drawables.putIfAbsent(component.first, drawable)
        }
        return drawables
    }
}

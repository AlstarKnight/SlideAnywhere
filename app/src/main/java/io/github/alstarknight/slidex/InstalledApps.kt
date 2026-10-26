package io.github.alstarknight.slidex

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.icu.text.Transliterator
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator

data class AppEntry(val label: String, val packageName: String, val activityName: String, val search: SearchKey)

object InstalledApps {
    private val icons = LruCache<String, ImageBitmap>(200)

    /** Launcher activities of all installed apps, sorted by label. Call off the main thread. */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val collator = Collator.getInstance()
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map {
                val label = it.loadLabel(pm).toString()
                val packageName = it.activityInfo.packageName
                AppEntry(
                    label = label,
                    packageName = packageName,
                    activityName = it.activityInfo.name,
                    search = searchKey(label, packageName, Pinyin::syllable),
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun cachedIcon(key: String): ImageBitmap? = icons.get(key)

    /** Forgets loaded icons, e.g. after the icon pack changed. */
    fun clearIconCache() = icons.evictAll()

    /** Icon of an app's activity from the chosen icon pack, else its own, else the app icon. Call off the main thread. */
    fun appIcon(context: Context, packageName: String, activityName: String, sizePx: Int): ImageBitmap? =
        icon("$packageName/$activityName", sizePx) {
            val pm = context.packageManager
            IconPacks.icon(context, packageName, activityName) ?: try {
                pm.getActivityIcon(ComponentName(packageName, activityName))
            } catch (e: PackageManager.NameNotFoundException) {
                try {
                    pm.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
        }

    /** Icon of the app that opens a non-web link (e.g. `alipays://`), or null for web pages. Call off the main thread. */
    fun linkIcon(context: Context, uri: String, sizePx: Int): ImageBitmap? {
        val intent = ItemLauncher.linkIntent(uri) ?: return null
        if (intent.data?.scheme in setOf("http", "https")) return null
        return icon("link:$uri", sizePx) {
            val pm = context.packageManager
            val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            // Several handlers resolve to the system chooser, whose icon says nothing about the link.
            info?.takeIf { it.packageName != "android" }?.loadIcon(pm)
        }
    }

    fun iconKey(item: Item): String? = when (item) {
        is AppItem -> "${item.packageName}/${item.activityName}"
        is LinkItem -> "link:${item.uri}"
        is ActionItem -> null
    }

    private fun icon(key: String, sizePx: Int, loader: () -> Drawable?): ImageBitmap? {
        icons.get(key)?.let { return it }
        val bitmap = loader()?.toBitmap(sizePx, sizePx)?.asImageBitmap() ?: return null
        icons.put(key, bitmap)
        return bitmap
    }
}

/** Pinyin of Chinese characters for app search, from the system's transliterator (Android 10+). */
private object Pinyin {
    private val cache = HashMap<Char, String>()

    @Synchronized
    fun syllable(char: Char): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (Character.UnicodeScript.of(char.code) != Character.UnicodeScript.HAN) return null
        return cache.getOrPut(char) { HanLatin.transliterate(char) }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private object HanLatin {
    private val transliterator = Transliterator.getInstance("Han-Latin; Latin-ASCII")

    fun transliterate(char: Char): String = transliterator.transliterate(char.toString()).trim()
}

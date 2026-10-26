package io.github.alstarknight.slideanywhere

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator

data class AppEntry(val label: String, val packageName: String, val activityName: String)

object InstalledApps {
    private val icons = LruCache<String, ImageBitmap>(200)

    /** Launcher activities of all installed apps, sorted by label. Call off the main thread. */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val collator = Collator.getInstance()
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun cachedIcon(key: String): ImageBitmap? = icons.get(key)

    /** Icon of an app's activity, falling back to the app icon. Call off the main thread. */
    fun appIcon(context: Context, packageName: String, activityName: String, sizePx: Int): ImageBitmap? =
        icon("$packageName/$activityName", sizePx) {
            val pm = context.packageManager
            try {
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

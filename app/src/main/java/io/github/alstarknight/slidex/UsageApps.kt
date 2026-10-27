package io.github.alstarknight.slidex

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.text.Collator

/** Recently and frequently used apps, read from the system's usage history. */
object UsageApps {

    /** Whether the user allowed usage access ("使用情况访问权限") on the system settings page. */
    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** Up to [count] apps used in the last week that can be opened from a launcher. Call off the main thread. */
    fun load(context: Context, kind: UsageKind, count: Int): List<AppItem> {
        val now = System.currentTimeMillis()
        val stats = context.getSystemService(UsageStatsManager::class.java)
            .queryAndAggregateUsageStats(now - HISTORY_MS, now)
        val usage = stats.values.map { AppUsage(it.packageName, it.lastTimeUsed, it.totalTimeInForeground) }
        val pm = context.packageManager
        val launchable = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .groupBy { it.activityInfo.packageName }
        val notLaunchable = usage.map { it.packageName }.filterNot { it in launchable }
        // The launcher is in the foreground between every two apps, so it would always come first.
        val exclude = notLaunchable.toSet() + setOfNotNull(context.packageName, homePackage(context))
        val collator = Collator.getInstance()
        // Names are read only for the apps shown, which is much quicker than for all; an app with several launcher
        // entries shows the one last by name.
        return rankUsage(usage, kind, exclude, count).map { packageName ->
            val (label, activity) = launchable.getValue(packageName)
                .map { it.loadLabel(pm).toString() to it.activityInfo }
                .sortedWith { a, b -> collator.compare(a.first, b.first) }
                .last()
            AppItem(label, activity.packageName, activity.name)
        }
    }

    private fun homePackage(context: Context): String? =
        context.packageManager
            .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName

    private const val HISTORY_MS = 7 * 24 * 60 * 60 * 1000L
}

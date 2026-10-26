package io.github.alstarknight.slidex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs(context).serviceEnabled && Settings.canDrawOverlays(context)) {
            PanelService.start(context)
        }
    }
}

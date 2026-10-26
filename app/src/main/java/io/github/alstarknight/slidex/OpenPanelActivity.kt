package io.github.alstarknight.slidex

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Invisible activity behind the quick settings tile and the home screen shortcut: opens the panel over whatever
 * was on screen, or the app itself while the panel is not switched on.
 */
class OpenPanelActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Prefs(this).serviceEnabled && Settings.canDrawOverlays(this)) {
            PanelService.openPanel(this, 0)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, OpenPanelActivity::class.java).setAction(Intent.ACTION_VIEW)

        /** Asks the launcher to put a shortcut that opens the panel on the home screen. */
        fun pinShortcut(context: Context) {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                Toast.makeText(context, R.string.shortcut_unsupported, Toast.LENGTH_SHORT).show()
                return
            }
            val shortcut = ShortcutInfoCompat.Builder(context, "open_panel_pinned")
                .setShortLabel(context.getString(R.string.shortcut_open_panel_short))
                .setLongLabel(context.getString(R.string.shortcut_open_panel))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_panel))
                .setIntent(intent(context))
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }
}

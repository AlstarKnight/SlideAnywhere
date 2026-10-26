package io.github.alstarknight.slidex

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick settings tile that closes the shade and opens the panel. */
class PanelTileService : TileService() {
    override fun onStartListening() {
        qsTile?.run {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        // Overlays cannot show over the lock screen, so unlock first.
        if (isLocked) unlockAndRun(::openPanel) else openPanel()
    }

    // Before Android 14 the Intent version is the only one; from 14 on the PendingIntent one is used.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openPanel() {
        val intent = OpenPanelActivity.intent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

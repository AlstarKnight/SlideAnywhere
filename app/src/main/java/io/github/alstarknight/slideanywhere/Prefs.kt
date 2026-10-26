package io.github.alstarknight.slideanywhere

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Prefs(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var serviceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_ENABLED, value) }

    var side: Side
        get() = Side.valueOf(prefs.getString(KEY_SIDE, Side.RIGHT.name)!!)
        set(value) = prefs.edit { putString(KEY_SIDE, value.name) }

    /** Vertical center of the handle, in percent of the screen height. */
    var handlePositionPercent: Int
        get() = prefs.getInt(KEY_HANDLE_POSITION, 40)
        set(value) = prefs.edit { putInt(KEY_HANDLE_POSITION, value) }

    var handleHeightDp: Int
        get() = prefs.getInt(KEY_HANDLE_HEIGHT, 160)
        set(value) = prefs.edit { putInt(KEY_HANDLE_HEIGHT, value) }

    var handleWidthDp: Int
        get() = prefs.getInt(KEY_HANDLE_WIDTH, 18)
        set(value) = prefs.edit { putInt(KEY_HANDLE_WIDTH, value) }

    var showHandle: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HANDLE, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_HANDLE, value) }

    var panelWidthPercent: Int
        get() = prefs.getInt(KEY_PANEL_WIDTH, 85)
        set(value) = prefs.edit { putInt(KEY_PANEL_WIDTH, value) }

    companion object {
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val KEY_SIDE = "side"
        const val KEY_HANDLE_POSITION = "handle_position"
        const val KEY_HANDLE_HEIGHT = "handle_height"
        const val KEY_HANDLE_WIDTH = "handle_width"
        const val KEY_SHOW_HANDLE = "show_handle"
        const val KEY_PANEL_WIDTH = "panel_width"
    }
}

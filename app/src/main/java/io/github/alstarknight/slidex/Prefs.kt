package io.github.alstarknight.slidex

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** How the panel is opened: a swipe from the edge strip, or a tap on the floating icon. */
enum class TriggerMode { EDGE, BALL }

enum class BallShape { CIRCLE, SQUARE }

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

    var triggerMode: TriggerMode
        get() = enumPref(KEY_TRIGGER_MODE, TriggerMode.EDGE)
        set(value) = prefs.edit { putString(KEY_TRIGGER_MODE, value.name) }

    var ballShape: BallShape
        get() = enumPref(KEY_BALL_SHAPE, BallShape.CIRCLE)
        set(value) = prefs.edit { putString(KEY_BALL_SHAPE, value.name) }

    var ballSizeDp: Int
        get() = prefs.getInt(KEY_BALL_SIZE, 48)
        set(value) = prefs.edit { putInt(KEY_BALL_SIZE, value) }

    var ballOpacityPercent: Int
        get() = prefs.getInt(KEY_BALL_OPACITY, 100)
        set(value) = prefs.edit { putInt(KEY_BALL_OPACITY, value) }

    /** Screen edge the floating icon rests on; the panel slides in from this side too. */
    var ballSide: Side
        get() = enumPref(KEY_BALL_SIDE, Side.RIGHT)
        set(value) = prefs.edit { putString(KEY_BALL_SIDE, value.name) }

    /** Vertical center of the floating icon, as a fraction of the screen height. */
    var ballPosition: Float
        get() = prefs.getFloat(KEY_BALL_POSITION, 0.6f)
        set(value) = prefs.edit { putFloat(KEY_BALL_POSITION, value) }

    var themePreset: ThemePreset
        get() = enumPref(KEY_THEME_PRESET, ThemePreset.STARRY)
        set(value) = prefs.edit { putString(KEY_THEME_PRESET, value.name) }

    var darkMode: DarkMode
        get() = enumPref(KEY_DARK_MODE, DarkMode.SYSTEM)
        set(value) = prefs.edit { putString(KEY_DARK_MODE, value.name) }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T {
        val name = prefs.getString(key, null)
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    companion object {
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val KEY_SIDE = "side"
        const val KEY_HANDLE_POSITION = "handle_position"
        const val KEY_HANDLE_HEIGHT = "handle_height"
        const val KEY_HANDLE_WIDTH = "handle_width"
        const val KEY_SHOW_HANDLE = "show_handle"
        const val KEY_PANEL_WIDTH = "panel_width"
        const val KEY_TRIGGER_MODE = "trigger_mode"
        const val KEY_BALL_SHAPE = "ball_shape"
        const val KEY_BALL_SIZE = "ball_size"
        const val KEY_BALL_OPACITY = "ball_opacity"
        const val KEY_BALL_SIDE = "ball_side"
        const val KEY_BALL_POSITION = "ball_position"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_DARK_MODE = "dark_mode"
    }
}

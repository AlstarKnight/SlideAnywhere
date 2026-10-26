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

    /** Packages in which the trigger is hidden; telling which app is in front needs the accessibility service. */
    var hiddenApps: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN_APPS, null).orEmpty().toSet()
        set(value) = prefs.edit { putStringSet(KEY_HIDDEN_APPS, value) }

    var hideInLandscape: Boolean
        get() = prefs.getBoolean(KEY_HIDE_IN_LANDSCAPE, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_IN_LANDSCAPE, value) }

    /** Blur what is behind the panel (Android 12+). */
    var panelBlur: Boolean
        get() = prefs.getBoolean(KEY_PANEL_BLUR, false)
        set(value) = prefs.edit { putBoolean(KEY_PANEL_BLUR, value) }

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

    /** Center of the floating icon, as fractions of the screen width and height. */
    val ballX: Float get() = prefs.getFloat(KEY_BALL_X, 1f)
    val ballY: Float get() = prefs.getFloat(KEY_BALL_Y, 0.6f)

    fun setBallPosition(x: Float, y: Float) = prefs.edit {
        putFloat(KEY_BALL_X, x)
        putFloat(KEY_BALL_Y, y)
    }

    /** What a tap on the floating icon does: null opens the panel, otherwise it runs that action. */
    var ballTapAction: Action?
        get() = actionPref(KEY_BALL_TAP)
        set(value) = prefs.edit { putString(KEY_BALL_TAP, value?.name) }

    var ballLongPressAction: Action?
        get() = actionPref(KEY_BALL_LONG_PRESS)
        set(value) = prefs.edit { putString(KEY_BALL_LONG_PRESS, value?.name) }

    /** Moves the edge handle in one write, so it does not jump between the two values. */
    fun setHandlePlacement(side: Side, positionPercent: Int) = prefs.edit {
        putString(KEY_SIDE, side.name)
        putInt(KEY_HANDLE_POSITION, positionPercent)
    }

    /** Whether the service shows its ongoing notification; without it the service is not a foreground service. */
    var showNotification: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOTIFICATION, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_NOTIFICATION, value) }

    var themePreset: ThemePreset
        get() = enumPref(KEY_THEME_PRESET, ThemePreset.STARRY)
        set(value) = prefs.edit { putString(KEY_THEME_PRESET, value.name) }

    var darkMode: DarkMode
        get() = enumPref(KEY_DARK_MODE, DarkMode.SYSTEM)
        set(value) = prefs.edit { putString(KEY_DARK_MODE, value.name) }

    private fun actionPref(key: String): Action? {
        val name = prefs.getString(key, null)
        return Action.entries.firstOrNull { it.name == name }
    }

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
        const val KEY_PANEL_BLUR = "panel_blur"
        const val KEY_HIDDEN_APPS = "hidden_apps"
        const val KEY_HIDE_IN_LANDSCAPE = "hide_in_landscape"
        const val KEY_TRIGGER_MODE = "trigger_mode"
        const val KEY_BALL_SHAPE = "ball_shape"
        const val KEY_BALL_SIZE = "ball_size"
        const val KEY_BALL_OPACITY = "ball_opacity"
        const val KEY_BALL_X = "ball_x"
        const val KEY_BALL_Y = "ball_y"
        const val KEY_BALL_TAP = "ball_tap"
        const val KEY_BALL_LONG_PRESS = "ball_long_press"
        const val KEY_SHOW_NOTIFICATION = "show_notification"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_DARK_MODE = "dark_mode"
    }
}

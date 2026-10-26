package io.github.alstarknight.slidex

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Foreground service that keeps the edge handle on screen and opens the panel from it. */
class PanelService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: Prefs
    private lateinit var windowManager: WindowManager
    private lateinit var launcher: ItemLauncher
    private var handle: EdgeHandleView? = null
    private var panel: PanelWindow? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        windowManager = getSystemService(WindowManager::class.java)
        launcher = ItemLauncher(this)
        // Add the overlay before going foreground: Android 15 only lets an overlay app start a
        // foreground service from the background while one of its overlay windows is visible.
        if (Settings.canDrawOverlays(this)) addHandle()
        prefs.prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Cannot start in foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (handle == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_OPEN_PANEL) openPanel()
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.prefs.unregisterOnSharedPreferenceChangeListener(this)
        panel?.remove()
        handle?.let { windowManager.removeView(it) }
        handle = null
        launcher.release()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateHandle()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updateHandle()
    }

    private fun addHandle() {
        val view = EdgeHandleView(this, onOpen = ::openPanel)
        handle = view
        applyHandleStyle(view)
        windowManager.addView(view, handleLayoutParams())
    }

    private fun updateHandle() {
        val view = handle ?: return
        applyHandleStyle(view)
        windowManager.updateViewLayout(view, handleLayoutParams())
    }

    private fun applyHandleStyle(view: EdgeHandleView) {
        view.side = prefs.side
        view.showStrip = prefs.showHandle
    }

    private fun handleLayoutParams(): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val width = (prefs.handleWidthDp * density).toInt()
        val height = (prefs.handleHeightDp * density).toInt()
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or if (prefs.side == Side.LEFT) Gravity.LEFT else Gravity.RIGHT
            y = handleTop(screenHeight(), height, prefs.handlePositionPercent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }.heightPixels
        }

    private fun openPanel() {
        if (panel != null || handle == null) return
        handle?.visibility = View.GONE
        panel = PanelWindow(
            context = this,
            windowManager = windowManager,
            side = prefs.side,
            widthFraction = prefs.panelWidthPercent / 100f,
            store = ConfigStore.get(this),
            launcher = launcher,
            onOpenSettings = {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onClosed = {
                panel = null
                handle?.visibility = View.VISIBLE
            },
        ).also { it.show() }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val openPanel = PendingIntent.getService(
            this,
            0,
            Intent(this, PanelService::class.java).setAction(ACTION_OPEN_PANEL),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_panel)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openPanel)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "PanelService"
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_OPEN_PANEL = "io.github.alstarknight.slidex.OPEN_PANEL"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PanelService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PanelService::class.java))
        }
    }
}

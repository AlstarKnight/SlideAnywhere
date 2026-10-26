package io.github.alstarknight.slideanywhere

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import java.net.URISyntaxException

val Action.labelRes: Int
    @StringRes get() = when (this) {
        Action.NOTIFICATIONS -> R.string.action_notifications
        Action.QUICK_SETTINGS -> R.string.action_quick_settings
        Action.RECENTS -> R.string.action_recents
        Action.HOME -> R.string.action_home
        Action.BACK -> R.string.action_back
        Action.LOCK_SCREEN -> R.string.action_lock_screen
        Action.SCREENSHOT -> R.string.action_screenshot
        Action.SPLIT_SCREEN -> R.string.action_split_screen
        Action.PLAY_PAUSE -> R.string.action_play_pause
        Action.NEXT_TRACK -> R.string.action_next_track
        Action.PREVIOUS_TRACK -> R.string.action_previous_track
        Action.VOLUME_UP -> R.string.action_volume_up
        Action.VOLUME_DOWN -> R.string.action_volume_down
        Action.FLASHLIGHT -> R.string.action_flashlight
        Action.VOICE_ASSISTANT -> R.string.action_voice_assistant
    }

/** Opens apps and links and runs shortcut actions for panel items. */
class ItemLauncher(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val camera = context.getSystemService(CameraManager::class.java)
    private var torchOn = false
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchOn = enabled
        }
    }

    init {
        camera.registerTorchCallback(torchCallback, handler)
    }

    fun release() {
        camera.unregisterTorchCallback(torchCallback)
        handler.removeCallbacksAndMessages(null)
    }

    fun launch(item: Item) {
        when (item) {
            is AppItem -> start(appIntent(item))
            is LinkItem -> linkIntent(item.uri)?.let(::start) ?: toast(R.string.launch_failed)
            // Let the panel finish closing first so it is not in the screenshot and does not take the back key.
            is ActionItem -> handler.postDelayed({ perform(item.action) }, ACTION_DELAY_MS)
        }
    }

    private fun appIntent(item: AppItem): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(item.packageName, item.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private fun start(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.launch_failed)
        } catch (e: SecurityException) {
            toast(R.string.launch_failed)
        }
    }

    private fun perform(action: Action) {
        when (action) {
            Action.NOTIFICATIONS -> global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            Action.QUICK_SETTINGS -> global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            Action.RECENTS -> global(AccessibilityService.GLOBAL_ACTION_RECENTS)
            Action.HOME -> global(AccessibilityService.GLOBAL_ACTION_HOME)
            Action.BACK -> global(AccessibilityService.GLOBAL_ACTION_BACK)
            Action.SPLIT_SCREEN -> global(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            Action.LOCK_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                toast(R.string.action_needs_android_9)
            }
            Action.SCREENSHOT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                toast(R.string.action_needs_android_9)
            }
            Action.PLAY_PAUSE -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Action.NEXT_TRACK -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            Action.PREVIOUS_TRACK -> mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Action.VOLUME_UP -> volume(AudioManager.ADJUST_RAISE)
            Action.VOLUME_DOWN -> volume(AudioManager.ADJUST_LOWER)
            Action.FLASHLIGHT -> toggleTorch()
            Action.VOICE_ASSISTANT -> start(Intent(Intent.ACTION_VOICE_COMMAND))
        }
    }

    private fun global(action: Int) {
        val service = ActionAccessibilityService.instance
        if (service == null) {
            toast(R.string.action_needs_accessibility)
            start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        service.performGlobalAction(action)
    }

    private fun mediaKey(keyCode: Int) {
        val audio = context.getSystemService(AudioManager::class.java)
        val time = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun volume(direction: Int) {
        context.getSystemService(AudioManager::class.java)
            .adjustSuggestedStreamVolume(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
    }

    private fun toggleTorch() {
        try {
            val id = camera.cameraIdList.firstOrNull {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id == null) {
                toast(R.string.action_failed)
                return
            }
            camera.setTorchMode(id, !torchOn)
        } catch (e: CameraAccessException) {
            toast(R.string.action_failed)
        }
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ACTION_DELAY_MS = 350L

        /** Builds the intent for a link item: `intent:` URIs are parsed, anything else is opened with ACTION_VIEW. */
        fun linkIntent(uri: String): Intent? {
            val text = uri.trim()
            if (text.isEmpty()) return null
            return try {
                if (text.startsWith("intent:") || text.contains("#Intent;")) {
                    Intent.parseUri(text, Intent.URI_INTENT_SCHEME)
                } else {
                    Intent(Intent.ACTION_VIEW, text.toUri())
                }
            } catch (e: URISyntaxException) {
                null
            }
        }
    }
}

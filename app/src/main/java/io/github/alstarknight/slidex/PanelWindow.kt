package io.github.alstarknight.slidex

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** Full-screen overlay window that hosts the sliding panel. Closes on outside tap or back. */
class PanelWindow(
    private val context: Context,
    private val windowManager: WindowManager,
    private val side: Side,
    private val initialPage: Int,
    private val widthFraction: Float,
    private val blurBehind: Boolean,
    private val store: ConfigStore,
    private val launcher: ItemLauncher,
    private val onOpenSettings: () -> Unit,
    private val onClosed: () -> Unit,
) {
    private val owner = OverlayLifecycleOwner()
    private val visibleState = MutableTransitionState(false).apply { targetState = true }
    private var root: PanelRootView? = null

    fun show() {
        owner.start()
        WidgetHost.get(context).startListening()
        val root = PanelRootView(context, onBack = ::onBack)
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.setViewTreeOnBackPressedDispatcherOwner(owner)
        root.addView(ComposeView(context).apply {
            setContent {
                AppTheme {
                    PanelOverlay(
                        visibleState = visibleState,
                        side = side,
                        widthFraction = widthFraction,
                        onDismiss = ::hide,
                        onHidden = ::remove,
                    ) {
                        PanelContent(
                            store = store,
                            launcher = launcher,
                            initialPage = initialPage,
                            onOpenSettings = {
                                hide()
                                onOpenSettings()
                            },
                            onClose = ::hide,
                        )
                    }
                }
            }
        })
        windowManager.addView(root, layoutParams())
        this.root = root
    }

    /** Back first goes to the panel's own handlers (closing a sheet, leaving edit mode), then closes the panel. */
    private fun onBack() {
        if (owner.onBackPressedDispatcher.hasEnabledCallbacks()) {
            owner.onBackPressedDispatcher.onBackPressed()
        } else {
            hide()
        }
    }

    /** Starts the closing animation; the window is removed once it finishes. */
    fun hide() {
        visibleState.targetState = false
    }

    /** Removes the window immediately, without animation. */
    fun remove() {
        val root = root ?: return
        this.root = null
        windowManager.removeView(root)
        owner.destroy()
        WidgetHost.get(context).stopListening()
        onClosed()
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setFitInsetsTypes(0)
        }
        // Let the panel's text fields bring up the keyboard and stay above it.
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        if (blurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            blurBehindRadius = (BLUR_RADIUS_DP * context.resources.displayMetrics.density).toInt()
        }
    }

    private companion object {
        const val BLUR_RADIUS_DP = 24
    }
}

/** Root of the panel window; routes the back gesture or key to [onBack]. */
@SuppressLint("ViewConstructor")
private class PanelRootView(context: Context, private val onBack: () -> Unit) : FrameLayout(context) {
    private var backCallback: Any? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val callback = OnBackInvokedCallback { onBack() }
            findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback,
            )
            backCallback = callback
        }
    }

    override fun onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (backCallback as? OnBackInvokedCallback)?.let {
                findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it)
            }
        }
        super.onDetachedFromWindow()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) onBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

/** Lifecycle, saved-state and back-dispatcher owner that Compose needs when it runs outside an activity. */
private class OverlayLifecycleOwner : SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val onBackPressedDispatcher = OnBackPressedDispatcher()

    fun start() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

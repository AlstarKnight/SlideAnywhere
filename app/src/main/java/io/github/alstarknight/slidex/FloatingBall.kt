package io.github.alstarknight.slidex

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Floating icon that can be dragged anywhere; a tap or a long press opens the panel or runs an action. */
class FloatingBall(
    context: Context,
    private val windowManager: WindowManager,
    private val prefs: Prefs,
    private val onGesture: (action: Action?, side: Side) -> Unit,
) {
    private val density = context.resources.displayMetrics.density
    private val view = BallView(context, onDragStart = ::onDragStart, onDrag = ::onDrag, onDrop = ::onDrop)
    private val params = WindowManager.LayoutParams(
        0,
        0,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    private var dragStartX = 0
    private var dragStartY = 0

    init {
        view.setOnClickListener { onGesture(prefs.ballTapAction, side) }
        view.setOnLongClickListener {
            onGesture(prefs.ballLongPressAction, side)
            true
        }
    }

    var visible: Boolean
        get() = view.visibility == View.VISIBLE
        set(value) {
            view.visibility = if (value) View.VISIBLE else View.GONE
        }

    /** Top edge of the on-screen keyboard, or null when none is shown; the icon moves up out of its way. */
    var keyboardTop: Int? = null
        set(value) {
            if (field == value) return
            field = value
            update()
        }

    /** The edge the icon is nearer to; the panel slides in from there. */
    val side: Side
        get() = nearestSide(params.x + params.width / 2f, windowManager.screenSize().x)

    fun add() {
        applyPrefs()
        windowManager.addView(view, params)
    }

    /** Re-reads size, shape, opacity and position, e.g. after a settings change or a rotation. */
    fun update() {
        if (view.dragging) return
        applyPrefs()
        windowManager.updateViewLayout(view, params)
    }

    fun remove() {
        windowManager.removeView(view)
    }

    private fun applyPrefs() {
        val size = (prefs.ballSizeDp * density).roundToInt()
        val screen = windowManager.screenSize()
        params.width = size
        params.height = size
        params.x = centeredOffset(screen.x, size, prefs.ballX)
        val top = centeredOffset(screen.y, size, prefs.ballY)
        params.y = aboveKeyboard(top, size, keyboardTop, (KEYBOARD_GAP_DP * density).roundToInt())
        view.shape = prefs.ballShape
        view.alpha = prefs.ballOpacityPercent / 100f
    }

    private fun onDragStart() {
        dragStartX = params.x
        dragStartY = params.y
    }

    private fun onDrag(dx: Float, dy: Float) {
        val screen = windowManager.screenSize()
        params.x = (dragStartX + dx.roundToInt()).coerceIn(0, (screen.x - params.width).coerceAtLeast(0))
        params.y = (dragStartY + dy.roundToInt()).coerceIn(0, (screen.y - params.height).coerceAtLeast(0))
        windowManager.updateViewLayout(view, params)
    }

    /** Remembers where the icon was dropped, relative to the screen size so it survives rotation. */
    private fun onDrop() {
        val screen = windowManager.screenSize()
        prefs.setBallPosition(
            centerFraction(screen.x, params.width, params.x),
            centerFraction(screen.y, params.height, params.y),
        )
    }
}

private const val KEYBOARD_GAP_DP = 12

/** Size of the whole screen in pixels, system bars included. */
fun WindowManager.screenSize(): Point =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        currentWindowMetrics.bounds.let { Point(it.width(), it.height()) }
    } else {
        @Suppress("DEPRECATION")
        Point().also { defaultDisplay.getRealSize(it) }
    }

/** The app icon clipped to a circle or a rounded square. */
@SuppressLint("ViewConstructor")
private class BallView(
    context: Context,
    private val onDragStart: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onDrop: () -> Unit,
) : View(context) {

    var shape = BallShape.CIRCLE
        set(value) {
            field = value
            updateClip()
            invalidate()
        }

    var dragging = false
        private set

    private val icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    private val clip = Path()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false
    private val longPress = Runnable {
        longPressed = true
        performLongClick()
    }

    init {
        contentDescription = context.getString(R.string.ball_description)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateClip()
    }

    private fun updateClip() {
        val w = width.toFloat()
        val h = height.toFloat()
        clip.reset()
        if (shape == BallShape.CIRCLE) {
            clip.addOval(0f, 0f, w, h, Path.Direction.CW)
        } else {
            clip.addRoundRect(0f, 0f, w, h, w * SQUARE_CORNER, h * SQUARE_CORNER, Path.Direction.CW)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val drawable = icon ?: return
        canvas.save()
        canvas.clipPath(clip)
        if (drawable is AdaptiveIconDrawable) {
            // The layers are 108dp with the visible 72dp in the middle, so they overhang a quarter on each side.
            val inset = width / 4
            for (layer in listOfNotNull(drawable.background, drawable.foreground)) {
                layer.setBounds(-inset, -inset, width + inset, height + inset)
                layer.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                longPressed = false
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && !longPressed && hypot(dx, dy) > touchSlop) {
                    removeCallbacks(longPress)
                    dragging = true
                    onDragStart()
                }
                if (dragging) onDrag(dx, dy)
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPress)
                when {
                    dragging -> {
                        dragging = false
                        onDrop()
                    }
                    !longPressed -> performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                if (dragging) {
                    dragging = false
                    onDrop()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private companion object {
        /** Corner radius of the square shape, as a fraction of the icon size. */
        const val SQUARE_CORNER = 0.22f
    }
}

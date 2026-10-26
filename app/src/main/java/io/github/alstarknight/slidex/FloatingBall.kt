package io.github.alstarknight.slidex

import android.animation.ValueAnimator
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
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Draggable floating icon that opens the panel when tapped and snaps to the nearer side edge when dropped. */
class FloatingBall(
    context: Context,
    private val windowManager: WindowManager,
    private val prefs: Prefs,
    private val onOpen: (Side) -> Unit,
) {
    private val density = context.resources.displayMetrics.density
    private val margin = (4 * density).roundToInt()
    private val view = BallView(context, onDragStart = ::onDragStart, onDrag = ::onDrag, onDrop = ::snap)
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
    private var snapAnimator: ValueAnimator? = null

    init {
        view.setOnClickListener { onOpen(prefs.ballSide) }
    }

    var visible: Boolean
        get() = view.visibility == View.VISIBLE
        set(value) {
            view.visibility = if (value) View.VISIBLE else View.GONE
        }

    fun add() {
        applyPrefs()
        windowManager.addView(view, params)
    }

    /** Re-reads size, shape, opacity and position, e.g. after a settings change or a rotation. */
    fun update() {
        if (snapAnimator != null || view.dragging) return
        applyPrefs()
        windowManager.updateViewLayout(view, params)
    }

    fun remove() {
        snapAnimator?.cancel()
        windowManager.removeView(view)
    }

    private fun applyPrefs() {
        val size = (prefs.ballSizeDp * density).roundToInt()
        val screen = windowManager.screenSize()
        params.width = size
        params.height = size
        params.x = ballLeft(prefs.ballSide, screen.x, size, margin)
        params.y = ballTop(screen.y, size, prefs.ballPosition)
        view.shape = prefs.ballShape
        view.alpha = prefs.ballOpacityPercent / 100f
    }

    private fun onDragStart() {
        snapAnimator?.cancel()
        dragStartX = params.x
        dragStartY = params.y
    }

    private fun onDrag(dx: Float, dy: Float) {
        val screen = windowManager.screenSize()
        params.x = (dragStartX + dx.roundToInt()).coerceIn(0, (screen.x - params.width).coerceAtLeast(0))
        params.y = (dragStartY + dy.roundToInt()).coerceIn(0, (screen.y - params.height).coerceAtLeast(0))
        windowManager.updateViewLayout(view, params)
    }

    /** Slides the icon to the nearer side edge, then remembers where it rests. */
    private fun snap() {
        val screen = windowManager.screenSize()
        val size = params.width
        val side = nearestSide(params.x + size / 2f, screen.x)
        snapAnimator = ValueAnimator.ofInt(params.x, ballLeft(side, screen.x, size, margin)).apply {
            duration = SNAP_DURATION_MS
            addUpdateListener {
                params.x = it.animatedValue as Int
                if (view.isAttachedToWindow) windowManager.updateViewLayout(view, params)
            }
            doOnEnd {
                snapAnimator = null
                prefs.ballSide = side
                prefs.ballPosition = ballPosition(screen.y, size, params.y)
            }
            start()
        }
    }

    private companion object {
        const val SNAP_DURATION_MS = 200L
    }
}

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
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && hypot(dx, dy) > touchSlop) {
                    dragging = true
                    onDragStart()
                }
                if (dragging) onDrag(dx, dy)
            }
            MotionEvent.ACTION_UP -> if (dragging) {
                dragging = false
                onDrop()
            } else {
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                onDrop()
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

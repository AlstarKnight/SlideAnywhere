package io.github.alstarknight.slidex

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * Thin touch strip on the screen edge. A swipe from it into the screen opens the panel ([onOpen] with the finger's
 * screen x); the rest of the swipe goes to [onDrag], and [onRelease] gets its sideways speed when it is let go.
 * After a long press the strip follows the finger instead ([onMove] with screen coordinates), and [onMoveEnd] fires
 * when it is let go.
 */
@SuppressLint("ViewConstructor")
class EdgeHandleView(
    context: Context,
    private val onOpen: (rawX: Float) -> Unit,
    private val onDrag: (rawX: Float) -> Unit,
    private val onRelease: (velocityX: Float) -> Unit,
    private val onMove: (rawX: Float, rawY: Float) -> Unit,
    private val onMoveEnd: () -> Unit,
) : View(context) {

    var side = Side.RIGHT
        set(value) {
            field = value
            invalidate()
        }

    var showStrip = true
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val openThresholdPx = 24 * density
    private val stripWidthPx = 4 * density
    private val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99888888.toInt() }
    private val movingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66888888 }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var opened = false
    private var moving = false
    private var velocityTracker: VelocityTracker? = null
    private val startMoving = Runnable {
        moving = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            velocityTracker?.recycle()
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                opened = false
                moving = false
                postDelayed(startMoving, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (moving) {
                    onMove(event.rawX, event.rawY)
                } else {
                    if (hypot(dx, dy) > touchSlop) removeCallbacks(startMoving)
                    if (opened) {
                        onDrag(event.rawX)
                    } else if (isOpenSwipe(side, dx, dy, openThresholdPx)) {
                        opened = true
                        onOpen(event.rawX)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(startMoving)
                if (moving) {
                    moving = false
                    invalidate()
                    onMoveEnd()
                }
                val tracker = velocityTracker
                velocityTracker = null
                if (opened && tracker != null) {
                    tracker.computeCurrentVelocity(1000)
                    onRelease(if (event.actionMasked == MotionEvent.ACTION_UP) tracker.xVelocity else 0f)
                }
                tracker?.recycle()
            }
        }
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Keep the system back gesture from taking swipes that start on the handle.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (moving) {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), stripWidthPx, stripWidthPx, movingPaint)
        }
        if (!showStrip && !moving) return
        val left = if (side == Side.LEFT) 0f else width - stripWidthPx
        canvas.drawRoundRect(left, 0f, left + stripWidthPx, height.toFloat(), stripWidthPx, stripWidthPx, stripPaint)
    }
}

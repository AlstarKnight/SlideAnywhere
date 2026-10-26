package io.github.alstarknight.slidex

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * Thin touch strip on the screen edge. A swipe from it into the screen opens the panel; after a long press it
 * follows the finger instead ([onMove] with screen coordinates), and [onMoveEnd] fires when it is let go.
 */
@SuppressLint("ViewConstructor")
class EdgeHandleView(
    context: Context,
    private val onOpen: () -> Unit,
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
    private val startMoving = Runnable {
        moving = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
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
                    if (!opened && isOpenSwipe(side, dx, dy, openThresholdPx)) {
                        opened = true
                        onOpen()
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

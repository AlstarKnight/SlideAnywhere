package io.github.alstarknight.slideanywhere

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View

/** Thin touch strip on the screen edge; a swipe from it into the screen opens the panel. */
@SuppressLint("ViewConstructor")
class EdgeHandleView(context: Context, private val onOpen: () -> Unit) : View(context) {

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

    private var downX = 0f
    private var downY = 0f
    private var opened = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                opened = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!opened && isOpenSwipe(side, event.rawX - downX, event.rawY - downY, openThresholdPx)) {
                    opened = true
                    onOpen()
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
        if (!showStrip) return
        val left = if (side == Side.LEFT) 0f else width - stripWidthPx
        canvas.drawRoundRect(left, 0f, left + stripWidthPx, height.toFloat(), stripWidthPx, stripWidthPx, stripPaint)
    }
}

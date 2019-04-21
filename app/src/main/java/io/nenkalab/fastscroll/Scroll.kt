@file:Suppress("KDocMissingDocumentation")

package io.nenkalab.fastscroll

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import java.lang.reflect.Method
import kotlin.math.roundToInt


class Scroll : Slider {

    private var mIsDragging: Boolean = false
    private var mMethodSetProgressFromUser: Method? = null
    private var mRotationAngle = SET_START

    var rotationAngle: Int
        get() = mRotationAngle
        set(angle) {
            if (!isValidRotationAngle(angle))
                throw IllegalArgumentException("Invalid angle specified :$angle")

            if (mRotationAngle == angle) return

            mRotationAngle = angle

            if (useViewRotation()) wrapper?.applyViewRotation()
            else requestLayout()
        }

    private val wrapper: ScrollWrapper?
        get() {
            parent.let {
                return if (it is ScrollWrapper) it
                else null
            }

        }

    constructor(context: Context) : super(context) {
        initialize(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initialize(context, attrs, defStyle, 0)
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initialize(context, attrs, defStyle, defStyleRes)
    }

    private fun initialize(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        ViewCompat.setLayoutDirection(this, ViewCompat.LAYOUT_DIRECTION_LTR)

        if (attrs != null) {
            val attr = context.obtainStyledAttributes(attrs, R.styleable.Scroll, defStyleAttr, defStyleRes)
            val rotationAngle = attr.getInteger(R.styleable.Scroll_scrollRotation, 0)
            if (isValidRotationAngle(rotationAngle)) {
                mRotationAngle = rotationAngle
            }
            attr.recycle()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isEnabled) {
            var direction = 0

            if (
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        direction = if (mRotationAngle == SET_START) 1 else -1
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        direction = if (mRotationAngle == SET_END) 1 else -1
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT ->
                        return false
                    else -> false
                }
            ) {
                val keyProgressIncrement = keyProgressIncrement
                var progress = progress

                progress += direction * keyProgressIncrement

                if (progress in 0..max) {
                    setProgressFromUser(progress, true)
                }

                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (!useViewRotation()) refreshThumb()
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (useViewRotation())
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        else {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec)

            val lp = layoutParams

            if (isInEditMode && lp != null && lp.height >= 0)
                setMeasuredDimension(super.getMeasuredHeight(), lp.height)
            else
                setMeasuredDimension(super.getMeasuredHeight(), super.getMeasuredWidth())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (useViewRotation()) super.onSizeChanged(w, h, oldw, oldh)
        else super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        if (!useViewRotation()) {
            when (mRotationAngle) {
                SET_START -> {
                    canvas.rotate(90f)
                    canvas.translate(0f, (-super.getWidth()).toFloat())
                }
                SET_END -> {
                    canvas.rotate(-90f)
                    canvas.translate((-super.getHeight()).toFloat(), 0f)
                }
            }
        }

        super.onDraw(canvas)
    }

    fun onDTouchEvent(event: MotionEvent): Boolean {
        return if (useViewRotation())
            onTouchEventUseViewRotation(event)
        else
            onTouchEventTraditionalRotation(event)
    }

    fun useViewRotation(): Boolean {
        return !isInEditMode
    }

    private fun refreshThumb() {
        onSizeChanged(super.getWidth(), super.getHeight(), 0, 0)
    }

    private fun isValidRotationAngle(angle: Int): Boolean {
        return angle == SET_START || angle == SET_END
    }

    private fun attemptClaimDrag(active: Boolean) {
        val parent = parent
        parent?.requestDisallowInterceptTouchEvent(active)
    }

    private fun onStartTrackingTouch() {
        mIsDragging = true
    }

    private fun onStopTrackingTouch() {
        mIsDragging = false
    }

    private fun onTouchEventTraditionalRotation(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                onStartTrackingTouch()
                trackTouchEvent(event)
                attemptClaimDrag(true)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> if (mIsDragging) {
                trackTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                if (mIsDragging) {
                    trackTouchEvent(event)
                    onStopTrackingTouch()
                    isPressed = false
                } else {
                    onStartTrackingTouch()
                    trackTouchEvent(event)
                    onStopTrackingTouch()
                    attemptClaimDrag(false)
                }
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                if (mIsDragging) {
                    onStopTrackingTouch()
                    isPressed = false
                }
                invalidate()

            }
        }
        return true
    }

    private fun onTouchEventUseViewRotation(event: MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)

        if (handled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> attemptClaimDrag(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> attemptClaimDrag(false)
            }
        }

        return handled
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val paddingLeft = super.getPaddingLeft()
        val paddingRight = super.getPaddingRight()
        val height = height

        val available = height - paddingLeft - paddingRight
        val y = event.y.roundToInt()

        val scale: Float
        var value = 0f

        when (mRotationAngle) {
            SET_START -> value = (y - paddingLeft).toFloat()
            SET_END -> value = (height - paddingLeft - y).toFloat()
        }

        scale = if (value < 0 || available == 0) 0.0f
        else if (value > available) 1.0f
        else value / available.toFloat()

        val max = max
        val progress = scale * max

        setProgressFromUser(progress.toInt(), true)
    }

    @Synchronized
    private fun setProgressFromUser(progress: Int, fromUser: Boolean) {
        if (mMethodSetProgressFromUser == null) {
            try {
                val m: Method = ProgressBar::class.java.getDeclaredMethod(
                    "setProgress",
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType
                )
                m.isAccessible = true
                mMethodSetProgressFromUser = m
            } catch (e: NoSuchMethodException) {
            }

        }

        if (mMethodSetProgressFromUser != null) try {
            mMethodSetProgressFromUser!!.invoke(this, progress, fromUser)
        } catch (ignore: IllegalArgumentException) {
        } else super.setProgress(progress)
        refreshThumb()
    }

    companion object {
        const val SET_START: Int = 90
        const val SET_END: Int = -90
    }
}
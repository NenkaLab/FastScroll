@file:Suppress("KDocMissingDocumentation")

package io.nenkalab.fastscroll

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

import androidx.core.view.ViewCompat

open class ScrollWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val childScroll: Scroll?
        get() {
            val child = if (childCount > 0) getChildAt(0) else null
            return if (child is Scroll) child else null
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (useViewRotation()) {
            onSizeChangedUseViewRotation(w, h, oldw, oldh)
        } else {
            onSizeChangedTraditionalRotation(w, h, oldw, oldh)
        }
    }

    private fun onSizeChangedTraditionalRotation(w: Int, h: Int, oldw: Int, oldh: Int) {
        val seekBar = childScroll

        if (seekBar != null) {
            val hPadding = paddingLeft + paddingRight
            val vPadding = paddingTop + paddingBottom
            val lp = seekBar.layoutParams as LayoutParams

            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.height = Math.max(0, h - vPadding)
            seekBar.layoutParams = lp

            seekBar.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)

            val seekBarMeasuredWidth = seekBar.measuredWidth
            seekBar.measure(
                MeasureSpec.makeMeasureSpec(Math.max(0, w - hPadding), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Math.max(0, h - vPadding), MeasureSpec.EXACTLY)
            )

            lp.gravity = Gravity.TOP or Gravity.START
            lp.leftMargin = (Math.max(0, w - hPadding) - seekBarMeasuredWidth) / 2
            seekBar.layoutParams = lp
        }

        super.onSizeChanged(w, h, oldw, oldh)
    }

    private fun onSizeChangedUseViewRotation(w: Int, h: Int, oldw: Int, oldh: Int) {
        val seekBar = childScroll

        if (seekBar != null) {
            val hPadding = paddingLeft + paddingRight
            val vPadding = paddingTop + paddingBottom
            seekBar.measure(
                MeasureSpec.makeMeasureSpec(Math.max(0, h - vPadding), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.max(0, w - hPadding), MeasureSpec.AT_MOST)
            )
        }

        applyViewRotation(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val seekBar: Scroll? = childScroll
        val widthMode: Int = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode: Int = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize: Int = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize: Int = MeasureSpec.getSize(heightMeasureSpec)

        if (seekBar != null && widthMode != MeasureSpec.EXACTLY) {
            val seekBarWidth: Int
            val seekBarHeight: Int
            val hPadding = paddingLeft + paddingRight
            val vPadding = paddingTop + paddingBottom
            val innerContentWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(Math.max(0, widthSize - hPadding), widthMode)
            val innerContentHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(Math.max(0, heightSize - vPadding), heightMode)

            if (useViewRotation()) {
                seekBar.measure(innerContentHeightMeasureSpec, innerContentWidthMeasureSpec)
                seekBarWidth = seekBar.measuredHeight
                seekBarHeight = seekBar.measuredWidth
            } else {
                seekBar.measure(innerContentWidthMeasureSpec, innerContentHeightMeasureSpec)
                seekBarWidth = seekBar.measuredWidth
                seekBarHeight = seekBar.measuredHeight
            }

            val measuredWidth = View.resolveSizeAndState(seekBarWidth + hPadding, widthMeasureSpec, 0)
            val measuredHeight = View.resolveSizeAndState(seekBarHeight + vPadding, heightMeasureSpec, 0)

            setMeasuredDimension(measuredWidth, measuredHeight)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    internal fun applyViewRotation() {
        applyViewRotation(width, height)
    }

    private fun applyViewRotation(w: Int, h: Int) {
        val seekBar = childScroll

        if (seekBar != null) {
            val isLTR = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR
            val rotationAngle = seekBar.rotationAngle
            val seekBarMeasuredWidth = seekBar.measuredWidth
            val seekBarMeasuredHeight = seekBar.measuredHeight
            val hPadding = paddingLeft + paddingRight
            val vPadding = paddingTop + paddingBottom
            val hOffset = (Math.max(0, w - hPadding) - seekBarMeasuredHeight) * 0.5f
            val lp = seekBar.layoutParams

            lp.width = Math.max(0, h - vPadding)
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT

            seekBar.layoutParams = lp

            seekBar.pivotX = if (isLTR) 0F else Math.max(0, h - vPadding).toFloat()
            seekBar.pivotY = 0F

            when (rotationAngle) {
                Scroll.SET_START -> {
                    seekBar.rotation = 90F
                    if (isLTR) {
                        seekBar.translationX = seekBarMeasuredHeight + hOffset
                        seekBar.translationY = 0F
                    } else {
                        seekBar.translationX = -hOffset
                        seekBar.translationY = seekBarMeasuredWidth.toFloat()
                    }
                }
                Scroll.SET_END -> {
                    seekBar.rotation = -90F
                    if (isLTR) {
                        seekBar.translationX = hOffset
                        seekBar.translationY = seekBarMeasuredWidth.toFloat()
                    } else {
                        seekBar.translationX = -(seekBarMeasuredHeight + hOffset)
                        seekBar.translationY = 0F
                    }
                }
            }
        }
    }

    private fun useViewRotation(): Boolean {
        val seekBar = childScroll
        return seekBar?.useViewRotation() ?: false
    }
}
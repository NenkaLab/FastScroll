@file:Suppress("KDocMissingDocumentation")

package io.nenkalab.fastscroll

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.view.ViewTreeObserver.*
import android.view.animation.AccelerateInterpolator
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import java.util.*
import kotlin.concurrent.timerTask


@Suppress("unused")
class FastScroll : RelativeLayout {

    constructor(context: Context) : super(context) {
        initView(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        initView(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs, defStyleAttr, 0)
    }

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        initView(context, attrs, defStyleAttr, defStyleRes)
    }

    private var mScrollId: Int? = null

    private var mHandleWrapper: ScrollWrapper? = null
    private var mHandle: Scroll? = null
    private var mScroll: View? = null
    private var mTimer: Timer? = null
    private var mAnimator: ValueAnimator? = null
    private var mAutoHide: Boolean = true
    private var mOnGlobalLayoutListener: OnGlobalLayoutListener? = null
    private var mOnScrollChangedListener: OnScrollChangedListener? = null

    private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val view = LayoutInflater.from(context).inflate(R.layout.scroll_view, this, false)

        mHandleWrapper = view as ScrollWrapper
        mHandle = view.findViewById(R.id.handle)

        addView(view)

        mTimer = Timer()
        safeHide()

        if (attrs != null) {
            val attr = context.obtainStyledAttributes(attrs, R.styleable.FastScroll, defStyleAttr, defStyleRes)
            val autoHide = attr.getBoolean(R.styleable.FastScroll_autoHide, true)
            mScrollId = attr.getResourceId(R.styleable.FastScroll_scroll, -1)
            val thumb = attr.getResourceId(R.styleable.FastScroll_scrollThumb, -1)
            val thumbTint = attr.getColor(R.styleable.FastScroll_scrollThumbTint, -1)
            val thumbTintMode = attr.getInt(R.styleable.FastScroll_scrollThumbTintMode, -1)
            val thumbColor = attr.getColor(R.styleable.FastScroll_scrollThumbColor, -1)
            val rotationAngle = attr.getInteger(R.styleable.FastScroll_scrollThumbRotation, Scroll.SET_START)

            mHandle?.rotationAngle = rotationAngle
            mAutoHide = autoHide

            if (thumb != -1) mHandle?.thumb = ContextCompat.getDrawable(context, thumb)
            if (thumbTint != -1) mHandle?.thumbTintList = ColorStateList.valueOf(thumbTint)
            if (thumbTintMode != -1) mHandle?.thumbTintMode = intToMode(thumbTintMode)
            if (thumbColor != -1) {
                mHandle?.thumbTintList = ColorStateList.valueOf(thumbColor)
                mHandle?.thumbTintMode = PorterDuff.Mode.SRC_IN
            }
            attr.recycle()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (mScrollId != null)
            if (mScrollId != -1) {
                setScrollRes(mScrollId!!, parent as? ViewGroup)
                mScrollId = null
            }
    }

    private fun setScrollRes(scroll: Int, parent: ViewGroup?) {
        if (parent != null) {
            val view = parent.findViewById<View>(scroll)
            if (view != null) {
                reset()
                mScroll = view
                ready()
            } else {
                setScrollRes(scroll, parent.parent as? ViewGroup)
            }
        }
    }

    fun setScroll(view: ScrollView) {
        reset()
        mScroll = view
        ready()
    }

    fun setScroll(view: NestedScrollView) {
        reset()
        mScroll = view
        ready()
    }

    fun setScroll(view: EditText) {
        reset()
        mScroll = view
        ready()
    }

    fun setScroll(view: TextInputEditText) {
        reset()
        mScroll = view
        ready()
    }

    fun setScroll(view: TextView) {
        reset()
        mScroll = view
        ready()
    }

    fun getScroll(): View? = mScroll

    fun setThumbTintList(colorStateList: ColorStateList?) {
        mHandle?.thumbTintList = colorStateList
    }

    fun getThumbTintList(): ColorStateList? = mHandle?.thumbTintList

    fun setThumb(thumb: Drawable?) {
        mHandle?.thumb = thumb
    }

    fun setThumbResource(@DrawableRes thumb: Int) {
        mHandle?.thumb = ContextCompat.getDrawable(context, thumb)
    }

    fun getThumb(): Drawable? = mHandle?.thumb

    fun setThumbTintMode(mode: PorterDuff.Mode) {
        mHandle?.thumbTintMode = mode
    }

    fun getThumbTintMode(): PorterDuff.Mode? = mHandle?.thumbTintMode

    private fun ready() {
        val view = mScroll
        val sh = fun() { safeHide() }
        mOnGlobalLayoutListener = OnGlobalLayoutListener {
            if (view.has()) {
                mHandle?.max = when (view) {
                    is ScrollView -> view.getScrollRange()
                    is NestedScrollView -> view.getScrollRange()
                    is EditText -> view.getScrollRange()
                    is TextInputEditText -> view.getScrollRange()
                    else -> (view as TextView).getScrollRange()
                }
                mHandle?.progress = view.scrollY
            }
            if (mAutoHide) {
                show()
                safeHide()
            }
        }
        mOnScrollChangedListener = OnScrollChangedListener {
            mHandle?.progress = view?.scrollY ?: 0
            if (mAutoHide) {
                show()
                safeHide()
            }
        }
        mScroll?.viewTreeObserver.run {
            this?.addOnGlobalLayoutListener(mOnGlobalLayoutListener)
            this?.addOnScrollChangedListener(mOnScrollChangedListener)
        }
        mHandle?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                view?.scrollBy(view.scrollX, view.scrollY)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                view?.scrollTo(view.scrollX, progress)
                sh()
            }
        })
    }

    private fun reset() {
        mScroll?.viewTreeObserver.run {
            if (mOnGlobalLayoutListener != null) this?.removeOnGlobalLayoutListener(mOnGlobalLayoutListener)
            if (mOnScrollChangedListener != null) this?.removeOnScrollChangedListener(mOnScrollChangedListener)
        }
        mOnGlobalLayoutListener = null
        mOnScrollChangedListener = null
        safeHide()
    }

    private fun safeHide() {
        if (!mAutoHide) return
        mTimer?.cancel()
        mTimer = Timer()
        mTimer?.schedule(timerTask {
            try {
                this@FastScroll.post {
                    try {
                        hide()
                    } catch (ignore: Exception) {
                    }
                }
            } catch (ignore: Exception) {
            }
        }, 1000)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun show() {
        val view = mHandle
        if (view?.visibility == View.VISIBLE) return
        val animator = mAnimator
        mAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                view?.alpha = (it.animatedValue as Float)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    animator?.cancel()
                    view?.visibility = View.VISIBLE
                }
            })
        }
        mAnimator?.start()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun hide() {
        val view = mHandle
        if (view?.visibility != View.VISIBLE) return
        val animator = mAnimator
        mAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                view.alpha = (it.animatedValue as Float)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    animator?.cancel()
                }

                override fun onAnimationEnd(animation: Animator?) {
                    view.visibility = View.INVISIBLE
                }
            })
        }
        mAnimator?.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) return mHandle?.onDTouchEvent(event) ?: super.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun intToMode(value: Int): PorterDuff.Mode {
        when (value) {
            0 -> return PorterDuff.Mode.CLEAR
            1 -> return PorterDuff.Mode.SRC
            2 -> return PorterDuff.Mode.DST
            3 -> return PorterDuff.Mode.SRC_OVER
            4 -> return PorterDuff.Mode.DST_OVER
            5 -> return PorterDuff.Mode.SRC_IN
            6 -> return PorterDuff.Mode.DST_IN
            7 -> return PorterDuff.Mode.SRC_OUT
            8 -> return PorterDuff.Mode.DST_OUT
            9 -> return PorterDuff.Mode.SRC_ATOP
            10 -> return PorterDuff.Mode.DST_ATOP
            11 -> return PorterDuff.Mode.XOR
            12 -> return PorterDuff.Mode.ADD
            13 -> return PorterDuff.Mode.MULTIPLY
            14 -> return PorterDuff.Mode.SCREEN
            15 -> return PorterDuff.Mode.OVERLAY
            16 -> return PorterDuff.Mode.DARKEN
            17 -> return PorterDuff.Mode.LIGHTEN
            else -> return PorterDuff.Mode.CLEAR
        }
    }
}
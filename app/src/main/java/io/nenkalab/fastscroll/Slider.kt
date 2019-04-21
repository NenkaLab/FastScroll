@file:Suppress("KDocMissingDocumentation")

package io.nenkalab.fastscroll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar


open class Slider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private var mThumb: Drawable? = null

    override fun setThumb(thumb: Drawable?) {
        super.setThumb(thumb)
        mThumb = thumb
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        /**
         *          T
         *          ㅡ
         *
         *  L               R
         *  |       E       |
         *
         *
         *          ㅡ
         *          B
         */
        if (event.action == MotionEvent.ACTION_DOWN)
            if (thumb != null)
                if (event.x >= mThumb!!.bounds.left
                    && event.x <= mThumb!!.bounds.right
                    && event.y <= mThumb!!.bounds.bottom
                    && event.y >= mThumb!!.bounds.top
                ) super.onTouchEvent(event)
                else return false
            else return false
        else if (event.action == MotionEvent.ACTION_UP) return false
        else super.onTouchEvent(event)

        return true
    }
}
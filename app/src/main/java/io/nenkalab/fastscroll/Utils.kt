@file:Suppress("KDocMissingDocumentation")

package io.nenkalab.fastscroll

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

fun <E : TextView> E.getScrollRange(): Int {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    measuredHeight
    return Math.max(
        0,
        measuredHeight - (height - paddingBottom - paddingTop)
    )
}

fun <E : FrameLayout> E.getScrollRange(): Int {
    var scrollRange = 0
    if (childCount > 0) {
        val child = getChildAt(0)
        scrollRange = Math.max(
            0,
            child.height - (height - paddingBottom - paddingTop)
        )
    }
    return scrollRange
}

fun View?.has(): Boolean {
    return this != null
}
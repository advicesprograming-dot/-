package com.example.ui.adhan

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class FullScreenVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    var fillScreen: Boolean = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (fillScreen) {
            val width = getDefaultSize(0, widthMeasureSpec)
            val height = getDefaultSize(0, heightMeasureSpec)
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}

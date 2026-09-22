package com.therian.oc.aaa.core.custom.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Join
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.therian.oc.aaa.R
import ir.kotlin.kavehcolorpicker.dp

class OuterStrokeTextView : AppCompatTextView {

    private var outerStrokeWidth = 0f
    private var outerStrokeColor: Int = Color.WHITE
    private var outerStrokeJoin: Join = Join.ROUND
    private var strokeMiter = 5f
    private var extraPadding = 0

    private var savedShadowRadius = 0f
    private var savedShadowDx = 0f
    private var savedShadowDy = 0f
    private var savedShadowColor = 0

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(attrs, R.styleable.OuterStrokeTextView)

        try {
            outerStrokeWidth = a.getDimension(
                R.styleable.OuterStrokeTextView_outerStrokeWidth, 0f
            )
            outerStrokeColor = a.getColor(
                R.styleable.OuterStrokeTextView_outerStrokeColor, Color.WHITE
            )
            outerStrokeJoin = when (a.getInt(
                R.styleable.OuterStrokeTextView_outerStrokeJoinStyle, 2)) {
                0 -> Join.MITER
                1 -> Join.BEVEL
                2 -> Join.ROUND
                else -> Join.ROUND
            }
        } finally {
            a.recycle()
        }

        if (outerStrokeWidth > 0f) {
            extraPadding = (outerStrokeWidth * dp(5)).toInt()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Lưu shadow từ XML trước khi clear
        savedShadowRadius = shadowRadius
        savedShadowDx = shadowDx
        savedShadowDy = shadowDy
        savedShadowColor = shadowColor

        // Clear shadow mặc định của TextView (sẽ tự quản lý trong onDraw)
        paint.clearShadowLayer()

        if (extraPadding > 0) {
            setPadding(
                paddingLeft + extraPadding,
                paddingTop + extraPadding,
                paddingRight + extraPadding,
                paddingBottom + extraPadding
            )
            extraPadding = 0
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (outerStrokeWidth > 0f) {
            val textColor = currentTextColor
            val paint = paint

            paint.isAntiAlias = true
            paint.isSubpixelText = true
            paint.strokeJoin = outerStrokeJoin
            paint.strokeMiter = strokeMiter
            paint.strokeCap = Paint.Cap.BUTT

            // Lớp 1: Stroke ngoài (màu tối) + shadow
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outerStrokeWidth * dp(1.2)
            setTextColor(outerStrokeColor)
            if (savedShadowColor != 0) {
                paint.setShadowLayer(savedShadowRadius, savedShadowDx, savedShadowDy, savedShadowColor)
            }
            super.onDraw(canvas)

            // Lớp 2: Fill chữ chính (không cần stroke trắng)
            paint.clearShadowLayer()
            paint.style = Paint.Style.FILL
            setTextColor(textColor)
            super.onDraw(canvas)

        } else {
            super.onDraw(canvas)
        }
    }
    fun setupSelectedTab() {
        outerStrokeWidth = resources.getDimension(R.dimen.dp_0_2)
        outerStrokeColor = ContextCompat.getColor(context, R.color.white)
        invalidate()
    }

    fun setupUnselectedTab() {
        outerStrokeWidth = resources.getDimension(R.dimen.dp_0_5)
        outerStrokeColor = ContextCompat.getColor(context, R.color.app)
        invalidate()
    }

}

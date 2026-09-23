package com.therian.oc.aaa.core.custom.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.therian.oc.aaa.R

class InstagramGradientLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(56, 48, 107)
    }
    private val bounds = RectF()

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width == 0 || height == 0) return

        fillPaint.shader = LinearGradient(
            0f,
            height * 0.34f,
            width * 0.90f,
            height * 1.82f,
            intArrayOf(
                Color.rgb(91, 81, 216),
                Color.rgb(225, 48, 108),
                Color.rgb(252, 175, 69)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val strokeWidth = resources.getDimension(R.dimen.dp_2)
        val halfStroke = strokeWidth / 2f
        val cornerRadius = resources.getDimension(R.dimen.dp_12)

        bounds.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, fillPaint)

        strokePaint.strokeWidth = strokeWidth
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, strokePaint)
        super.onDraw(canvas)
    }
}

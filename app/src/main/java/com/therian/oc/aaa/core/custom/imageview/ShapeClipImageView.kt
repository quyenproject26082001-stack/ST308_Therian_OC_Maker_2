package com.therian.oc.aaa.core.custom.imageview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView




class ShapeClipImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()
    private var maskBitmap: Bitmap? = null
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    // Set drawable làm mask
    var maskDrawable: Drawable? = null
        set(value) {
            field = value
            maskBitmap = null // reset cache
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maskBitmap = null // reset khi size thay đổi
    }

    private fun getMaskBitmap(w: Int, h: Int): Bitmap? {
        if (w <= 0 || h <= 0) return null
        if (maskBitmap != null) return maskBitmap

        val drawable = maskDrawable ?: return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        maskBitmap = bmp
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        val mask = getMaskBitmap(w, h)
        if (mask == null) {
            // Không có mask → vẽ bình thường
            super.onDraw(canvas)
            return
        }

        // Vẽ vào layer riêng để dùng PorterDuff
        val count = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)

        // Vẽ ảnh gốc (Glide load vào)
        super.onDraw(canvas)

        // Dùng DST_IN → giữ phần ảnh nằm trong vùng mask có alpha
        canvas.drawBitmap(mask, 0f, 0f, maskPaint)

        canvas.restoreToCount(count)
    }
}
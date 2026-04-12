package com.example.spaceadvisor.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.example.spaceadvisor.R

enum class GradientDirection {
    VERTICAL, HORIZONTAL, DIAGONAL
}

fun ImageView.applyGradientTint(
    startColor: Int = context.getColorFromAttr(R.attr.customColorPrimary),
    endColor: Int = context.getColorFromAttr(R.attr.customColorPrimaryVariant),
    direction: GradientDirection = GradientDirection.VERTICAL
) {
    val original = drawable ?: return

    val gradientWrapper = object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return

            val (x1, y1) = when (direction) {
                GradientDirection.VERTICAL -> 0f to b.height().toFloat()
                GradientDirection.HORIZONTAL -> b.width().toFloat() to 0f
                GradientDirection.DIAGONAL -> b.width().toFloat() to b.height().toFloat()
            }

            paint.shader = LinearGradient(
                b.left.toFloat(), b.top.toFloat(),
                b.left + x1, b.top + y1,
                startColor, endColor, Shader.TileMode.CLAMP
            )

            val count = canvas.saveLayer(
                b.left.toFloat(), b.top.toFloat(),
                b.right.toFloat(), b.bottom.toFloat(), null
            )

            original.bounds = b
            original.draw(canvas)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawRect(b, paint)
            paint.xfermode = null

            canvas.restoreToCount(count)
        }

        override fun setAlpha(alpha: Int) {
            original.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            original.colorFilter = colorFilter
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = original.intrinsicWidth
        override fun getIntrinsicHeight(): Int = original.intrinsicHeight
    }

    setImageDrawable(gradientWrapper)
}


fun TextView.applyGradientText(
    startColor: Int = context.getColorFromAttr(R.attr.customColorPrimary),
    endColor: Int = context.getColorFromAttr(R.attr.customColorPrimaryVariant),
    direction: GradientDirection = GradientDirection.HORIZONTAL
) {
    post {
        val width = paint.measureText(text.toString())
        val height = textSize * lineCount

        val (x1, y1) = when (direction) {
            GradientDirection.VERTICAL -> 0f to height
            GradientDirection.HORIZONTAL -> width to 0f
            GradientDirection.DIAGONAL -> width to height
        }

        val shader = LinearGradient(
            0f, 0f, x1, y1,
            intArrayOf(startColor, endColor),
            null,
            Shader.TileMode.CLAMP
        )
        this.paint.shader = shader
        invalidate()
    }
}

@ColorInt
fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int {
    val typedValue = TypedValue()
    if (theme.resolveAttribute(attrColor, typedValue, true)) {
        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(this, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }
    theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
    return typedValue.data

}


package com.example.spaceadvisor.ui.fragments

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.spaceadvisor.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.activities.AuthActivity
import com.example.spaceadvisor.ui.activities.BaseActivity
import com.example.spaceadvisor.ui.activities.LoadingType
import com.example.spaceadvisor.ui.viewmodels.UIViewModel

enum class GradientDirection {
    VERTICAL, HORIZONTAL, DIAGONAL
}

abstract class BaseFragment : Fragment() {

    protected val uiViewModel: UIViewModel by activityViewModels()

    abstract fun getUIConfig(): UIConfig

    override fun onStart() {
        super.onStart()
        if (!isHidden) {
            uiViewModel.updateUI(getUIConfig())
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            uiViewModel.updateUI(getUIConfig())
        }
    }

    fun navigateTo(fragment: BaseFragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom_to_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top_to_bottom
            )
            .add(R.id.main_frame, fragment)
            .hide(this)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToAuth() {
        val intent = Intent(requireActivity(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    fun showLoading(
        message: String? = null,
        type: LoadingType = LoadingType.PROGRESS_BAR
    ) {
        (activity as? BaseActivity)?.showLoading(message, type)
    }

    fun hideLoading() {
        (activity as? BaseActivity)?.hideLoading()
    }

    fun showError(message: String) {
        showCustomMessage("Error", message)
    }

    fun showCustomMessage(title: String, body: String, duration: Long = 3000) {
        (activity as? BaseActivity)?.showCustomMessage(title, body, duration)
    }

    fun ImageView.applyGradientTint(
        startColor: Int = ContextCompat.getColor(context, R.color.space_blue),
        endColor: Int = ContextCompat.getColor(context, R.color.primary_variant),
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
        startColor: Int = ContextCompat.getColor(context, R.color.space_blue),
        endColor: Int = ContextCompat.getColor(context, R.color.primary_variant),
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
}
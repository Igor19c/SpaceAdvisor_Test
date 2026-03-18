package com.example.spaceadvisor.ui.activities

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.spaceadvisor.R

enum class LoadingType {
    PROGRESS_BAR, LOTTIE
}

abstract class BaseActivity : AppCompatActivity() {

    private var loadingOverlay: View? = null

    protected fun hideSystemBars(view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, view)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun showLoading(message: String? = null, type: LoadingType = LoadingType.PROGRESS_BAR) {
        if (loadingOverlay != null) return

        val root = findViewById<ViewGroup>(android.R.id.content)
        val layoutRes = when (type) {
            LoadingType.PROGRESS_BAR -> R.layout.layout_loading_progress_bar_overlay
            LoadingType.LOTTIE -> R.layout.layout_loading_lottie_overlay
        }
        
        loadingOverlay = LayoutInflater.from(this).inflate(layoutRes, root, false)
        
        message?.let {
            loadingOverlay?.findViewById<TextView>(R.id.loading_message)?.text = it
        }

        root.addView(loadingOverlay)
    }

    fun hideLoading() {
        loadingOverlay?.let {
            val root = findViewById<ViewGroup>(android.R.id.content)
            root.removeView(it)
            loadingOverlay = null
        }
    }

    fun showCustomMessage(title: String, body: String, duration: Long = 3000) {
        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        val messageView =
            LayoutInflater.from(this).inflate(R.layout.layout_custom_message, root, false)

        messageView.findViewById<TextView>(R.id.message_title).text = title
        messageView.findViewById<TextView>(R.id.message_body).text = body
        messageView.findViewById<View>(R.id.close_message).setOnClickListener {
            root.removeView(messageView)
        }

        root.addView(messageView)
        messageView.alpha = 0f
        messageView.translationY = -100f
        messageView.animate().alpha(1f).translationY(0f).setDuration(400).start()

        Handler(Looper.getMainLooper()).postDelayed({
            if (messageView.parent != null) {
                messageView.animate().alpha(0f).translationY(-100f).setDuration(400)
                    .withEndAction { root.removeView(messageView) }.start()
            }
        }, duration)
    }
}
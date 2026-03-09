package com.example.spaceadvisor.ui.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.spaceadvisor.R

abstract class BaseActivity : AppCompatActivity() {

    private var loadingOverlay: View? = null

    protected fun hideSystemBars(view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, view)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun showLoading(message: String? = null) {
        if (loadingOverlay != null) return

        val root = findViewById<ViewGroup>(android.R.id.content)
        loadingOverlay = LayoutInflater.from(this).inflate(R.layout.layout_loading_overlay, root, false)
        
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
}
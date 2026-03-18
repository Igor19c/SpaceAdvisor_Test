package com.example.spaceadvisor.ui.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.spaceadvisor.R

fun Activity.showCustomMessage(title: String, body: String, duration: Long = 3000) {
    val root = findViewById<ViewGroup>(android.R.id.content) ?: return
    showCustomMessageInternal(root, title, body, duration)
}

fun Fragment.showCustomMessage(title: String, body: String, duration: Long = 3000) {
    activity?.showCustomMessage(title, body, duration)
}

private fun showCustomMessageInternal(root: ViewGroup, title: String, body: String, duration: Long) {
    val context = root.context
    val messageView = LayoutInflater.from(context).inflate(R.layout.layout_custom_message, root, false)

    messageView.findViewById<TextView>(R.id.message_title).text = title
    messageView.findViewById<TextView>(R.id.message_body).text = body

    messageView.findViewById<View>(R.id.close_message).setOnClickListener {
        root.removeView(messageView)
    }

    root.addView(messageView)

    messageView.alpha = 0f
    messageView.translationY = -100f
    messageView.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(400)
        .start()

    Handler(Looper.getMainLooper()).postDelayed({
        if (messageView.parent != null) {
            messageView.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(400)
                .withEndAction { root.removeView(messageView) }
                .start()
        }
    }, duration)
}
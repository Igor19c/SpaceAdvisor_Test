package com.example.spaceadvisor.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.spaceadvisor.R

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ACCENT_THEME = "accent_theme"
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            applyDarkMode(value)
        }

    var selectedThemeResId: Int
        get() = prefs.getInt(KEY_ACCENT_THEME, R.style.Theme_SpaceAdvisor)
        set(value) {
            prefs.edit().putInt(KEY_ACCENT_THEME, value).apply()
        }

    fun applyDarkMode(isDark: Boolean) {
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applyTheme(context: Context) {
        context.setTheme(selectedThemeResId)
    }
}
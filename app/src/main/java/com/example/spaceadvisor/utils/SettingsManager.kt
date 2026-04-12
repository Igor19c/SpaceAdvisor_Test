package com.example.spaceadvisor.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.spaceadvisor.R

class SettingsManager(private val context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
    private var currentUid: String? = null

    companion object {
        private const val GLOBAL_PREFS = "global_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ACCENT_THEME = "accent_theme"
        private const val KEY_NEEDS_REAUTH = "needs_reauth"
    }

    fun switchUser(uid: String?) {
        currentUid = uid
        val prefsName = if (uid != null) "settings_$uid" else GLOBAL_PREFS
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        applyDarkMode(isDarkMode)
    }

    fun resetToDefaults() {
        switchUser(null)
        prefs.edit().clear().apply()
        isDarkMode = true
        selectedThemeResId = R.style.Theme_SpaceAdvisor
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            applyDarkMode(value)
        }

    var selectedThemeResId: Int
        get() = prefs.getInt(KEY_ACCENT_THEME, R.style.Theme_SpaceAdvisor)
        set(value) {
            prefs.edit().putInt(KEY_ACCENT_THEME, value).apply()
        }

    var needsReAuthAfterPasswordReset: Boolean
        get() = prefs.getBoolean(KEY_NEEDS_REAUTH, false)
        set(value) = prefs.edit().putBoolean(KEY_NEEDS_REAUTH, value).apply()

    fun applyDarkMode(isDark: Boolean) {
        val mode =
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applyTheme(activityContext: Context) {
        activityContext.setTheme(selectedThemeResId)
    }
}
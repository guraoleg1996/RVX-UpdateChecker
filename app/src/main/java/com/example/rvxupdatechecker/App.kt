package com.example.rvxupdatechecker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("RVX_PREFS", MODE_PRIVATE)
        val themeMode = prefs.getInt("pref_theme_mode", 0) // 0 = system, 1 = light, 2 = dark

        when (themeMode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Если вы сохраняете локаль в prefs (в SettingsActivity вы вызываете setApplicationLocales),
        // примените её здесь тоже (пример ниже — если вы храните "pref_lang" как "ru" или "en")
        val langTag = prefs.getString("pref_lang_tag", null)
        if (!langTag.isNullOrBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langTag))
        }
    }
}

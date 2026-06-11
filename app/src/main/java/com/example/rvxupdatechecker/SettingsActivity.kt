package com.example.rvxupdatechecker

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class SettingsActivity : AppCompatActivity() {

    private val siteNamesDefault = mapOf(
        "anddea.youtube" to "ReVanced Extended",
        "app.morphe.android.apps.youtube.music" to "ReVanced Music",
        "app.revanced.android.gms" to "GMSCore"
    )

    private val THEME_SYSTEM = 0
    private val THEME_LIGHT = 1
    private val THEME_DARK = 2

    private lateinit var bottomBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("RVX_PREFS", Context.MODE_PRIVATE)

        val etMain = findViewById<EditText>(R.id.et_main)
        val etMusic = findViewById<EditText>(R.id.et_music)
        val etGms = findViewById<EditText>(R.id.et_gms)

        val tvMainSiteLabel = findViewById<TextView>(R.id.tv_label_main_site)
        val tvMusicSiteLabel = findViewById<TextView>(R.id.tv_label_music_site)
        val tvGmsSiteLabel = findViewById<TextView>(R.id.tv_label_gms_site)

        val spinnerLang = findViewById<Spinner>(R.id.spinner_lang)
        val spinnerTheme = findViewById<Spinner>(R.id.spinner_theme)
        val switchAmoled = findViewById<Switch>(R.id.switch_amoled)

        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnClose = findViewById<Button>(R.id.btn_close_settings)

        bottomBar = findViewById(R.id.bottom_bar)

        val pkgMain = prefs.getString("pkg_main", "anddea.youtube") ?: "anddea.youtube"
        val pkgMusic = prefs.getString("pkg_music", "app.morphe.android.apps.youtube.music") ?: "app.morphe.android.apps.youtube.music"
        val pkgGms = prefs.getString("pkg_gms", "app.revanced.android.gms") ?: "app.revanced.android.gms"

        etMain.setText(pkgMain)
        etMusic.setText(pkgMusic)
        etGms.setText(pkgGms)

        val savedSiteNameMain = prefs.getString("site_name_$pkgMain", siteNamesDefault[pkgMain])
        val savedSiteNameMusic = prefs.getString("site_name_$pkgMusic", siteNamesDefault[pkgMusic])
        val savedSiteNameGms = prefs.getString("site_name_$pkgGms", siteNamesDefault[pkgGms])

        val siteNameMainStatic = savedSiteNameMain ?: siteNamesDefault[pkgMain] ?: pkgMain
        val siteNameMusicStatic = savedSiteNameMusic ?: siteNamesDefault[pkgMusic] ?: pkgMusic
        val siteNameGmsStatic = savedSiteNameGms ?: siteNamesDefault[pkgGms] ?: pkgGms

        val mainAppName = getAppName(pkgMain)
        val musicAppName = getAppName(pkgMusic)
        val gmsAppName = getAppName(pkgGms)

        tvMainSiteLabel.text = if (mainAppName != getString(R.string.not_installed_label)) "$siteNameMainStatic — $mainAppName" else siteNameMainStatic
        tvMusicSiteLabel.text = if (musicAppName != getString(R.string.not_installed_label)) "$siteNameMusicStatic — $musicAppName" else siteNameMusicStatic
        tvGmsSiteLabel.text = if (gmsAppName != getString(R.string.not_installed_label)) "$siteNameGmsStatic — $gmsAppName" else siteNameGmsStatic

        // Language spinner
        val langs = arrayOf("Русский", "English")
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langs)

        val savedLangTag = prefs.getString("pref_lang_tag", null)
        if (!savedLangTag.isNullOrBlank()) {
            spinnerLang.setSelection(if (savedLangTag.startsWith("ru")) 0 else 1)
        } else {
            val currentLang = resources.configuration.locales.get(0).language
            spinnerLang.setSelection(if (currentLang == "ru") 0 else 1)
        }

        // Theme spinner
        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        spinnerTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        val savedTheme = prefs.getInt("pref_theme_mode", THEME_SYSTEM)
        spinnerTheme.setSelection(savedTheme)

        val savedAmoled = prefs.getBoolean("pref_amoled", false)
        switchAmoled.isChecked = savedAmoled

        // Обработка WindowInsets для bottomBar (чтобы кнопка Save не была под навигацией)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll_content)) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = sysBars.bottom
            bottomBar.updatePadding(bottom = bottomInset + dpToPx(8))
            insets
        }

        btnSave.setOnClickListener {
            val newPkgMain = etMain.text.toString().trim().ifEmpty { "anddea.youtube" }
            val newPkgMusic = etMusic.text.toString().trim().ifEmpty { "app.morphe.android.apps.youtube.music" }
            val newPkgGms = etGms.text.toString().trim().ifEmpty { "app.revanced.android.gms" }

            val selectedTag = if (spinnerLang.selectedItemPosition == 0) "ru" else "en"
            val selectedThemeMode = spinnerTheme.selectedItemPosition

            prefs.edit().apply {
                putString("pkg_main", newPkgMain)
                putString("pkg_music", newPkgMusic)
                putString("pkg_gms", newPkgGms)
                putInt("pref_theme_mode", selectedThemeMode)
                putBoolean("pref_amoled", switchAmoled.isChecked)
                putString("pref_lang_tag", selectedTag)
                apply()
            }

            when (selectedThemeMode) {
                THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedTag))

            val updatedMainAppName = getAppName(newPkgMain)
            val updatedMusicAppName = getAppName(newPkgMusic)
            val updatedGmsAppName = getAppName(newPkgGms)

            val updatedSiteNameMainStatic = prefs.getString("site_name_$newPkgMain", siteNamesDefault[newPkgMain]) ?: siteNamesDefault[newPkgMain] ?: newPkgMain
            val updatedSiteNameMusicStatic = prefs.getString("site_name_$newPkgMusic", siteNamesDefault[newPkgMusic]) ?: siteNamesDefault[newPkgMusic] ?: newPkgMusic
            val updatedSiteNameGmsStatic = prefs.getString("site_name_$newPkgGms", siteNamesDefault[newPkgGms]) ?: siteNamesDefault[newPkgGms] ?: newPkgGms

            tvMainSiteLabel.text = if (updatedMainAppName != getString(R.string.not_installed_label)) "$updatedSiteNameMainStatic — $updatedMainAppName" else updatedSiteNameMainStatic
            tvMusicSiteLabel.text = if (updatedMusicAppName != getString(R.string.not_installed_label)) "$updatedSiteNameMusicStatic — $updatedMusicAppName" else updatedSiteNameMusicStatic
            tvGmsSiteLabel.text = if (updatedGmsAppName != getString(R.string.not_installed_label)) "$updatedSiteNameGmsStatic — $updatedGmsAppName" else updatedSiteNameGmsStatic

            applyAmoledIfNeeded()

            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()

            // Пересоздаём Activity, чтобы изменения темы/локали применились немедленно
            recreate()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        applyAmoledIfNeeded()
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            getString(R.string.not_installed_label)
        }
    }

    private fun applyAmoledIfNeeded() {
        val prefs = getSharedPreferences("RVX_PREFS", Context.MODE_PRIVATE)
        val useAmoled = prefs.getBoolean("pref_amoled", false)

        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = nightMode == Configuration.UI_MODE_NIGHT_YES

        if (isNight && useAmoled) {
            window.decorView.setBackgroundColor(Color.BLACK)
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
        } else {
            val typedArray = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
            val bg = typedArray.getDrawable(0)
            typedArray.recycle()
            window.decorView.background = bg
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

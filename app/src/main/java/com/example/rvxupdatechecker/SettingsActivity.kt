package com.example.rvxupdatechecker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    private val NOTIF_CHANNEL_ID = "rvx_update_channel"
    private val NOTIF_ID = 1001

    private var checkingDialog: AlertDialog? = null

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
        val btnCheckUpdate = findViewById<Button>(R.id.btn_check_update)

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

        val langs = arrayOf("Русский", "English")
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langs)

        val savedLangTag = prefs.getString("pref_lang_tag", null)
        if (!savedLangTag.isNullOrBlank()) {
            spinnerLang.setSelection(if (savedLangTag.startsWith("ru")) 0 else 1)
        } else {
            val currentLang = resources.configuration.locales.get(0).language
            spinnerLang.setSelection(if (currentLang == "ru") 0 else 1)
        }

        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        spinnerTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        val savedTheme = prefs.getInt("pref_theme_mode", THEME_SYSTEM)
        spinnerTheme.setSelection(savedTheme)

        val savedAmoled = prefs.getBoolean("pref_amoled", false)
        switchAmoled.isChecked = savedAmoled

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

            recreate()
        }

        btnClose.setOnClickListener {
            finish()
        }

        btnCheckUpdate.setOnClickListener {
            // Показываем модальный прогресс и уведомление, затем запускаем проверку
            showProgressDialog()
            ensureNotificationChannel()
            showCheckingNotification()

            UpdateChecker.checkForUpdate(
                context = this,
                onStart = { /* индикатор уже показан */ },
                onResult = { result ->
                    // Закрываем прогресс-диалог
                    dismissProgressDialog()

                    when (result) {
                        is UpdateResult.UpToDate -> {
                            updateNotificationResult(getString(R.string.update_notification_title), getString(R.string.update_up_to_date))
                            Toast.makeText(this, getString(R.string.update_up_to_date), Toast.LENGTH_SHORT).show()
                        }
                        is UpdateResult.UpdateAvailable -> {
                            val tag = result.tag
                            val apkUrl = result.apkUrl
                            val prereleaseNote = if (result.isPrerelease) " (pre-release)" else ""
                            updateNotificationResult(getString(R.string.update_notification_title), getString(R.string.update_available_message, "$tag$prereleaseNote"))
                            AlertDialog.Builder(this)
                                .setTitle(getString(R.string.update_available_title))
                                .setMessage(getString(R.string.update_available_message, "$tag$prereleaseNote"))
                                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                    if (!apkUrl.isNullOrBlank()) {
                                        UpdateChecker.openUrl(this, apkUrl)
                                    } else {
                                        UpdateChecker.openReleasesPage(this)
                                    }
                                }
                                .setNegativeButton(getString(R.string.no), null)
                                .show()
                        }
                        is UpdateResult.Error -> {
                            updateNotificationResult(getString(R.string.update_notification_title), result.message)
                            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                suppressUpToDate = false
            )
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.update_notification_channel_name)
            val desc = getString(R.string.update_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(NOTIF_CHANNEL_ID, name, importance).apply {
                description = desc
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showCheckingNotification() {
        val builder = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.update_notification_title))
            .setContentText(getString(R.string.update_checking_message))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(this).notify(NOTIF_ID, builder.build())
    }

    private fun updateNotificationResult(title: String, text: String, ongoing: Boolean = false) {
        val builder = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(this).notify(NOTIF_ID, builder.build())
    }

    private fun showProgressDialog() {
        if (checkingDialog?.isShowing == true) return

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            val pad = dpToPx(16)
            setPadding(pad, pad, pad, pad)
        }

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        container.addView(progressBar, params)

        builder.setView(container)
        builder.setMessage(getString(R.string.update_checking_message))
        checkingDialog = builder.create()
        checkingDialog?.show()
    }

    private fun dismissProgressDialog() {
        try {
            checkingDialog?.dismiss()
        } catch (e: Exception) {
            // ignore
        } finally {
            checkingDialog = null
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

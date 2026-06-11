package com.example.rvxupdatechecker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val siteNamesStatic = mapOf(
        "anddea.youtube" to "ReVanced Extended",
        "app.morphe.android.apps.youtube.music" to "ReVanced Music",
        "app.revanced.android.gms" to "GMSCore"
    )

    private val siteUrls = mapOf(
        "anddea.youtube" to "https://rvx.to/ru/",
        "app.morphe.android.apps.youtube.music" to "https://rvx.to/ru/music/",
        "app.revanced.android.gms" to "https://rvx.to/ru/gmscore/"
    )

    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefresh = findViewById(R.id.swipe_refresh)

        findViewById<Button>(R.id.btn_go_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_self_update).setOnClickListener {
            // На главном экране эта кнопка проверяет актуальность установленных приложений
            swipeRefresh.isRefreshing = true
            refreshUiData(checkSelfApp = false) // не проверяем само приложение здесь
        }

        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finishAffinity()
        }

        swipeRefresh.setOnRefreshListener {
            refreshUiData(checkSelfApp = false)
        }

        // стартовая загрузка (используем swipeRefresh как индикатор)
        swipeRefresh.isRefreshing = true
        refreshUiData(checkSelfApp = false)
    }

    override fun onResume() {
        super.onResume()
        applyAmoledIfNeeded()
    }

    /**
     * checkSelfApp: если true — дополнительно проверяем обновление самого RVX UpdateChecker (через UpdateChecker)
     * если false — только проверяем версии установленных приложений (основная логика UI)
     */
    private fun refreshUiData(checkSelfApp: Boolean) {
        val prefs = getSharedPreferences("RVX_PREFS", Context.MODE_PRIVATE)

        val pkgMain = prefs.getString("pkg_main", "anddea.youtube") ?: "anddea.youtube"
        val pkgMusic = prefs.getString("pkg_music", "app.morphe.android.apps.youtube.music") ?: "app.morphe.android.apps.youtube.music"
        val pkgGms = prefs.getString("pkg_gms", "app.revanced.android.gms") ?: "app.revanced.android.gms"

        val tvMainSiteTitle = findViewById<TextView>(R.id.tv_main_site_title)
        val tvMusicSiteTitle = findViewById<TextView>(R.id.tv_music_site_title)
        val tvGmsSiteTitle = findViewById<TextView>(R.id.tv_gms_site_title)

        val tvMainStatus = findViewById<TextView>(R.id.tv_main_status)
        val tvMusicStatus = findViewById<TextView>(R.id.tv_music_status)
        val tvGmsStatus = findViewById<TextView>(R.id.tv_gms_status)

        val tvMainSiteVersion = findViewById<TextView>(R.id.tv_main_site_version)
        val tvMusicSiteVersion = findViewById<TextView>(R.id.tv_music_site_version)
        val tvGmsSiteVersion = findViewById<TextView>(R.id.tv_gms_site_version)

        val btnMainAction = findViewById<Button>(R.id.btn_main_action)
        val btnMusicAction = findViewById<Button>(R.id.btn_music_action)
        val btnGmsAction = findViewById<Button>(R.id.btn_gms_action)

        val siteNameMainStatic = siteNamesStatic[pkgMain] ?: pkgMain
        val siteNameMusicStatic = siteNamesStatic[pkgMusic] ?: pkgMusic
        val siteNameGmsStatic = siteNamesStatic[pkgGms] ?: pkgGms

        val mainAppName = getAppName(pkgMain)
        val musicAppName = getAppName(pkgMusic)
        val gmsAppName = getAppName(pkgGms)

        val mainVersionInstalled = getAppVersion(pkgMain)
        val musicVersionInstalled = getAppVersion(pkgMusic)
        val gmsVersionInstalled = getAppVersion(pkgGms)

        tvMainSiteTitle.text = if (mainAppName != getString(R.string.not_installed_label)) "$siteNameMainStatic — $mainAppName" else siteNameMainStatic
        tvMusicSiteTitle.text = if (musicAppName != getString(R.string.not_installed_label)) "$siteNameMusicStatic — $musicAppName" else siteNameMusicStatic
        tvGmsSiteTitle.text = if (gmsAppName != getString(R.string.not_installed_label)) "$siteNameGmsStatic — $gmsAppName" else siteNameGmsStatic

        tvMainStatus.text = if (mainAppName == getString(R.string.not_installed_label)) getString(R.string.status_not_found) else getString(R.string.installed_version, mainVersionInstalled)
        tvMusicStatus.text = if (musicAppName == getString(R.string.not_installed_label)) getString(R.string.status_not_found) else getString(R.string.installed_version, musicVersionInstalled)
        tvGmsStatus.text = if (gmsAppName == getString(R.string.not_installed_label)) getString(R.string.status_not_found) else getString(R.string.installed_version, gmsVersionInstalled)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val siteVerMain = withContext(Dispatchers.IO) { fetchVersionFromSiteJsoup(siteUrls[pkgMain]) }
                val siteVerMusic = withContext(Dispatchers.IO) { fetchVersionFromSiteJsoup(siteUrls[pkgMusic]) }
                val siteVerGms = withContext(Dispatchers.IO) { fetchVersionForGmscore(siteUrls[pkgGms]) }

                tvMainSiteVersion.text = getString(R.string.site_version, siteVerMain ?: "—")
                tvMusicSiteVersion.text = getString(R.string.site_version, siteVerMusic ?: "—")
                tvGmsSiteVersion.text = getString(R.string.site_version, siteVerGms ?: "—")

                configureActionButton(btnMainAction, pkgMain, mainAppName, mainVersionInstalled, siteVerMain, siteUrls[pkgMain])
                configureActionButton(btnMusicAction, pkgMusic, musicAppName, musicVersionInstalled, siteVerMusic, siteUrls[pkgMusic])
                configureActionButton(btnGmsAction, pkgGms, gmsAppName, gmsVersionInstalled, siteVerGms, siteUrls[pkgGms])

                // Если требуется — дополнительно проверяем само приложение (Settings -> manual check)
                if (checkSelfApp) {
                    UpdateChecker.checkForUpdate(
                        this@MainActivity,
                        onStart = { Log.d(TAG, "Update check started (main manual)") },
                        onResult = { result ->
                            swipeRefresh.isRefreshing = false
                            when (result) {
                                is UpdateResult.UpToDate -> {
                                    // На главном экране при ручной проверке из настроек мы не ожидаем этого пути,
                                    // но если checkSelfApp==true и UpToDate — можно показать тост
                                    Toast.makeText(this@MainActivity, getString(R.string.update_up_to_date), Toast.LENGTH_SHORT).show()
                                }
                                is UpdateResult.UpdateAvailable -> {
                                    showInstallDialog(result.tag, result.apkUrl, result.isPrerelease)
                                }
                                is UpdateResult.Error -> {
                                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        suppressUpToDate = false
                    )
                } else {
                    // На главном экране: проверяем обновление самого приложения, но подавляем "актуально" тост
                    UpdateChecker.checkForUpdate(
                        this@MainActivity,
                        onStart = { Log.d(TAG, "Update check started (main background)") },
                        onResult = { result ->
                            swipeRefresh.isRefreshing = false
                            when (result) {
                                is UpdateResult.UpToDate -> {
                                    Log.d(TAG, "App is up to date (suppressed on main)")
                                }
                                is UpdateResult.UpdateAvailable -> {
                                    showInstallDialog(result.tag, result.apkUrl, result.isPrerelease)
                                }
                                is UpdateResult.Error -> {
                                    Log.w(TAG, "Update check error on main: ${result.message}")
                                }
                            }
                        },
                        suppressUpToDate = true
                    )
                }
            } catch (e: Exception) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, getString(R.string.status_error_fetch), Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error refreshing UI data", e)
            }
        }
    }

    private fun showInstallDialog(tag: String, apkUrl: String?, isPrerelease: Boolean) {
        val prereleaseNote = if (isPrerelease) " (pre-release)" else ""
        val message = getString(R.string.update_available_message, "$tag$prereleaseNote")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_available_title))
            .setMessage(message)
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

    private fun configureActionButton(btn: Button, pkgName: String, installedName: String, installedVersion: String, siteVersion: String?, siteUrl: String?) {
        when {
            installedName == getString(R.string.not_installed_label) -> {
                btn.text = getString(R.string.action_install)
                btn.setOnClickListener {
                    if (!siteUrl.isNullOrBlank()) openUrl(siteUrl) else Toast.makeText(this, getString(R.string.status_error_no_url), Toast.LENGTH_SHORT).show()
                }
            }
            siteVersion.isNullOrBlank() -> {
                btn.text = getString(R.string.action_check)
                btn.setOnClickListener {
                    if (!siteUrl.isNullOrBlank()) openUrl(siteUrl) else Toast.makeText(this, getString(R.string.status_error_no_url), Toast.LENGTH_SHORT).show()
                }
            }
            installedVersion == getString(R.string.version_not_found) || installedVersion == getString(R.string.version_no_data) -> {
                btn.text = getString(R.string.action_check)
                btn.setOnClickListener {
                    if (!siteUrl.isNullOrBlank()) openUrl(siteUrl) else Toast.makeText(this, getString(R.string.status_error_no_url), Toast.LENGTH_SHORT).show()
                }
            }
            compareVersions(installedVersion, siteVersion) < 0 -> {
                btn.text = getString(R.string.action_update)
                btn.setOnClickListener {
                    if (!siteUrl.isNullOrBlank()) openUrl(siteUrl) else Toast.makeText(this, getString(R.string.status_error_no_url), Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                btn.text = getString(R.string.action_open)
                btn.setOnClickListener {
                    val launch = packageManager.getLaunchIntentForPackage(pkgName)
                    if (launch != null) startActivity(launch) else if (!siteUrl.isNullOrBlank()) openUrl(siteUrl) else Toast.makeText(this, getString(R.string.status_error_no_url), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.status_error_open), Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchVersionFromSiteJsoup(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val doc = Jsoup.connect(url).timeout(10_000).get()
            val verRegex = "(\\d+\\.[\\d.]+)".toRegex()
            val candidates = listOf(
                doc.selectFirst("h1")?.text(),
                doc.selectFirst("h2")?.text(),
                doc.selectFirst(".version")?.text(),
                doc.selectFirst(".post-meta")?.text(),
                doc.title()
            )
            for (c in candidates) {
                if (!c.isNullOrBlank()) {
                    val m = verRegex.find(c)
                    if (m != null) return m.value
                }
            }
            val mBody = verRegex.find(doc.body().text())
            mBody?.value
        } catch (e: Exception) {
            Log.w(TAG, "fetchVersionFromSiteJsoup failed for $url", e)
            null
        }
    }

    private fun fetchVersionForGmscore(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val doc = Jsoup.connect(url).timeout(10_000).get()
            val selectors = listOf(".download-version", ".version", ".release-version", ".post-meta", ".post-title .version", "h2", "h1")
            val verRegex = "(\\d+\\.[\\d.]+)".toRegex()
            for (sel in selectors) {
                val el = doc.selectFirst(sel)
                if (el != null) {
                    val txt = el.text().trim()
                    if (txt.isNotEmpty()) {
                        val m = verRegex.find(txt)
                        if (m != null) return m.value
                    }
                }
            }
            val linkCandidates = doc.select("a[href]").map { it.text() } + doc.select("a[href]").map { it.attr("href") }
            for (txt in linkCandidates) {
                val m = verRegex.find(txt)
                if (m != null) return m.value
            }
            val mBody = verRegex.find(doc.body().text())
            mBody?.value
        } catch (e: Exception) {
            Log.w(TAG, "fetchVersionForGmscore failed for $url", e)
            null
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        try {
            val a = v1.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toInt() }
            val b = v2.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toInt() }
            val maxLen = maxOf(a.size, b.size)
            for (i in 0 until maxLen) {
                val ai = if (i < a.size) a[i] else 0
                val bi = if (i < b.size) b[i] else 0
                if (ai < bi) return -1
                if (ai > bi) return 1
            }
            return 0
        } catch (e: Exception) {
            return v1.compareTo(v2)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            getString(R.string.not_installed_label)
        }
    }

    private fun getAppVersion(packageName: String): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: getString(R.string.version_no_data)
        } catch (e: PackageManager.NameNotFoundException) {
            getString(R.string.version_not_found)
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

    // Helper to convert dp to px
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

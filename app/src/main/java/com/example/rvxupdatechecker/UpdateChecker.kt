package com.example.rvxupdatechecker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class UpdateAvailable(val tag: String, val apkUrl: String?, val isPrerelease: Boolean) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val OWNER = "guraoleg1996"
    private const val REPO = "RVX-UpdateChecker"
    private const val RELEASES_URL = "https://api.github.com/repos/$OWNER/$REPO/releases"
    private const val TAGS_URL = "https://api.github.com/repos/$OWNER/$REPO/tags"
    private const val RELEASES_PAGE = "https://github.com/$OWNER/$REPO/releases"

    /**
     * Проверяет обновление репозитория (включая pre-release).
     * onResult возвращает UpdateResult в UI-потоке.
     *
     * suppressUpToDate — если true, вызывающий код может игнорировать UpToDate (не показывать тост).
     */
    fun checkForUpdate(
        context: Context,
        onStart: (() -> Unit)? = null,
        onResult: (UpdateResult) -> Unit,
        suppressUpToDate: Boolean = false
    ) {
        onStart?.invoke()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val client = OkHttpClient()
                val releases = fetchReleases(client)
                if (releases == null) {
                    val firstTag = fetchFirstTag(client)
                    if (firstTag.isNullOrBlank()) {
                        onResult(UpdateResult.Error(context.getString(R.string.update_no_releases)))
                        return@launch
                    } else {
                        handleComparisonWithTag(context, firstTag, null, false, onResult)
                        return@launch
                    }
                }

                val parsed = parseReleases(releases)
                if (parsed.isEmpty()) {
                    val firstTag = fetchFirstTag(client)
                    if (firstTag.isNullOrBlank()) {
                        onResult(UpdateResult.Error(context.getString(R.string.update_no_releases)))
                        return@launch
                    } else {
                        handleComparisonWithTag(context, firstTag, null, false, onResult)
                        return@launch
                    }
                }

                val newest = selectNewestRelease(parsed)
                if (newest == null) {
                    onResult(UpdateResult.Error(context.getString(R.string.update_check_error)))
                    return@launch
                }

                handleComparisonWithTag(context, newest.tag, newest.apkUrl, newest.isPrerelease, onResult)
            } catch (e: Exception) {
                Log.e(TAG, "checkForUpdate error", e)
                onResult(UpdateResult.Error(context.getString(R.string.update_check_error)))
            }
        }
    }

    private suspend fun fetchReleases(client: OkHttpClient): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(RELEASES_URL).build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string()
                Log.d(TAG, "releases response code=${resp.code}")
                Log.d(TAG, "releases body=${body?.take(1000)}")
                if (!resp.isSuccessful || body.isNullOrBlank()) return@withContext null
                JSONArray(body)
            } catch (e: Exception) {
                Log.w(TAG, "fetchReleases failed", e)
                null
            }
        }
    }

    private suspend fun fetchFirstTag(client: OkHttpClient): String? {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(TAGS_URL).build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string()
                Log.d(TAG, "tags response code=${resp.code}")
                Log.d(TAG, "tags body=${body?.take(1000)}")
                if (!resp.isSuccessful || body.isNullOrBlank()) return@withContext null
                val arr = JSONArray(body)
                if (arr.length() == 0) return@withContext null
                val first = arr.getJSONObject(0)
                first.optString("name", null)
            } catch (e: Exception) {
                Log.w(TAG, "fetchFirstTag failed", e)
                null
            }
        }
    }

    private data class ReleaseInfo(val tag: String, val apkUrl: String?, val isPrerelease: Boolean)

    private fun parseReleases(releases: JSONArray): List<ReleaseInfo> {
        val list = mutableListOf<ReleaseInfo>()
        for (i in 0 until releases.length()) {
            val r = releases.getJSONObject(i)
            val tag = r.optString("tag_name", "").removePrefix("v")
            if (tag.isBlank()) continue
            val isPrerelease = r.optBoolean("prerelease", false)
            var apkUrl: String? = null
            val assets = r.optJSONArray("assets")
            if (assets != null) {
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    val name = a.optString("name", "")
                    val url = a.optString("browser_download_url", "")
                    if (name.endsWith(".apk", true)) {
                        apkUrl = url
                        break
                    }
                    if (apkUrl == null && url.isNotBlank()) apkUrl = url
                }
            }
            list.add(ReleaseInfo(tag, apkUrl, isPrerelease))
        }
        return list
    }

    private fun selectNewestRelease(list: List<ReleaseInfo>): ReleaseInfo? {
        if (list.isEmpty()) return null
        return list.maxWithOrNull { a, b -> compareSemver(a.tag, b.tag) }
    }

    private fun compareSemver(v1raw: String, v2raw: String): Int {
        try {
            val v1 = v1raw.split("-", "+")[0]
            val v2 = v2raw.split("-", "+")[0]
            val a = v1.split(".").mapNotNull { it.toIntOrNull() }
            val b = v2.split(".").mapNotNull { it.toIntOrNull() }
            val max = maxOf(a.size, b.size)
            for (i in 0 until max) {
                val ai = if (i < a.size) a[i] else 0
                val bi = if (i < b.size) b[i] else 0
                if (ai != bi) return ai - bi
            }
            val tail1 = v1raw.substringAfter(v1, "")
            val tail2 = v2raw.substringAfter(v2, "")
            return tail1.compareTo(tail2, ignoreCase = true)
        } catch (e: Exception) {
            return v1raw.compareTo(v2raw, ignoreCase = true)
        }
    }

    private fun handleComparisonWithTag(
        context: Context,
        latestTagRaw: String?,
        apkUrl: String?,
        isPrerelease: Boolean,
        onResult: (UpdateResult) -> Unit
    ) {
        val latestTag = latestTagRaw?.removePrefix("v") ?: ""
        val currentVersion = try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: ""
        } catch (e: Exception) {
            ""
        }

        Log.d(TAG, "currentVersion=$currentVersion latestTag=$latestTag isPrerelease=$isPrerelease")

        if (latestTag.isBlank()) {
            onResult(UpdateResult.Error(context.getString(R.string.update_no_releases)))
            return
        }

        val cmp = compareSemver(latestTag, currentVersion)
        if (cmp > 0) {
            onResult(UpdateResult.UpdateAvailable(latestTag, apkUrl, isPrerelease))
        } else {
            onResult(UpdateResult.UpToDate)
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.status_error_open), Toast.LENGTH_SHORT).show()
        }
    }

    fun openReleasesPage(context: Context) {
        openUrl(context, RELEASES_PAGE)
    }
}

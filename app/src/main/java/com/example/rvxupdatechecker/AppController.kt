package com.example.rvxupdatechecker

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object AppController {

    fun getLocalVersion(context: Context, packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    suspend fun getRemoteVersion(url: String): String = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val bodyText = doc.body().text()
            val match = Regex("""\d+\.\d+\.\d+""").find(bodyText)
            match?.value ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }
}
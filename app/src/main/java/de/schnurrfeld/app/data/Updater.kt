package de.schnurrfeld.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A release on GitHub that can be installed. */
data class ReleaseInfo(val version: String, val notes: String, val apkUrl: String, val apkName: String, val size: Long)

/**
 * Updates via GitHub Releases of arthurtschik/Schnurrfeld.
 *
 * Release convention: tag "v1.3" (or "1.3"), APK attached as "Schnurrfeld-1.3.apk"
 * (and optionally "Schnurrfeld-1.3-Dev.apk" for the developer app).
 * The APK must be signed with the same key and have a higher versionCode, otherwise Android refuses to install it.
 */
object Updater {
    const val REPO = "arthurtschik/Schnurrfeld"
    private const val LATEST = "https://api.github.com/repos/$REPO/releases/latest"

    /** "1.2", "v1.2.1", "1.3-dev" -> [1, 2], [1, 2, 1], [1, 3] */
    fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore(' ')
            .split('.').map { it.filter(Char::isDigit) }.filter { it.isNotEmpty() }.map { it.toInt() }

    fun isNewer(remote: String, local: String): Boolean {
        val a = parts(remote); val b = parts(local)
        if (a.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Picks the right APK from a release JSON (dev app -> the "-Dev" APK, otherwise the normal one). */
    fun parseRelease(json: String, dev: Boolean): ReleaseInfo? {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val tag = o.optString("tag_name").ifEmpty { o.optString("name") }
        val assets = o.optJSONArray("assets") ?: return null
        val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
        val isDevName = { a: JSONObject -> a.optString("name").contains("dev", ignoreCase = true) }
        val pick = (if (dev) apks.firstOrNull(isDevName) else apks.firstOrNull { !isDevName(it) }) ?: apks.firstOrNull() ?: return null
        val version = tag.ifEmpty { pick.optString("name") }
        return ReleaseInfo(
            version = version.removePrefix("v").removePrefix("V"),
            notes = o.optString("body").trim(),
            apkUrl = pick.optString("browser_download_url"),
            apkName = pick.optString("name"),
            size = pick.optLong("size"),
        )
    }

    /** The newest release if it is newer than [currentVersion], else null. Throws on network errors. */
    suspend fun check(currentVersion: String, dev: Boolean): ReleaseInfo? = withContext(Dispatchers.IO) {
        val con = (URL(LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Schnurrfeld-App")
        }
        try {
            when (con.responseCode) {
                200 -> parseRelease(con.inputStream.bufferedReader().readText(), dev)?.takeIf { isNewer(it.version, currentVersion) }
                404 -> null   // no release published yet
                else -> throw java.io.IOException("GitHub antwortet mit ${con.responseCode}")
            }
        } finally { con.disconnect() }
    }

    /** Downloads the APK into the app cache, reporting progress 0..1. */
    suspend fun download(context: Context, info: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, info.apkName.ifEmpty { "Schnurrfeld-${info.version}.apk" })
        var url = URL(info.apkUrl)
        var con: HttpURLConnection
        // GitHub redirects the download to its storage host
        var hops = 0
        while (true) {
            con = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false; connectTimeout = 15_000; readTimeout = 30_000
                setRequestProperty("User-Agent", "Schnurrfeld-App")
            }
            val code = con.responseCode
            if (code in 300..399 && hops++ < 5) { url = URL(url, con.getHeaderField("Location")); con.disconnect(); continue }
            if (code != 200) { con.disconnect(); throw java.io.IOException("Download fehlgeschlagen ($code)") }
            break
        }
        val total = con.contentLengthLong.takeIf { it > 0 } ?: info.size
        try {
            con.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) withContext(Dispatchers.Main) { onProgress((done.toFloat() / total).coerceIn(0f, 1f)) }
                    }
                }
            }
        } finally { con.disconnect() }
        file
    }

    /** true if Android lets this app start installations (Android 8+ asks the user once). */
    fun canInstall(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Opens the system setting "Unbekannte Apps installieren" for this app. */
    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Hands the downloaded APK to the Android package installer. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

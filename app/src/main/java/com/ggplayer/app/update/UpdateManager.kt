package com.ggplayer.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.ggplayer.app.BuildConfig
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.data.Net
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateState()
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

/**
 * Reads the latest GitHub Release of the repo set in Settings, downloads its APK
 * and hands it to the system installer.
 */
object UpdateManager {
    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun reset() {
        val s = state
        if (s is UpdateState.Checking || s is UpdateState.Downloading) return
        state = UpdateState.Idle
    }

    /** [silent] = automatic check at app start: errors are not shown. */
    fun check(context: Context, silent: Boolean = false) {
        val current = state
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return
        state = UpdateState.Checking
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
            val info = result.getOrNull()
            if (result.isSuccess) {
                state = if (info != null && isNewer(info.versionName, BuildConfig.VERSION_NAME)) {
                    UpdateState.Available(info)
                } else {
                    UpdateState.UpToDate
                }
            } else {
                state = if (silent) UpdateState.Idle else UpdateState.Failed(friendly(result.exceptionOrNull()))
            }
        }
    }

    fun download(context: Context, info: UpdateInfo) {
        val appContext = context.applicationContext
        state = UpdateState.Downloading(info, 0)
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { downloadTo(appContext, info) } }
            val file = result.getOrNull()
            state = if (result.isSuccess && file != null) {
                UpdateState.Ready(info, file)
            } else {
                UpdateState.Failed(friendly(result.exceptionOrNull()))
            }
        }
    }

    /** Opens the system installer. Returns a message for the user when something else is needed first. */
    fun install(context: Context, file: File): String? {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            return try {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Allow GG Player to install apps on the screen that opened, then come back and tap Install again."
            } catch (e: Exception) {
                "Allow installs from this app first. Phone: Settings, Apps, Special access, Install unknown apps, GG Player. " +
                    "Fire TV: Settings, My Fire TV, Developer options, Install unknown apps. Then tap Install again."
            }
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            null
        } catch (e: Exception) {
            "Could not open the installer: ${e.message}"
        }
    }

    // ---- internals ----

    private fun fetchLatest(): UpdateInfo {
        val repo = AppSettings.repo.trim()
        if (!repo.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) {
            throw IOException("The GitHub repo must look like owner/name")
        }
        val text = Net.getText(
            "https://api.github.com/repos/$repo/releases/latest",
            mapOf("Accept" to "application/vnd.github+json"),
        )
        val json = JSONObject(text)
        val tag = json.optString("tag_name")
        if (tag.isEmpty()) throw IOException("The latest release has no version tag")
        val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()
        var apkUrl = ""
        var size = 0L
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                size = asset.optLong("size")
                break
            }
        }
        if (apkUrl.isEmpty()) throw IOException("The latest release has no APK file attached")
        return UpdateInfo(
            versionName = tag.trimStart('v', 'V'),
            notes = json.optString("body"),
            apkUrl = apkUrl,
            sizeBytes = size,
        )
    }

    private fun downloadTo(context: Context, info: UpdateInfo): File {
        val dir = File(context.cacheDir, "updates")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "GG-Player-${info.versionName}.apk")
        val conn = Net.openGet(info.apkUrl, timeoutMs = 30000)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Download failed, server answered $code")
            val headerLength = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: 0L
            val total = if (headerLength > 0) headerLength else info.sizeBytes
            var lastPercent = -1
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                state = UpdateState.Downloading(info, percent)
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (out.length() < 100_000) throw IOException("The downloaded file is too small to be the app")
        return out
    }

    private fun parts(version: String): List<Int> =
        version.split('.', '-', '+').map { piece ->
            piece.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0
        }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        val n = maxOf(r.size, l.size)
        for (i in 0 until n) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun friendly(error: Throwable?): String {
        val message = error?.message ?: "Unknown error"
        return when {
            message.contains("404") ->
                "No release found for ${AppSettings.repo}. Check the repo name in Update settings."
            message.contains("403") ->
                "GitHub refused the request (too many checks). Try again in a few minutes."
            error is java.net.UnknownHostException || error is java.net.SocketTimeoutException ->
                "No internet connection."
            else -> message
        }
    }
}

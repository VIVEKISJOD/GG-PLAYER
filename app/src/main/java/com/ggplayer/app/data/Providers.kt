package com.ggplayer.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID

enum class ProviderType(val label: String) {
    M3U("M3U link"),
    XTREAM("Xtream login"),
}

data class Provider(
    val id: String,
    val name: String,
    val type: ProviderType,
    val m3uUrl: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
) {
    /** Short line shown under the provider name. */
    fun summary(): String = when (type) {
        ProviderType.M3U -> m3uUrl
        ProviderType.XTREAM -> "$server  -  $username"
    }
}

/** Stores the user's IPTV providers in one JSON file inside the app's private folder. */
object ProviderStore {
    private var file: File? = null
    val providers = mutableStateListOf<Provider>()

    fun init(context: Context) {
        if (file != null) return
        file = File(context.applicationContext.filesDir, "providers.json")
        readFile()
    }

    /** Replaces the whole list. Used by restore. */
    fun replaceAll(list: List<Provider>) {
        providers.clear()
        providers.addAll(list)
        persist()
    }

    private fun readFile() {
        val f = file ?: return
        providers.clear()
        if (!f.exists()) return
        try {
            val array = JSONArray(f.readText())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val type = ProviderType.values().firstOrNull { it.name == o.optString("type") }
                    ?: ProviderType.M3U
                providers.add(
                    Provider(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        type = type,
                        m3uUrl = o.optString("m3uUrl"),
                        server = o.optString("server"),
                        username = o.optString("username"),
                        password = o.optString("password"),
                        epgUrl = o.optString("epgUrl"),
                    )
                )
            }
        } catch (e: Exception) {
            // A broken file is ignored. The user can add providers again.
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun get(id: String): Provider? = providers.firstOrNull { it.id == id }

    fun save(provider: Provider) {
        val index = providers.indexOfFirst { it.id == provider.id }
        if (index >= 0) providers[index] = provider else providers.add(provider)
        persist()
    }

    fun delete(id: String) {
        providers.removeAll { it.id == id }
        persist()
    }

    private fun persist() {
        val f = file ?: return
        val array = JSONArray()
        for (p in providers) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("type", p.type.name)
            o.put("m3uUrl", p.m3uUrl)
            o.put("server", p.server)
            o.put("username", p.username)
            o.put("password", p.password)
            o.put("epgUrl", p.epgUrl)
            array.put(o)
        }
        try {
            f.writeText(array.toString())
        } catch (e: Exception) {
            // Nothing sensible to do here, the list stays in memory for this session.
        }
    }

    /** Adds http:// if missing and removes a trailing slash. */
    fun cleanServer(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "http://$s"
        return s.trimEnd('/')
    }

    /** Checks that the provider really answers. Returns a short success text or an error. */
    suspend fun test(p: Provider): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (p.type) {
                ProviderType.M3U -> {
                    val head = Net.getText(p.m3uUrl.trim(), maxBytes = 4096)
                    if (!head.trimStart().startsWith("#EXTM3U")) {
                        throw IOException("The link answered, but it is not an M3U playlist")
                    }
                    "Playlist link works."
                }
                ProviderType.XTREAM -> {
                    val base = cleanServer(p.server)
                    val user = URLEncoder.encode(p.username.trim(), "UTF-8")
                    val pass = URLEncoder.encode(p.password.trim(), "UTF-8")
                    val text = Net.getText("$base/player_api.php?username=$user&password=$pass")
                    val info = JSONObject(text).optJSONObject("user_info")
                        ?: throw IOException("The server did not return account info")
                    if (info.opt("auth")?.toString() != "1") {
                        throw IOException("Login was rejected. Check username and password")
                    }
                    val status = info.optString("status", "Active")
                    "Login works. Account status: $status."
                }
            }
        }
    }
}

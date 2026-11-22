package com.ggplayer.app.data

import android.content.Context
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.Calendar
import java.util.TimeZone
import java.util.zip.GZIPInputStream

data class Channel(
    val id: String,
    val providerId: String,
    val name: String,
    val group: String,
    val logo: String,
    val tvgId: String,
    val url: String,
    /** Second address to try if the first fails (Xtream .m3u8 next to .ts). */
    val altUrl: String,
)

private fun clean(s: String): String = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

object M3uParser {
    private val attrRegex = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    fun parse(reader: BufferedReader, providerId: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""
        var group = ""
        var logo = ""
        var tvg = ""
        var pending = false
        while (true) {
            val raw = reader.readLine() ?: break
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF")) {
                val attrs = HashMap<String, String>()
                for (m in attrRegex.findAll(line)) attrs[m.groupValues[1].lowercase()] = m.groupValues[2]
                val q = line.lastIndexOf('"')
                val tail = if (q >= 0) line.substring(q + 1) else line
                name = tail.substringAfter(',', "").trim()
                if (name.isEmpty()) name = attrs["tvg-name"] ?: ""
                group = attrs["group-title"] ?: ""
                logo = attrs["tvg-logo"] ?: ""
                tvg = attrs["tvg-id"] ?: ""
                pending = true
            } else if (line.startsWith("#EXTGRP:")) {
                if (group.isEmpty()) group = line.substring(8).trim()
            } else if (line.startsWith("#")) {
                continue
            } else if (pending) {
                out.add(
                    Channel(
                        id = providerId + ":" + line.hashCode().toString(16),
                        providerId = providerId,
                        name = clean(name).ifEmpty { "Channel" },
                        group = clean(group).ifEmpty { "Other" },
                        logo = clean(logo),
                        tvgId = clean(tvg),
                        url = line,
                        altUrl = "",
                    )
                )
                pending = false
            }
        }
        return out
    }
}

object XtreamApi {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun loadChannels(p: Provider): List<Channel> {
        val base = ProviderStore.cleanServer(p.server)
        val user = p.username.trim()
        val pass = p.password.trim()
        val api = "$base/player_api.php?username=${enc(user)}&password=${enc(pass)}"
        val cats = HashMap<String, String>()
        try {
            val ca = JSONArray(Net.getText("$api&action=get_live_categories", maxBytes = 4_000_000))
            for (i in 0 until ca.length()) {
                val o = ca.optJSONObject(i) ?: continue
                cats[o.opt("category_id")?.toString() ?: ""] = o.optString("category_name")
            }
        } catch (e: Exception) {
            // Channels still load, just without group names.
        }
        val text = Net.getText("$api&action=get_live_streams", maxBytes = 80_000_000)
        val arr = JSONArray(text)
        val out = ArrayList<Channel>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val sid = o.opt("stream_id")?.toString() ?: continue
            val cat = cats[o.opt("category_id")?.toString() ?: ""] ?: "Other"
            val stem = "$base/live/${enc(user)}/${enc(pass)}/$sid"
            out.add(
                Channel(
                    id = p.id + ":" + sid,
                    providerId = p.id,
                    name = clean(o.optString("name")).ifEmpty { "Channel $sid" },
                    group = clean(cat).ifEmpty { "Other" },
                    logo = clean(o.optString("stream_icon")),
                    tvgId = clean(o.optString("epg_channel_id")),
                    url = "$stem.ts",
                    altUrl = "$stem.m3u8",
                )
            )
        }
        return out
    }
}

/** Loads channel lists. Keeps them in memory and in a cache file so the next start is fast. */
object ChannelRepo {
    private val memory = HashMap<String, List<Channel>>()
    private const val MAX_AGE_MS = 12 * 60 * 60 * 1000L

    private fun cacheFile(context: Context, providerId: String) =
        File(context.applicationContext.cacheDir, "channels_$providerId.tsv")

    private val stale = HashSet<String>()

    /** Call after a provider was edited or deleted, so its channels are fetched again. */
    fun invalidate(providerId: String) {
        memory.remove(providerId)
        stale.add(providerId)
    }

    suspend fun load(context: Context, p: Provider, requested: Boolean): List<Channel> =
        withContext(Dispatchers.IO) {
            val force = requested || stale.remove(p.id)
            if (!force) memory[p.id]?.let { return@withContext it }
            val f = cacheFile(context, p.id)
            if (!force && f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE_MS) {
                val cached = readCache(f)
                if (cached.isNotEmpty()) {
                    memory[p.id] = cached
                    return@withContext cached
                }
            }
            val list = when (p.type) {
                ProviderType.M3U -> {
                    val stream = Net.openStream(p.m3uUrl.trim())
                    stream.use { s ->
                        M3uParser.parse(BufferedReader(InputStreamReader(s, "UTF-8"), 65536), p.id)
                    }
                }
                ProviderType.XTREAM -> XtreamApi.loadChannels(p)
            }
            if (list.isEmpty()) throw IOException("The provider answered, but no channels were found")
            memory[p.id] = list
            writeCache(f, list)
            list
        }

    private fun writeCache(f: File, list: List<Channel>) {
        try {
            f.bufferedWriter().use { w ->
                for (c in list) {
                    w.write(listOf(c.id, c.name, c.group, c.logo, c.tvgId, c.url, c.altUrl).joinToString("\t"))
                    w.write("\n")
                }
            }
        } catch (e: Exception) {
            // The cache is optional.
        }
    }

    private fun readCache(f: File): List<Channel> {
        val out = ArrayList<Channel>()
        try {
            f.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val p = line.split("\t")
                    if (p.size < 7) continue
                    out.add(
                        Channel(
                            id = p[0], providerId = p[0].substringBefore(':'), name = p[1], group = p[2],
                            logo = p[3], tvgId = p[4], url = p[5], altUrl = p[6],
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return out
    }
}

/** Favourite channels, kept in SharedPreferences. */
object FavoriteStore {
    private var prefs: android.content.SharedPreferences? = null
    var ids by mutableStateOf<Set<String>>(emptySet())
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("gg_fav", Context.MODE_PRIVATE)
        prefs = p
        reload()
    }

    fun reload() {
        ids = prefs?.getStringSet("ids", emptySet())?.toSet() ?: emptySet()
    }

    fun isFavorite(id: String) = ids.contains(id)

    fun toggle(id: String) {
        ids = if (ids.contains(id)) ids - id else ids + id
        prefs?.edit()?.putStringSet("ids", HashSet(ids))?.apply()
    }

    fun setAll(list: Collection<String>) {
        ids = list.toSet()
        prefs?.edit()?.putStringSet("ids", HashSet(ids))?.apply()
    }
}

/** Remembers where each movie or episode stopped. */
object ResumeStore {
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("gg_resume", Context.MODE_PRIVATE)
        }
    }

    fun get(key: String): Long = prefs?.getLong(key, 0L) ?: 0L

    fun put(key: String, positionMs: Long) {
        prefs?.edit()?.putLong(key, positionMs)?.apply()
    }

    fun clear(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
}

/** Hides adult groups when the parental option is on. */
object Parental {
    private val words = setOf("xxx", "adult", "adults", "porn", "erotic", "playboy", "hustler")

    fun isAdult(c: Channel): Boolean {
        val g = c.group.lowercase()
        if (g.contains("18+") || c.name.lowercase().contains("18+")) return true
        val tokens = g.replace(Regex("[^a-z0-9]+"), " ").trim().split(" ")
        return tokens.any { words.contains(it) }
    }

    fun filter(list: List<Channel>): List<Channel> =
        if (AppSettings.hideAdult) list.filter { !isAdult(it) } else list
}

// ---- TV guide (XMLTV) ----

data class Programme(val start: Long, val stop: Long, val title: String, val desc: String)

object EpgStore {
    var version by mutableStateOf(0)
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var loadedProvider by mutableStateOf("")
        private set

    private var byChannel: Map<String, List<Programme>> = emptyMap()
    private var nameToId: Map<String, String> = emptyMap()

    private val nonAlnum = Regex("[^a-z0-9]+")

    fun normName(s: String): String = s.lowercase().replace(nonAlnum, "")

    fun programmes(c: Channel): List<Programme> {
        val direct = byChannel[c.tvgId.lowercase()]
        if (direct != null && c.tvgId.isNotEmpty()) return direct
        val n = normName(c.name)
        val viaName = nameToId[n]?.let { byChannel[it] }
        return viaName ?: byChannel[n] ?: emptyList()
    }

    fun now(c: Channel, time: Long = System.currentTimeMillis()): Programme? =
        programmes(c).firstOrNull { it.start <= time && time < it.stop }

    fun next(c: Channel, time: Long = System.currentTimeMillis()): Programme? =
        programmes(c).firstOrNull { it.start > time }

    fun guideUrl(p: Provider): String = when {
        p.epgUrl.isNotBlank() -> p.epgUrl.trim()
        p.type == ProviderType.XTREAM ->
            "${ProviderStore.cleanServer(p.server)}/xmltv.php?username=${URLEncoder.encode(p.username.trim(), "UTF-8")}" +
                "&password=${URLEncoder.encode(p.password.trim(), "UTF-8")}"
        else -> ""
    }

    suspend fun load(p: Provider, force: Boolean = false) {
        if (!force && loadedProvider == p.id && byChannel.isNotEmpty()) return
        val url = guideUrl(p)
        if (url.isEmpty()) {
            message = "No TV guide link for this provider."
            return
        }
        loading = true
        message = "Loading TV guide..."
        try {
            withContext(Dispatchers.IO) { parseInto(url) }
            loadedProvider = p.id
            message = if (byChannel.isEmpty()) "The guide link worked, but it has no programmes." else ""
        } catch (e: Exception) {
            message = "TV guide could not be loaded: ${e.message ?: "unknown error"}"
        } finally {
            loading = false
            version++
        }
    }

    private fun parseInto(url: String) {
        val raw = BufferedInputStream(Net.openStream(url, timeoutMs = 60000), 65536)
        raw.use { bis ->
            bis.mark(2)
            val b1 = bis.read()
            val b2 = bis.read()
            bis.reset()
            val input = if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(bis, 65536) else bis
            val now = System.currentTimeMillis()
            val from = now - 2 * 3600_000L
            val to = now + 36 * 3600_000L
            val map = HashMap<String, MutableList<Programme>>()
            val names = HashMap<String, String>()

            val parser = Xml.newPullParser()
            parser.setInput(input, null)
            var curChannel: String? = null
            var inProg = false
            var pCh = ""
            var pStart = 0L
            var pStop = 0L
            var pTitle = ""
            var pDesc = ""
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "channel" -> {
                            curChannel = parser.getAttributeValue(null, "id")
                        }
                        "display-name" -> {
                            val txt = parser.nextText()
                            val id = curChannel
                            if (id != null) {
                                val key = normName(txt)
                                if (key.isNotEmpty() && !names.containsKey(key)) names[key] = id.lowercase()
                            }
                        }
                        "programme" -> {
                            inProg = true
                            pCh = (parser.getAttributeValue(null, "channel") ?: "").lowercase()
                            pStart = parseTime(parser.getAttributeValue(null, "start") ?: "")
                            pStop = parseTime(parser.getAttributeValue(null, "stop") ?: "")
                            pTitle = ""
                            pDesc = ""
                        }
                        "title" -> {
                            if (inProg && pTitle.isEmpty()) pTitle = parser.nextText()
                        }
                        "desc" -> {
                            if (inProg && pDesc.isEmpty()) pDesc = parser.nextText()
                        }
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    if (parser.name == "channel") {
                        curChannel = null
                    } else if (parser.name == "programme") {
                        if (inProg && pStop > from && pStart < to && pStop > pStart) {
                            map.getOrPut(pCh) { ArrayList() }.add(Programme(pStart, pStop, pTitle.trim(), pDesc.trim()))
                        }
                        inProg = false
                    }
                }
                event = parser.next()
            }
            for (list in map.values) list.sortBy { it.start }
            byChannel = map
            nameToId = names
        }
    }

    /** XMLTV time like 20241121183000 +0530. */
    fun parseTime(raw: String): Long {
        val t = raw.trim()
        if (t.length < 12) return 0L
        return try {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.clear()
            val sec = if (t.length >= 14) t.substring(12, 14).toIntOrNull() ?: 0 else 0
            cal.set(
                t.substring(0, 4).toInt(), t.substring(4, 6).toInt() - 1, t.substring(6, 8).toInt(),
                t.substring(8, 10).toInt(), t.substring(10, 12).toInt(), sec,
            )
            var ms = cal.timeInMillis
            val rest = t.substring(minOf(14, t.length)).trim()
            if (rest.length >= 5 && (rest[0] == '+' || rest[0] == '-')) {
                val sign = if (rest[0] == '-') -1 else 1
                val oh = rest.substring(1, 3).toIntOrNull() ?: 0
                val om = rest.substring(3, 5).toIntOrNull() ?: 0
                ms -= sign * (oh * 60 + om) * 60_000L
            }
            ms
        } catch (e: Exception) {
            0L
        }
    }
}

/** Saves and restores everything the user set up, as one block of text. */
object Backup {
    fun export(context: Context): String {
        val root = JSONObject()
        val settings = JSONObject()
        for ((k, v) in AppSettings.rawValues()) {
            if (v is String || v is Boolean) settings.put(k, v)
        }
        root.put("settings", settings)
        val providers = JSONArray()
        for (p in ProviderStore.providers) {
            providers.put(
                JSONObject()
                    .put("id", p.id).put("name", p.name).put("type", p.type.name)
                    .put("m3uUrl", p.m3uUrl).put("server", p.server).put("username", p.username)
                    .put("password", p.password).put("epgUrl", p.epgUrl)
            )
        }
        root.put("providers", providers)
        root.put("addons", JSONArray(AddonStore.urls()))
        root.put("favorites", JSONArray(FavoriteStore.ids.toList()))
        root.put("app", "GGPlayer")
        return root.toString()
    }

    suspend fun restore(context: Context, text: String): Result<String> = runCatching {
        val root = JSONObject(text.trim())
        if (root.optString("app") != "GGPlayer") throw IOException("This is not a GG Player backup")
        val settings = root.optJSONObject("settings")
        if (settings != null) {
            val map = HashMap<String, Any?>()
            val keys = settings.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = settings.get(k)
            }
            AppSettings.writeRaw(map)
        }
        val pa = root.optJSONArray("providers")
        var providerCount = 0
        if (pa != null) {
            val list = ArrayList<Provider>()
            for (i in 0 until pa.length()) {
                val o = pa.getJSONObject(i)
                list.add(
                    Provider(
                        id = o.optString("id").ifBlank { ProviderStore.newId() },
                        name = o.optString("name"),
                        type = ProviderType.values().firstOrNull { it.name == o.optString("type") } ?: ProviderType.M3U,
                        m3uUrl = o.optString("m3uUrl"),
                        server = o.optString("server"),
                        username = o.optString("username"),
                        password = o.optString("password"),
                        epgUrl = o.optString("epgUrl"),
                    )
                )
            }
            ProviderStore.replaceAll(list)
            providerCount = list.size
        }
        val fa = root.optJSONArray("favorites")
        if (fa != null) {
            val favs = ArrayList<String>()
            for (i in 0 until fa.length()) favs.add(fa.optString(i))
            FavoriteStore.setAll(favs)
        }
        var addonOk = 0
        var addonFail = 0
        val aa = root.optJSONArray("addons")
        if (aa != null) {
            for (i in 0 until aa.length()) {
                val url = aa.optString(i)
                if (AddonStore.urls().contains(url)) continue
                if (AddonStore.add(url).isSuccess) addonOk++ else addonFail++
            }
        }
        "Restored $providerCount providers, $addonOk addons added" +
            (if (addonFail > 0) ", $addonFail addons could not be reached" else "") + "."
    }
}

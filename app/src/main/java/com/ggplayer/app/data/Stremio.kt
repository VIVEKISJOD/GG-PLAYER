package com.ggplayer.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder

/** Like optString, but a JSON null gives an empty text instead of the word null. */
private fun JSONObject.s(key: String): String = if (isNull(key)) "" else optString(key)

// ---- Models ----

data class CatalogDef(
    val type: String,
    val id: String,
    val name: String,
    val searchable: Boolean,
    /** True when the catalog needs extra input (like a genre), so it cannot be shown as a plain row. */
    val needsExtra: Boolean,
)

data class ResourceDef(val name: String, val types: List<String>, val idPrefixes: List<String>)

data class Addon(
    val manifestUrl: String,
    val base: String,
    val name: String,
    val version: String,
    val description: String,
    val types: List<String>,
    val idPrefixes: List<String>,
    val resources: List<ResourceDef>,
    val catalogs: List<CatalogDef>,
    val rawManifest: String,
) {
    fun hasResource(name: String): Boolean = resources.any { it.name == name }

    /** True when this addon answers [resource] (catalog, meta, stream) for this type and id. */
    fun supports(resource: String, type: String, id: String): Boolean {
        for (r in resources) {
            if (r.name != resource) continue
            val t = if (r.types.isEmpty()) types else r.types
            val p = if (r.idPrefixes.isEmpty()) idPrefixes else r.idPrefixes
            val typeOk = t.isEmpty() || t.contains(type)
            val idOk = p.isEmpty() || p.any { id.startsWith(it) }
            if (typeOk && idOk) return true
        }
        return false
    }
}

data class MetaPreview(
    val type: String,
    val id: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String,
    val year: String,
    val rating: String,
    val genres: List<String>,
)

data class Episode(
    val id: String,
    val season: Int,
    val number: Int,
    val title: String,
    val thumbnail: String?,
    val overview: String,
)

data class MetaDetail(val preview: MetaPreview, val episodes: List<Episode>)

/** One raw stream entry as an addon returned it. */
data class RawStream(
    val name: String,
    val title: String,
    val description: String,
    val url: String?,
    val infoHash: String?,
    val fileName: String,
    val videoSize: Long,
    val headers: Map<String, String>,
)

// ---- Storage ----

/** The movie and series addons the user added. Saved as addons.json. */
object AddonStore {
    const val CINEMETA = "https://v3-cinemeta.strem.io/manifest.json"

    private var file: File? = null
    val addons = mutableStateListOf<Addon>()

    /** Goes up every time the list changes, so screens know to reload catalogs. */
    var version by mutableStateOf(0)
        private set

    fun init(context: Context) {
        if (file != null) return
        file = File(context.applicationContext.filesDir, "addons.json")
        readFile()
    }

    fun reload() {
        readFile()
    }

    private fun readFile() {
        val f = file ?: return
        addons.clear()
        if (f.exists()) {
            try {
                val array = JSONArray(f.readText())
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val a = parseManifest(o.s("url"), o.s("manifest"))
                    if (a != null) addons.add(a)
                }
            } catch (e: Exception) {
                // A broken file is ignored. The user can add addons again.
            }
        }
        version++
    }

    private fun persist() {
        val f = file ?: return
        val array = JSONArray()
        for (a in addons) {
            val o = JSONObject()
            o.put("url", a.manifestUrl)
            o.put("manifest", a.rawManifest)
            array.put(o)
        }
        try {
            f.writeText(array.toString())
        } catch (e: Exception) {
            // Stays in memory for this session.
        }
        version++
    }

    fun urls(): List<String> = addons.map { it.manifestUrl }

    fun normalizeUrl(raw: String): String {
        var u = raw.trim()
        if (u.startsWith("stremio://", true)) u = "https://" + u.substring(10)
        if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
        val noQuery = u.substringBefore('#')
        return if (noQuery.endsWith("manifest.json")) noQuery else noQuery.trimEnd('/') + "/manifest.json"
    }

    fun parseManifest(url: String, text: String): Addon? {
        return try {
            val o = JSONObject(text)
            val name = o.s("name")
            if (name.isBlank() || !o.has("resources")) return null
            val topTypes = strings(o.optJSONArray("types"))
            val topPrefixes = strings(o.optJSONArray("idPrefixes"))
            val resources = ArrayList<ResourceDef>()
            val ra = o.optJSONArray("resources") ?: JSONArray()
            for (i in 0 until ra.length()) {
                val item = ra.get(i)
                if (item is String) {
                    resources.add(ResourceDef(item, emptyList(), emptyList()))
                } else if (item is JSONObject) {
                    resources.add(
                        ResourceDef(
                            item.s("name"),
                            strings(item.optJSONArray("types")),
                            strings(item.optJSONArray("idPrefixes")),
                        )
                    )
                }
            }
            val catalogs = ArrayList<CatalogDef>()
            val ca = o.optJSONArray("catalogs") ?: JSONArray()
            for (i in 0 until ca.length()) {
                val c = ca.optJSONObject(i) ?: continue
                var searchable = false
                var needsExtra = false
                val extra = c.optJSONArray("extra")
                if (extra != null) {
                    for (j in 0 until extra.length()) {
                        val e = extra.optJSONObject(j) ?: continue
                        if (e.s("name") == "search") searchable = true
                        if (e.optBoolean("isRequired", false)) needsExtra = true
                    }
                }
                val required = c.optJSONArray("extraRequired")
                if (required != null && required.length() > 0) needsExtra = true
                catalogs.add(
                    CatalogDef(
                        type = c.s("type"),
                        id = c.s("id"),
                        name = c.s("name").ifBlank { c.s("id") },
                        searchable = searchable,
                        needsExtra = needsExtra,
                    )
                )
            }
            val base = url.removeSuffix("manifest.json").trimEnd('/')
            Addon(
                manifestUrl = url,
                base = base,
                name = name,
                version = o.s("version"),
                description = o.s("description"),
                types = topTypes,
                idPrefixes = topPrefixes,
                resources = resources,
                catalogs = catalogs,
                rawManifest = text,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun strings(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        val out = ArrayList<String>()
        for (i in 0 until a.length()) out.add(a.optString(i))
        return out
    }

    /** Downloads the manifest, checks it and saves the addon. Returns the addon or an error. */
    suspend fun add(rawUrl: String): Result<Addon> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalizeUrl(rawUrl)
            val text = Net.getText(url, maxBytes = 1_000_000)
            val addon = parseManifest(url, text)
                ?: throw IOException("This link is not a Stremio addon manifest")
            if (addons.any { it.manifestUrl == addon.manifestUrl }) {
                throw IOException("This addon is already added")
            }
            withContext(Dispatchers.Main) {
                addons.add(addon)
                persist()
            }
            addon
        }
    }

    fun remove(url: String) {
        addons.removeAll { it.manifestUrl == url }
        persist()
    }
}

// ---- Calls to addons ----

object StremioApi {
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun getJson(url: String): JSONObject = JSONObject(Net.getText(url, maxBytes = 8_000_000))

    suspend fun fetchCatalog(a: Addon, c: CatalogDef, search: String? = null): List<MetaPreview> =
        withContext(Dispatchers.IO) {
            val extra = if (search != null) "/search=${enc(search)}" else ""
            val o = getJson("${a.base}/catalog/${enc(c.type)}/${enc(c.id)}$extra.json")
            val arr = o.optJSONArray("metas") ?: JSONArray()
            val out = ArrayList<MetaPreview>()
            for (i in 0 until minOf(arr.length(), 60)) {
                val m = arr.optJSONObject(i) ?: continue
                parsePreview(m, c.type)?.let { out.add(it) }
            }
            out
        }

    private fun parsePreview(m: JSONObject, defaultType: String): MetaPreview? {
        val id = m.s("id")
        val name = m.s("name")
        if (id.isBlank() || name.isBlank()) return null
        val genres = ArrayList<String>()
        val g = m.optJSONArray("genres") ?: m.optJSONArray("genre")
        if (g != null) for (i in 0 until g.length()) genres.add(g.optString(i))
        val year = m.s("year").ifBlank { m.s("releaseInfo") }
        return MetaPreview(
            type = m.s("type").ifBlank { defaultType },
            id = id,
            name = name,
            poster = m.s("poster").ifBlank { null },
            background = m.s("background").ifBlank { null },
            description = m.s("description"),
            year = year,
            rating = m.s("imdbRating"),
            genres = genres,
        )
    }

    /** Asks every addon that offers meta for this item, returns the first answer. */
    suspend fun fetchMeta(type: String, id: String): MetaDetail? = withContext(Dispatchers.IO) {
        for (a in AddonStore.addons.toList()) {
            if (!a.supports("meta", type, id)) continue
            try {
                val o = getJson("${a.base}/meta/${enc(type)}/${enc(id)}.json")
                val m = o.optJSONObject("meta") ?: continue
                val preview = parsePreview(m, type) ?: continue
                val episodes = ArrayList<Episode>()
                val v = m.optJSONArray("videos")
                if (v != null) {
                    for (i in 0 until v.length()) {
                        val e = v.optJSONObject(i) ?: continue
                        val eid = e.s("id")
                        if (eid.isBlank()) continue
                        val num = if (e.has("episode")) e.optInt("episode") else e.optInt("number")
                        episodes.add(
                            Episode(
                                id = eid,
                                season = e.optInt("season"),
                                number = num,
                                title = e.s("title").ifBlank { e.s("name") },
                                thumbnail = e.s("thumbnail").ifBlank { null },
                                overview = e.s("overview").ifBlank { e.s("description") },
                            )
                        )
                    }
                }
                episodes.sortWith(compareBy({ it.season }, { it.number }))
                return@withContext MetaDetail(preview, episodes)
            } catch (e: Exception) {
                // Try the next addon.
            }
        }
        null
    }

    suspend fun fetchStreams(a: Addon, type: String, id: String): List<RawStream> =
        withContext(Dispatchers.IO) {
            val o = getJson("${a.base}/stream/${enc(type)}/${enc(id)}.json")
            val arr = o.optJSONArray("streams") ?: JSONArray()
            val out = ArrayList<RawStream>()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val hints = s.optJSONObject("behaviorHints")
                val headers = HashMap<String, String>()
                val req = hints?.optJSONObject("proxyHeaders")?.optJSONObject("request")
                if (req != null) {
                    val keys = req.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        headers[k] = req.optString(k)
                    }
                }
                out.add(
                    RawStream(
                        name = s.s("name"),
                        title = s.s("title"),
                        description = s.s("description"),
                        url = s.s("url").ifBlank { null },
                        infoHash = s.s("infoHash").ifBlank { null },
                        fileName = hints?.s("filename") ?: "",
                        videoSize = hints?.optLong("videoSize", 0L) ?: 0L,
                        headers = headers,
                    )
                )
            }
            out
        }
}

package com.ggplayer.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** One playable (or not playable) stream, with the badges the app shows for it. */
data class StreamOption(
    val addonName: String,
    val addonIndex: Int,
    val title: String,
    val detail: String,
    val url: String?,
    val isTorrent: Boolean,
    val headers: Map<String, String>,
    val languages: List<Lang>,
    val multiAudio: Boolean,
    val resolution: Int,
    val isCam: Boolean,
    val sizeBytes: Long,
    val score: Long,
) {
    val playable: Boolean get() = url != null

    /** Text for the quality badge, for example 1080p or 4K. */
    fun qualityLabel(): String = when {
        resolution >= 2160 -> "4K"
        resolution > 0 -> "${resolution}p"
        else -> "Unknown"
    }
}

object StreamParser {
    private val sizeRegex = Regex("""(\d+(?:\.\d+)?)\s?(GB|MB)""", RegexOption.IGNORE_CASE)
    private val resTokens = mapOf(
        "2160p" to 2160, "4k" to 2160, "uhd" to 2160, "2160" to 2160,
        "1440p" to 1440,
        "1080p" to 1080, "1080i" to 1080, "fhd" to 1080, "1080" to 1080,
        "720p" to 720, "hd720" to 720, "720" to 720,
        "576p" to 576,
        "480p" to 480, "480" to 480,
        "360p" to 360,
    )
    private val camTokens = setOf("cam", "hdcam", "camrip", "hdts", "telesync", "telecine")
    private val resRank = listOf(360, 480, 576, 720, 1080, 1440, 2160)

    fun parse(raw: RawStream, addonName: String, addonIndex: Int): StreamOption {
        val all = listOf(raw.name, raw.title, raw.description, raw.fileName).joinToString(" ")
        val tokens = all.lowercase().replace(Regex("[^a-z0-9+]+"), " ").trim().split(" ").toSet()

        val languages = ArrayList<Lang>()
        for (l in realLanguages) {
            if (l.tokens.any { tokens.contains(it) }) languages.add(l)
        }
        val lower = all.lowercase()
        val multi = lower.contains("dual audio") || lower.contains("multi audio") ||
            lower.contains("dual-audio") || tokens.contains("multi") || tokens.contains("dual")

        var res = 0
        for (t in tokens) {
            val r = resTokens[t] ?: continue
            if (r > res) res = r
        }
        val cam = tokens.any { camTokens.contains(it) }

        var size = raw.videoSize
        if (size <= 0) {
            val m = sizeRegex.find(all)
            if (m != null) {
                val n = m.groupValues[1].toDoubleOrNull() ?: 0.0
                size = if (m.groupValues[2].equals("GB", true)) (n * 1024 * 1024 * 1024).toLong()
                else (n * 1024 * 1024).toLong()
            }
        }

        val title = raw.title.ifBlank { raw.fileName }.ifBlank { raw.name }.ifBlank { addonName }
        val detail = listOf(raw.name, raw.description).filter { it.isNotBlank() }.joinToString("  ")
        val option = StreamOption(
            addonName = addonName,
            addonIndex = addonIndex,
            title = title.replace("\n", "  ").trim(),
            detail = detail.replace("\n", "  ").trim(),
            url = raw.url,
            isTorrent = raw.url == null && raw.infoHash != null,
            headers = raw.headers,
            languages = languages,
            multiAudio = multi,
            resolution = res,
            isCam = cam,
            sizeBytes = size,
            score = 0,
        )
        return option.copy(score = score(option))
    }

    /** Language matters most, then quality, then the order of the addons. */
    fun score(s: StreamOption): Long {
        val primary = AppSettings.primaryLang
        val secondary = AppSettings.secondaryLang
        val lang = when {
            s.languages.contains(primary) -> 5
            s.multiAudio -> 4
            secondary != Lang.None && s.languages.contains(secondary) -> 3
            s.languages.isEmpty() -> 2
            else -> 1
        }
        val want = AppSettings.quality.res
        val quality = when {
            s.resolution == 0 -> 40
            s.resolution == want -> 100
            else -> {
                val gap = Math.abs(resRank.indexOf(s.resolution).coerceAtLeast(0) - resRank.indexOf(want))
                // Below the wish is better than above it.
                if (s.resolution < want) 90 - gap * 8 else 60 - gap * 8
            }
        } - (if (s.isCam) 200 else 0)
        var total = lang * 1_000_000L + (quality + 300) * 1000L - s.addonIndex
        if (!s.playable) total -= 10_000_000_000L
        return total
    }

    /** Is this language badge a match for the user's first or second choice? 2 first, 1 second, 0 none. */
    fun languageMatch(l: Lang): Int = when {
        l == AppSettings.primaryLang -> 2
        l != Lang.None && l == AppSettings.secondaryLang -> 1
        else -> 0
    }
}

/** Collects streams from every stream addon and sorts them by the user's preferences. */
object StreamResolver {
    private data class Cached(val time: Long, val list: List<StreamOption>)

    private val cache = HashMap<String, Cached>()

    suspend fun resolve(type: String, id: String, force: Boolean = false): List<StreamOption> {
        val key = "$type|$id|${AddonStore.version}"
        val hit = cache[key]
        if (!force && hit != null && System.currentTimeMillis() - hit.time < 10 * 60_000L) {
            // Scores depend on settings, so sort again.
            return hit.list.map { it.copy(score = StreamParser.score(it)) }.sortedByDescending { it.score }
        }
        val addons = AddonStore.addons.toList().withIndex().filter { it.value.supports("stream", type, id) }
        val result = coroutineScope {
            addons.map { (index, addon) ->
                async {
                    try {
                        withTimeout(20_000) {
                            StremioApi.fetchStreams(addon, type, id)
                                .map { StreamParser.parse(it, addon.name, index) }
                        }
                    } catch (e: Exception) {
                        emptyList<StreamOption>()
                    }
                }
            }.awaitAll().flatten()
        }
        val sorted = result.sortedByDescending { it.score }
        cache[key] = Cached(System.currentTimeMillis(), sorted)
        return sorted
    }
}

/** One row of posters on the Home page. */
data class CatalogRow(val key: String, val title: String, val type: String, val items: List<MetaPreview>)

fun typeLabel(type: String): String = when (type) {
    "movie" -> "Movies"
    "series" -> "Series"
    "tv" -> "TV"
    "channel" -> "Channels"
    else -> type.replaceFirstChar { it.uppercase() }
}

/** Loads the poster rows from the catalogs of all addons. */
object HomeRepo {
    var rows by mutableStateOf<List<CatalogRow>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set

    private var loadedVersion = -1
    private val mutex = Mutex()

    suspend fun load(force: Boolean = false) {
        mutex.withLock {
            if (!force && loadedVersion == AddonStore.version) return
            val version = AddonStore.version
            loading = true
            try {
                val jobs = ArrayList<Triple<Addon, CatalogDef, Int>>()
                for (a in AddonStore.addons.toList()) {
                    var count = 0
                    for (c in a.catalogs) {
                        if (c.needsExtra || count >= 6) continue
                        if (!a.hasResource("catalog")) continue
                        jobs.add(Triple(a, c, jobs.size))
                        count++
                    }
                }
                val limited = jobs.take(14)
                val loaded = coroutineScope {
                    limited.map { (addon, cat, _) ->
                        async(Dispatchers.IO) {
                            try {
                                withTimeout(20_000) {
                                    val items = StremioApi.fetchCatalog(addon, cat)
                                    if (items.isEmpty()) null
                                    else CatalogRow(
                                        key = addon.manifestUrl + "|" + cat.type + "|" + cat.id,
                                        title = "${cat.name} - ${typeLabel(cat.type)}",
                                        type = cat.type,
                                        items = items,
                                    )
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll()
                }
                rows = loaded.filterNotNull()
                loadedVersion = version
            } finally {
                loading = false
            }
        }
    }

    /** Search in every catalog that supports search for this type. */
    suspend fun search(query: String, type: String): List<MetaPreview> = coroutineScope {
        val jobs = ArrayList<Pair<Addon, CatalogDef>>()
        for (a in AddonStore.addons.toList()) {
            for (c in a.catalogs) {
                if (c.searchable && c.type == type) jobs.add(Pair(a, c))
            }
        }
        val lists = jobs.take(6).map { (addon, cat) ->
            async(Dispatchers.IO) {
                try {
                    withTimeout(20_000) { StremioApi.fetchCatalog(addon, cat, query) }
                } catch (e: Exception) {
                    emptyList<MetaPreview>()
                }
            }
        }.awaitAll()
        val seen = HashSet<String>()
        val out = ArrayList<MetaPreview>()
        for (list in lists) for (m in list) if (seen.add(m.id)) out.add(m)
        out
    }
}

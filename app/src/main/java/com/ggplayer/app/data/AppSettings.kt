package com.ggplayer.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AccentChoice(val label: String, val color: Color) {
    Orange("Orange", Color(0xFFFF8A1F)),
    Gold("Gold", Color(0xFFC9A24B)),
    Blue("Blue", Color(0xFF3C8DD9)),
    Green("Green", Color(0xFF4CB782)),
}

enum class LayoutMode(val label: String) {
    Auto("Auto"),
    Tv("TV"),
    Phone("Phone or tablet"),
}

enum class TextSize(val label: String, val scale: Float) {
    Normal("Normal", 1f),
    Large("Large", 1.15f),
    ExtraLarge("Extra large", 1.3f),
}

enum class TvMargin(val label: String, val value: Int) {
    Small("Small", 24),
    Normal("Normal", 48),
    Large("Large", 72),
}

/** Names match ui.Section so GGApp can map one to the other. */
enum class StartScreen(val label: String) {
    Home("Home"),
    LiveTv("Live TV"),
    Library("Movies and Series"),
}

/** Returns the entry after [current], wrapping around. Used by "tap to change" rows. */
fun <T> Array<T>.cycle(current: T): T {
    val index = indexOf(current)
    return this[(index + 1) % size]
}

fun <T> List<T>.cycle(current: T): T {
    val index = indexOf(current)
    return this[(index + 1) % size]
}

/** All app settings. Values are Compose state, so screens update at once when one changes. */
object AppSettings {
    const val DEFAULT_REPO = "VIVEKISJOD/GG-PLAYER"
    const val DEFAULT_UA = "GGPlayer/0.3 (Android)"

    private var prefs: SharedPreferences? = null

    var autoCheckUpdates by mutableStateOf(true)
        private set
    var repo by mutableStateOf(DEFAULT_REPO)
        private set
    var accent by mutableStateOf(AccentChoice.Orange)
        private set
    var textSize by mutableStateOf(TextSize.Normal)
        private set
    var layoutMode by mutableStateOf(LayoutMode.Auto)
        private set
    var tvMargin by mutableStateOf(TvMargin.Normal)
        private set
    var startScreen by mutableStateOf(StartScreen.Home)
        private set

    // Streaming preferences (step 3)
    var primaryLang by mutableStateOf(Lang.Hindi)
        private set
    var secondaryLang by mutableStateOf(Lang.English)
        private set
    var quality by mutableStateOf(QualityPref.Q1080)
        private set
    var autoPick by mutableStateOf(true)
        private set
    var autoPlayOnOpen by mutableStateOf(false)
        private set

    // Playback
    var resume by mutableStateOf(true)
        private set
    var aspect by mutableStateOf(AspectChoice.Fit)
        private set
    var seekStep by mutableStateOf(SeekStep.S10)
        private set

    // Audio and subtitles
    var preferAudioLang by mutableStateOf(true)
        private set
    var subsOn by mutableStateOf(false)
        private set
    var subLang by mutableStateOf(Lang.English)
        private set
    var subSize by mutableStateOf(SubSize.Normal)
        private set

    // Network
    var buffer by mutableStateOf(BufferChoice.Normal)
        private set
    var timeout by mutableStateOf(TimeoutChoice.Normal)
        private set
    var userAgent by mutableStateOf(DEFAULT_UA)
        private set

    // Parental
    var hideAdult by mutableStateOf(false)
        private set
    var pin by mutableStateOf("")
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("gg_settings", Context.MODE_PRIVATE)
        prefs = p
        load(p)
    }

    /** Reads everything again from disk. Used after a backup is restored. */
    fun reload() {
        prefs?.let { load(it) }
    }

    /** Raw copy of all saved values, used by backup. */
    fun rawValues(): Map<String, Any?> = prefs?.all ?: emptyMap()

    /** Writes raw values (strings and booleans) back, used by restore. */
    fun writeRaw(values: Map<String, Any?>) {
        val e = prefs?.edit() ?: return
        e.clear()
        for ((k, v) in values) {
            when (v) {
                is String -> e.putString(k, v)
                is Boolean -> e.putBoolean(k, v)
            }
        }
        e.apply()
        reload()
    }

    private fun load(p: SharedPreferences) {
        autoCheckUpdates = p.getBoolean("auto_check_updates", true)
        repo = p.getString("repo", DEFAULT_REPO) ?: DEFAULT_REPO
        accent = readEnum(p, "accent", AccentChoice.Orange)
        textSize = readEnum(p, "text_size", TextSize.Normal)
        layoutMode = readEnum(p, "layout_mode", LayoutMode.Auto)
        tvMargin = readEnum(p, "tv_margin", TvMargin.Normal)
        startScreen = readEnum(p, "start_screen", StartScreen.Home)
        primaryLang = readEnum(p, "primary_lang", Lang.Hindi)
        secondaryLang = readEnum(p, "secondary_lang", Lang.English)
        quality = readEnum(p, "quality", QualityPref.Q1080)
        autoPick = p.getBoolean("auto_pick", true)
        autoPlayOnOpen = p.getBoolean("auto_play_open", false)
        resume = p.getBoolean("resume", true)
        aspect = readEnum(p, "aspect", AspectChoice.Fit)
        seekStep = readEnum(p, "seek_step", SeekStep.S10)
        preferAudioLang = p.getBoolean("prefer_audio_lang", true)
        subsOn = p.getBoolean("subs_on", false)
        subLang = readEnum(p, "sub_lang", Lang.English)
        subSize = readEnum(p, "sub_size", SubSize.Normal)
        buffer = readEnum(p, "buffer", BufferChoice.Normal)
        timeout = readEnum(p, "timeout", TimeoutChoice.Normal)
        userAgent = p.getString("user_agent", DEFAULT_UA) ?: DEFAULT_UA
        hideAdult = p.getBoolean("hide_adult", false)
        pin = p.getString("pin", "") ?: ""
    }

    private fun saveBool(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(p: SharedPreferences, key: String, default: T): T {
        val name = p.getString(key, null) ?: return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    private fun saveString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    fun updateAutoCheck(value: Boolean) {
        autoCheckUpdates = value
        prefs?.edit()?.putBoolean("auto_check_updates", value)?.apply()
    }

    fun updateRepo(value: String) {
        repo = value
        saveString("repo", value)
    }

    fun updateAccent(value: AccentChoice) {
        accent = value
        saveString("accent", value.name)
    }

    fun updateTextSize(value: TextSize) {
        textSize = value
        saveString("text_size", value.name)
    }

    fun updateLayoutMode(value: LayoutMode) {
        layoutMode = value
        saveString("layout_mode", value.name)
    }

    fun updateTvMargin(value: TvMargin) {
        tvMargin = value
        saveString("tv_margin", value.name)
    }

    fun updateStartScreen(value: StartScreen) {
        startScreen = value
        saveString("start_screen", value.name)
    }

    fun updatePrimaryLang(v: Lang) { primaryLang = v; saveString("primary_lang", v.name) }
    fun updateSecondaryLang(v: Lang) { secondaryLang = v; saveString("secondary_lang", v.name) }
    fun updateQuality(v: QualityPref) { quality = v; saveString("quality", v.name) }
    fun updateAutoPick(v: Boolean) { autoPick = v; saveBool("auto_pick", v) }
    fun updateAutoPlayOnOpen(v: Boolean) { autoPlayOnOpen = v; saveBool("auto_play_open", v) }
    fun updateResume(v: Boolean) { resume = v; saveBool("resume", v) }
    fun updateAspect(v: AspectChoice) { aspect = v; saveString("aspect", v.name) }
    fun updateSeekStep(v: SeekStep) { seekStep = v; saveString("seek_step", v.name) }
    fun updatePreferAudioLang(v: Boolean) { preferAudioLang = v; saveBool("prefer_audio_lang", v) }
    fun updateSubsOn(v: Boolean) { subsOn = v; saveBool("subs_on", v) }
    fun updateSubLang(v: Lang) { subLang = v; saveString("sub_lang", v.name) }
    fun updateSubSize(v: SubSize) { subSize = v; saveString("sub_size", v.name) }
    fun updateBuffer(v: BufferChoice) { buffer = v; saveString("buffer", v.name) }
    fun updateTimeout(v: TimeoutChoice) { timeout = v; saveString("timeout", v.name) }
    fun updateUserAgent(v: String) { userAgent = v; saveString("user_agent", v) }
    fun updateHideAdult(v: Boolean) { hideAdult = v; saveBool("hide_adult", v) }
    fun updatePin(v: String) { pin = v; saveString("pin", v) }

    fun resetAll() {
        updatePrimaryLang(Lang.Hindi)
        updateSecondaryLang(Lang.English)
        updateQuality(QualityPref.Q1080)
        updateAutoPick(true)
        updateAutoPlayOnOpen(false)
        updateResume(true)
        updateAspect(AspectChoice.Fit)
        updateSeekStep(SeekStep.S10)
        updatePreferAudioLang(true)
        updateSubsOn(false)
        updateSubLang(Lang.English)
        updateSubSize(SubSize.Normal)
        updateBuffer(BufferChoice.Normal)
        updateTimeout(TimeoutChoice.Normal)
        updateUserAgent(DEFAULT_UA)
        // The PIN and the adult filter are not touched by "Reset settings" on purpose.
        updateAutoCheck(true)
        updateRepo(DEFAULT_REPO)
        updateAccent(AccentChoice.Orange)
        updateTextSize(TextSize.Normal)
        updateLayoutMode(LayoutMode.Auto)
        updateTvMargin(TvMargin.Normal)
        updateStartScreen(StartScreen.Home)
    }
}

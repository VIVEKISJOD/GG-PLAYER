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

/** All app settings. Values are Compose state, so screens update at once when one changes. */
object AppSettings {
    const val DEFAULT_REPO = "VIVEKISJOD/GG-PLAYER"

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

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("gg_settings", Context.MODE_PRIVATE)
        prefs = p
        autoCheckUpdates = p.getBoolean("auto_check_updates", true)
        repo = p.getString("repo", DEFAULT_REPO) ?: DEFAULT_REPO
        accent = readEnum(p, "accent", AccentChoice.Orange)
        textSize = readEnum(p, "text_size", TextSize.Normal)
        layoutMode = readEnum(p, "layout_mode", LayoutMode.Auto)
        tvMargin = readEnum(p, "tv_margin", TvMargin.Normal)
        startScreen = readEnum(p, "start_screen", StartScreen.Home)
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

    fun resetAll() {
        updateAutoCheck(true)
        updateRepo(DEFAULT_REPO)
        updateAccent(AccentChoice.Orange)
        updateTextSize(TextSize.Normal)
        updateLayoutMode(LayoutMode.Auto)
        updateTvMargin(TvMargin.Normal)
        updateStartScreen(StartScreen.Home)
    }
}

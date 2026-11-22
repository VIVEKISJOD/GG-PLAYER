package com.ggplayer.app.data

/**
 * Languages the user can pick as first and second choice. [tokens] are lower case words that
 * mark this language inside a stream name (checked after punctuation is turned into spaces).
 */
enum class Lang(val label: String, val iso: String, val tokens: Set<String>) {
    None("None", "", emptySet()),
    Hindi("Hindi", "hi", setOf("hindi", "hin", "hind")),
    English("English", "en", setOf("english", "eng")),
    Tamil("Tamil", "ta", setOf("tamil", "tam")),
    Telugu("Telugu", "te", setOf("telugu", "tel")),
    Malayalam("Malayalam", "ml", setOf("malayalam")),
    Kannada("Kannada", "kn", setOf("kannada")),
    Bengali("Bengali", "bn", setOf("bengali")),
    Punjabi("Punjabi", "pa", setOf("punjabi")),
    Marathi("Marathi", "mr", setOf("marathi")),
    Spanish("Spanish", "es", setOf("spanish", "latino", "castellano")),
    French("French", "fr", setOf("french", "vff", "truefrench")),
    German("German", "de", setOf("german", "deutsch")),
    Japanese("Japanese", "ja", setOf("japanese", "jpn")),
    Korean("Korean", "ko", setOf("korean", "kor")),
}

/** Languages that can be a real choice (everything except None). */
val realLanguages: List<Lang> = Lang.values().filter { it != Lang.None }

/** Preferred video quality. [res] is the picture height in pixels. */
enum class QualityPref(val label: String, val res: Int) {
    Q2160("4K", 2160),
    Q1080("1080p", 1080),
    Q720("720p", 720),
    Q480("480p", 480),
}

/** Aspect ratio modes. [mode] matches the Media3 resize modes (fit 0, fill 3, zoom 4). */
enum class AspectChoice(val label: String, val mode: Int) {
    Fit("Fit", 0),
    Fill("Stretch", 3),
    Zoom("Zoom", 4),
}

enum class BufferChoice(val label: String, val minMs: Int, val maxMs: Int) {
    Small("Small (15 s)", 15_000, 30_000),
    Normal("Normal (30 s)", 30_000, 60_000),
    Large("Large (60 s)", 60_000, 120_000),
}

enum class TimeoutChoice(val label: String, val ms: Int) {
    Short("8 seconds", 8_000),
    Normal("15 seconds", 15_000),
    Long("30 seconds", 30_000),
}

enum class SubSize(val label: String, val fraction: Float) {
    Small("Small", 0.04f),
    Normal("Normal", 0.0533f),
    Large("Large", 0.07f),
}

enum class SeekStep(val label: String, val seconds: Int) {
    S10("10 seconds", 10),
    S20("20 seconds", 20),
    S30("30 seconds", 30),
}

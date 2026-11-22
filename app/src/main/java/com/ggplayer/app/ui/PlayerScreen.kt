package com.ggplayer.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.data.Channel
import com.ggplayer.app.data.EpgStore
import com.ggplayer.app.data.ResumeStore
import com.ggplayer.app.data.StreamOption
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the player should play. Set this, then open Route.Player. */
class PlayRequest(
    val title: String,
    val subtitle: String,
    val url: String,
    val headers: Map<String, String>,
    val live: Boolean,
    val resumeKey: String?,
    val channels: List<Channel>?,
    val index: Int,
    val altUrl: String,
)

object PlayerHost {
    var request: PlayRequest? = null

    fun forStream(title: String, opt: StreamOption, resumeKey: String): PlayRequest? {
        val u = opt.url ?: return null
        return PlayRequest(
            title = title,
            subtitle = "${opt.qualityLabel()}  -  ${opt.addonName}",
            url = u,
            headers = opt.headers,
            live = false,
            resumeKey = resumeKey,
            channels = null,
            index = 0,
            altUrl = "",
        )
    }

    fun forChannels(list: List<Channel>, index: Int): PlayRequest {
        val c = list[index]
        return PlayRequest(
            title = c.name,
            subtitle = c.group,
            url = c.url,
            headers = emptyMap(),
            live = true,
            resumeKey = null,
            channels = list,
            index = index,
            altUrl = c.altUrl,
        )
    }
}

private fun findActivity(context: Context): Activity? {
    var c: Context? = context
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private class Sheet(val title: String, val options: List<Pair<String, () -> Unit>>)

private fun fmtTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

private fun trackLabel(f: Format, index: Int): String {
    val lang = f.language
    val name = if (!lang.isNullOrBlank() && lang != "und") Locale.forLanguageTag(lang).displayLanguage else ""
    val label = f.label ?: ""
    val parts = listOf(label, name).filter { it.isNotBlank() }.distinct()
    val base = if (parts.isEmpty()) "Track ${index + 1}" else parts.joinToString(" ")
    return if (f.channelCount > 0 && f.sampleMimeType?.startsWith("audio") == true) "$base (${f.channelCount} ch)" else base
}

@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val req = PlayerHost.request
    if (req == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    PlayerContent(req = req, onBack = onBack)
}

@Composable
private fun PlayerContent(req: PlayRequest, onBack: () -> Unit) {
    val tv = LocalIsTv.current
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { findActivity(context) }

    var channelIndex by remember { mutableStateOf(req.index) }
    val channel: Channel? = req.channels?.getOrNull(channelIndex)
    val baseUrl = channel?.url ?: req.url
    val title = channel?.name ?: req.title
    val subtitleLine = channel?.group ?: req.subtitle

    var triedAlt by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(baseUrl) }
    LaunchedEffect(channelIndex) {
        triedAlt = false
        currentUrl = baseUrl
    }
    var subtitleUrl by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableStateOf(0) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var subInput by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var speed by remember { mutableStateOf(1f) }
    var aspect by remember { mutableStateOf(AppSettings.aspect) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var hint by remember { mutableStateOf<String?>(null) }

    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }

    // The player is built once. It reads the user agent, timeouts and buffer from Settings.
    val exo = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(AppSettings.userAgent.ifBlank { AppSettings.DEFAULT_UA })
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(AppSettings.timeout.ms)
            .setReadTimeoutMs(AppSettings.timeout.ms)
            .setDefaultRequestProperties(req.headers)
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(AppSettings.buffer.minMs, AppSettings.buffer.maxMs, 2500, 5000)
            .build()
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http)))
            .setLoadControl(load)
            .build()
        val params = p.trackSelectionParameters.buildUpon()
        if (AppSettings.preferAudioLang && AppSettings.primaryLang.iso.isNotEmpty()) {
            params.setPreferredAudioLanguage(AppSettings.primaryLang.iso)
        }
        if (AppSettings.subsOn && AppSettings.subLang.iso.isNotEmpty()) {
            params.setPreferredTextLanguage(AppSettings.subLang.iso)
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        }
        p.trackSelectionParameters = params.build()
        p
    }

    val resumeKey = if (!req.live && AppSettings.resume) req.resumeKey else null
    var resumed by remember { mutableStateOf(false) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    errorText = null
                    if (!resumed && resumeKey != null) {
                        resumed = true
                        val saved = ResumeStore.get(resumeKey)
                        if (saved > 15_000 && exo.duration > 0 && saved < exo.duration - 30_000) {
                            exo.seekTo(saved)
                            hint = "Continuing from ${fmtTime(saved)}"
                        }
                    }
                }
                if (state == Player.STATE_ENDED && resumeKey != null) ResumeStore.clear(resumeKey)
            }

            override fun onTracksChanged(t: Tracks) {
                tracks = t
            }

            override fun onPlayerError(error: PlaybackException) {
                val alt = req.channels?.getOrNull(channelIndex)?.altUrl ?: req.altUrl
                if (alt.isNotEmpty() && !triedAlt) {
                    triedAlt = true
                    currentUrl = alt
                } else {
                    errorText = "Could not play this. ${error.errorCodeName}"
                }
            }
        }
        exo.addListener(listener)
        onDispose {
            if (resumeKey != null && exo.duration > 0) {
                val pos = exo.currentPosition
                if (pos > 15_000 && pos < exo.duration - 30_000) ResumeStore.put(resumeKey, pos)
            }
            exo.removeListener(listener)
            exo.release()
        }
    }

    // Load (or reload) the media when the address, subtitle or retry counter changes.
    LaunchedEffect(currentUrl, subtitleUrl, retryKey) {
        val builder = MediaItem.Builder().setUri(Uri.parse(currentUrl))
        val sub = subtitleUrl
        if (sub != null) {
            val mime = if (sub.lowercase().contains(".vtt")) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub))
                        .setMimeType(mime)
                        .setLanguage("und")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        val keep = if (sub != null) exo.currentPosition else 0L
        errorText = null
        exo.setMediaItem(builder.build(), keep)
        exo.prepare()
        exo.playWhenReady = true
    }

    LaunchedEffect(speed) { exo.setPlaybackSpeed(speed) }

    // Position, duration and the resume position are refreshed twice a second.
    LaunchedEffect(exo) {
        var tick = 0
        while (true) {
            position = exo.currentPosition
            duration = if (exo.duration > 0) exo.duration else 0L
            tick++
            if (tick % 20 == 0 && resumeKey != null && position > 15_000 && duration > 0 && position < duration - 30_000) {
                ResumeStore.put(resumeKey, position)
            }
            delay(500)
        }
    }

    // Hide the controls after a few quiet seconds.
    LaunchedEffect(controlsVisible, interaction, sheet, subInput) {
        if (controlsVisible && sheet == null && subInput == null) {
            delay(5000)
            controlsVisible = false
        }
    }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(2500)
            hint = null
        }
    }
    LaunchedEffect(controlsVisible, sheet, subInput) {
        runCatching {
            if (sheet == null && subInput == null) {
                if (controlsVisible) playFocus.requestFocus() else rootFocus.requestFocus()
            }
        }
    }

    // Fullscreen, screen stays on, phones turn to landscape.
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (!tv) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
            activity?.requestedOrientation = previous
        }
    }

    BackHandler {
        if (sheet != null) sheet = null
        else if (subInput != null) subInput = null
        else onBack()
    }

    fun poke() {
        interaction++
    }

    fun seekBy(deltaMs: Long) {
        val target = (exo.currentPosition + deltaMs).coerceAtLeast(0L)
        exo.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
        hint = (if (deltaMs > 0) "+" else "-") + (Math.abs(deltaMs) / 1000) + " s"
    }

    fun togglePlay() {
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    fun changeChannel(delta: Int) {
        val list = req.channels ?: return
        if (list.isEmpty()) return
        channelIndex = (channelIndex + delta + list.size) % list.size
        hint = list[channelIndex].name
    }

    val step = AppSettings.seekStep.seconds * 1000L

    fun openTrackSheet(type: Int, name: String) {
        val opts = ArrayList<Pair<String, () -> Unit>>()
        fun addOption(label: String, action: () -> Unit) {
            opts.add(Pair(label, action))
        }
        addOption("Automatic") {
            val b = exo.trackSelectionParameters.buildUpon().clearOverridesOfType(type)
            if (type == C.TRACK_TYPE_TEXT) b.setTrackTypeDisabled(type, false)
            exo.trackSelectionParameters = b.build()
        }
        if (type == C.TRACK_TYPE_TEXT) {
            addOption("Off") {
                exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
            }
        }
        for (group in tracks.groups) {
            if (group.type != type) continue
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val label = trackLabel(group.getTrackFormat(i), opts.size)
                addOption(label) {
                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(type, false)
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                        .build()
                }
            }
        }
        sheet = Sheet(name, opts)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                poke()
                if (sheet != null || subInput != null || controlsVisible) {
                    // Controls are on screen, so the arrow keys move between the buttons.
                    return@onPreviewKeyEvent when (e.key) {
                        Key.MediaPlayPause -> { togglePlay(); true }
                        Key.ChannelUp -> { changeChannel(1); true }
                        Key.ChannelDown -> { changeChannel(-1); true }
                        else -> false
                    }
                }
                when (e.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause, Key.Spacebar -> {
                        togglePlay(); controlsVisible = true; true
                    }
                    Key.MediaPlay -> { exo.play(); true }
                    Key.MediaPause -> { exo.pause(); true }
                    Key.DirectionLeft, Key.MediaRewind -> {
                        if (req.live) controlsVisible = true else seekBy(-step)
                        true
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        if (req.live) controlsVisible = true else seekBy(step)
                        true
                    }
                    Key.DirectionUp, Key.ChannelUp -> {
                        if (req.live && req.channels != null) changeChannel(1) else controlsVisible = true
                        true
                    }
                    Key.DirectionDown, Key.ChannelDown -> {
                        if (req.live && req.channels != null) changeChannel(-1) else controlsVisible = true
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsVisible = !controlsVisible },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = exo
                    isFocusable = false
                    isFocusableInTouchMode = false
                    resizeMode = aspect.mode
                    subtitleView?.setFractionalTextSize(AppSettings.subSize.fraction)
                }
            },
            update = { it.resizeMode = aspect.mode },
        )

        if (buffering && errorText == null) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val h = hint
        if (h != null) {
            Text(
                text = h,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        val err = errorText
        if (err != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6000000))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = err, style = MaterialTheme.typography.bodyLarge.copy(color = GG.danger))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GGButton(text = "Try again", onClick = { retryKey++ })
                    GGButton(text = "Back", primary = false, onClick = onBack)
                }
            }
        }

        if (controlsVisible && sheet == null && subInput == null) {
            // Top: what is playing
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(horizontal = if (tv) 48.dp else 20.dp, vertical = 14.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                if (subtitleLine.isNotBlank()) {
                    Text(text = subtitleLine, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                if (req.live && channel != null) {
                    val now = EpgStore.now(channel)
                    val next = EpgStore.next(channel)
                    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                    if (now != null) {
                        Text(
                            text = "Now: ${now.title}  (${fmt.format(Date(now.start))} - ${fmt.format(Date(now.stop))})",
                            style = MaterialTheme.typography.bodyMedium.copy(color = GG.text),
                            maxLines = 1,
                        )
                    }
                    if (next != null) {
                        Text(
                            text = "Next: ${next.title}  (${fmt.format(Date(next.start))})",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Bottom: seek bar and buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xB3000000))
                    .padding(horizontal = if (tv) 48.dp else 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!req.live && duration > 0) {
                    var barWidth by remember { mutableStateOf(1) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = fmtTime(position), style = MaterialTheme.typography.labelLarge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .onSizeChanged { barWidth = it.width.coerceAtLeast(1) }
                                .pointerInput(duration) {
                                    detectTapGestures { offset ->
                                        poke()
                                        exo.seekTo((offset.x / barWidth * duration).toLong())
                                    }
                                },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(GG.line),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((position.toFloat() / duration).coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(GG.accent),
                            )
                        }
                        Text(text = fmtTime(duration), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GGButton(text = "Back", primary = false, icon = Icons.Default.ArrowBack, onClick = onBack)
                    GGButton(
                        text = if (playing) "Pause" else "Play",
                        icon = if (playing) null else Icons.Default.PlayArrow,
                        onClick = { togglePlay(); poke() },
                        modifier = Modifier.focusRequester(playFocus),
                    )
                    if (req.live && req.channels != null) {
                        GGButton(text = "Channel -", primary = false, onClick = { changeChannel(-1); poke() })
                        GGButton(text = "Channel +", primary = false, onClick = { changeChannel(1); poke() })
                    } else {
                        GGButton(text = "-${AppSettings.seekStep.seconds}s", primary = false, onClick = { seekBy(-step); poke() })
                        GGButton(text = "+${AppSettings.seekStep.seconds}s", primary = false, onClick = { seekBy(step); poke() })
                    }
                    GGButton(text = "Audio", primary = false, onClick = { openTrackSheet(C.TRACK_TYPE_AUDIO, "Audio track") })
                    GGButton(text = "Subtitles", primary = false, onClick = { openTrackSheet(C.TRACK_TYPE_TEXT, "Subtitles") })
                    GGButton(text = "Subtitle link", primary = false, onClick = { subInput = "" })
                    if (!req.live) {
                        GGButton(
                            text = "Speed ${speedLabel(speed)}",
                            primary = false,
                            onClick = { speed = nextSpeed(speed); poke() },
                        )
                    }
                    GGButton(
                        text = "Aspect: ${aspect.label}",
                        primary = false,
                        onClick = {
                            aspect = com.ggplayer.app.data.AspectChoice.values().let { all ->
                                all[(all.indexOf(aspect) + 1) % all.size]
                            }
                            poke()
                        },
                    )
                }
            }
        }

        val s = sheet
        if (s != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .width(if (tv) 520.dp else 320.dp)
                        .fillMaxHeight(0.8f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GG.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = s.title, style = MaterialTheme.typography.titleLarge)
                    val firstFocus = remember { FocusRequester() }
                    s.options.forEachIndexed { i, opt ->
                        FocusSurface(
                            onClick = {
                                opt.second()
                                sheet = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (i == 0) Modifier.focusRequester(firstFocus) else Modifier),
                        ) {
                            Text(
                                text = opt.first,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                    if (s.options.size <= 1) {
                        Text(
                            text = "This video has no extra tracks.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
                }
            }
        }

        val si = subInput
        if (si != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .width(if (tv) 620.dp else 340.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GG.surface)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "External subtitle", style = MaterialTheme.typography.titleLarge)
                    GGField(
                        label = "Link to a .srt or .vtt file",
                        value = si,
                        onValueChange = { subInput = it },
                        placeholder = "https://example.com/subtitle.srt",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GGButton(
                            text = "Load",
                            onClick = {
                                val link = si.trim()
                                if (link.startsWith("http", true)) {
                                    subtitleUrl = link
                                    subInput = null
                                }
                            },
                        )
                        GGButton(text = "Cancel", primary = false, onClick = { subInput = null })
                    }
                }
            }
        }
    }
}

private val speedSteps = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun nextSpeed(current: Float): Float {
    val i = speedSteps.indexOfFirst { it >= current - 0.001f }
    return speedSteps[(i + 1).coerceAtLeast(0) % speedSteps.size]
}

private fun speedLabel(s: Float): String = if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"

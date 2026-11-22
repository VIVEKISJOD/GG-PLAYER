package com.ggplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.data.Episode
import com.ggplayer.app.data.MetaDetail
import com.ggplayer.app.data.MetaPreview
import com.ggplayer.app.data.StreamOption
import com.ggplayer.app.data.StreamResolver
import com.ggplayer.app.data.StremioApi

/** Details page of a movie or series. It already looks for the best source, so Play is one tap. */
@Composable
fun TitleScreen(meta: MetaPreview, autoPlay: Boolean, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val tv = LocalIsTv.current
    val isSeries = meta.type == "series"
    var detail by remember { mutableStateOf<MetaDetail?>(null) }
    var selectedVideo by remember { mutableStateOf<String?>(null) }
    var season by remember { mutableStateOf(1) }
    var streams by remember { mutableStateOf<List<StreamOption>?>(null) }
    var played by remember { mutableStateOf(false) }
    val playFocus = remember { FocusRequester() }

    val info = detail?.preview ?: meta
    val episodes: List<Episode> = detail?.episodes ?: emptyList()

    LaunchedEffect(meta.id) { detail = StremioApi.fetchMeta(meta.type, meta.id) }

    LaunchedEffect(detail) {
        if (isSeries && selectedVideo == null) {
            val first = episodes.firstOrNull { it.season > 0 } ?: episodes.firstOrNull()
            if (first != null) {
                selectedVideo = first.id
                season = first.season
            }
        }
    }

    val videoId: String? = if (isSeries) selectedVideo else meta.id
    val episode = episodes.firstOrNull { it.id == videoId }
    val displayTitle = if (isSeries && episode != null) {
        "${info.name}  S${episode.season}E${episode.number}"
    } else {
        info.name
    }

    LaunchedEffect(videoId) {
        if (videoId != null) {
            streams = null
            streams = StreamResolver.resolve(meta.type, videoId)
        }
    }

    fun play(opt: StreamOption) {
        val id = videoId ?: return
        val request = PlayerHost.forStream(displayTitle, opt, "vod:$id") ?: return
        PlayerHost.request = request
        onOpen(Route.Player)
    }

    fun chooseSource() {
        val id = videoId ?: return
        onOpen(Route.Sources(meta.type, id, displayTitle))
    }

    val list = streams
    val best = list?.firstOrNull { it.playable }

    LaunchedEffect(list) {
        if (list != null && !played && (autoPlay || AppSettings.autoPlayOnOpen) && AppSettings.autoPick) {
            val b = list.firstOrNull { it.playable }
            if (b != null) {
                played = true
                play(b)
            }
        }
    }
    LaunchedEffect(best) {
        if (best != null) runCatching { playFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SubHeader(title = "Details", onBack = onBack)

        val image = info.background ?: info.poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tv) 300.dp else 200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GG.surface),
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0x000B0B0E), Color(0xF20B0B0E)))),
            )
            Text(
                text = info.name,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            )
        }

        val metaLine = listOf(
            info.year,
            if (info.rating.isNotBlank()) "IMDb ${info.rating}" else "",
            info.genres.take(3).joinToString(", "),
        ).filter { it.isNotBlank() }.joinToString("  -  ")
        if (metaLine.isNotEmpty()) Text(text = metaLine, style = MaterialTheme.typography.bodyMedium)
        if (info.description.isNotBlank()) {
            Text(text = info.description, style = MaterialTheme.typography.bodyLarge, maxLines = 6)
        }

        // The pick
        InfoCard {
            Text(
                text = if (isSeries && episode != null) "Best source for ${displayTitle}" else "Best source",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Your choice: ${AppSettings.primaryLang.label}" +
                    (if (AppSettings.secondaryLang.iso.isNotEmpty()) ", then ${AppSettings.secondaryLang.label}" else "") +
                    ", ${AppSettings.quality.label}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (isSeries && videoId == null) {
                Text(
                    text = if (detail == null) "Loading episodes..." else "No episodes found for this series.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (list == null) {
                Text(text = "Looking for the best source...", style = MaterialTheme.typography.bodyMedium)
            } else if (best != null) {
                StreamBadges(s = best, best = true, modifier = Modifier.horizontalScroll(rememberScrollState()))
            } else if (list.isEmpty()) {
                Text(
                    text = "No source found. Add a stream addon in Settings, Movie and series sources.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GG.danger),
                )
            } else {
                Text(
                    text = "Sources were found, but none can play directly (torrent only). A debrid addon is needed.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GG.danger),
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GGButton(
                    text = "Play",
                    icon = Icons.Default.PlayArrow,
                    enabled = best != null,
                    modifier = Modifier.focusRequester(playFocus),
                    onClick = {
                        val b = best
                        if (b != null) {
                            if (AppSettings.autoPick) play(b) else chooseSource()
                        }
                    },
                )
                GGButton(
                    text = "Choose source",
                    primary = false,
                    enabled = videoId != null,
                    onClick = { chooseSource() },
                )
            }
        }

        if (isSeries && episodes.isNotEmpty()) {
            val seasons = episodes.map { it.season }.distinct().sorted()
            Text(text = "Episodes", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                seasons.forEach { n ->
                    Chip(
                        text = if (n == 0) "Specials" else "Season $n",
                        selected = n == season,
                        onClick = { season = n },
                    )
                }
            }
            episodes.filter { it.season == season }.forEach { ep ->
                val selected = ep.id == selectedVideo
                FocusSurface(
                    onClick = { selectedVideo = ep.id },
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (selected) GG.accent else GG.line,
                    focusScale = 1.015f,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (ep.thumbnail != null) {
                            AsyncImage(
                                model = ep.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(if (tv) 150.dp else 100.dp)
                                    .height(if (tv) 84.dp else 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GG.surfaceHigh),
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${ep.number}. ${ep.title.ifBlank { "Episode ${ep.number}" }}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (selected) GG.accent else GG.text,
                                ),
                                maxLines = 2,
                            )
                            if (ep.overview.isNotBlank()) {
                                Text(text = ep.overview, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            }
                        }
                        if (selected) StatusTag(text = "Selected", color = GG.accent)
                    }
                }
            }
        }
    }
}

/** All sources for one movie or episode, each with language, quality and source badges. */
@Composable
fun SourcesScreen(type: String, videoId: String, title: String, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    var streams by remember { mutableStateOf<List<StreamOption>?>(null) }
    var note by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        streams = null
        streams = StreamResolver.resolve(type, videoId)
    }

    Column(modifier = Modifier.fillMaxSize().then(pagePadding()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubHeader(title = "Choose source", onBack = onBack)
        Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        val list = streams
        if (list == null) {
            Text(text = "Looking for sources...", style = MaterialTheme.typography.bodyMedium)
        } else if (list.isEmpty()) {
            Text(
                text = "No source found. Add a stream addon in Settings, Movie and series sources.",
                style = MaterialTheme.typography.bodyLarge.copy(color = GG.danger),
            )
        }
        val n = note
        if (n != null) Text(text = n, style = MaterialTheme.typography.bodyMedium.copy(color = GG.accent))
        if (list != null && list.isNotEmpty()) {
            val firstPlayable = list.indexOfFirst { it.playable }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(list) { index, s ->
                    FocusSurface(
                        onClick = {
                            val request = PlayerHost.forStream(title, s, "vod:$videoId")
                            if (request != null) {
                                PlayerHost.request = request
                                onOpen(Route.Player)
                            } else {
                                note = "This is a torrent. It cannot play without a debrid addon that gives a direct link."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        focusScale = 1.015f,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(text = s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                            StreamBadges(
                                s = s,
                                best = index == firstPlayable,
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
            }
        }
    }
}

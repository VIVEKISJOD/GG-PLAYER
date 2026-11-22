package com.ggplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ggplayer.app.data.Channel
import com.ggplayer.app.data.ChannelRepo
import com.ggplayer.app.data.EpgStore
import com.ggplayer.app.data.FavoriteStore
import com.ggplayer.app.data.Parental
import com.ggplayer.app.data.ProviderStore
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FAV_GROUP = "__favorites__"

/** Live TV: channel list with groups, search, favourites and now/next. */
@Composable
fun LiveTvScreen(onOpen: (Route) -> Unit) {
    val providers = ProviderStore.providers
    if (providers.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).then(pagePadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader(title = "Live TV", subtitle = "Channels from your IPTV provider.")
            InfoCard {
                Text(
                    text = "No IPTV provider yet. Add an M3U link or an Xtream login.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                GGButton(
                    text = "Add provider",
                    icon = Icons.Default.Add,
                    onClick = { onOpen(Route.ProviderForm(null)) },
                )
            }
        }
    } else {
        LiveContent(onOpen = onOpen)
    }
}

@Composable
private fun LiveContent(onOpen: (Route) -> Unit) {
    val context = LocalContext.current
    val providers = ProviderStore.providers
    var providerId by remember { mutableStateOf(providers.first().id) }
    val provider = providers.firstOrNull { it.id == providerId } ?: providers.first()

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var group by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(provider.id, reloadKey) {
        loading = true
        error = null
        try {
            channels = ChannelRepo.load(context, provider, requested = reloadKey > 0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            channels = emptyList()
            error = e.message ?: "Could not load channels"
        }
        loading = false
    }
    // The TV guide loads in the background. Channels do not wait for it.
    LaunchedEffect(provider.id, reloadKey) {
        EpgStore.load(provider, force = reloadKey > 0)
    }

    val hideAdult = com.ggplayer.app.data.AppSettings.hideAdult
    val visible = remember(channels, hideAdult) { Parental.filter(channels) }
    val groups = remember(visible) { visible.map { it.group }.distinct().sorted() }
    val favs = FavoriteStore.ids
    val shown = remember(visible, group, query, favs) {
        visible.filter { c ->
            val inGroup = when (group) {
                "" -> true
                FAV_GROUP -> favs.contains(c.id)
                else -> c.group == group
            }
            inGroup && (query.isBlank() || c.name.contains(query.trim(), ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "Live TV",
            subtitle = "${provider.name}  -  ${visible.size} channels",
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (providers.size > 1) {
                GGButton(
                    text = "Provider: ${provider.name}",
                    primary = false,
                    onClick = {
                        val i = providers.indexOfFirst { it.id == provider.id }
                        providerId = providers[(i + 1) % providers.size].id
                        group = ""
                        query = ""
                    },
                )
            }
            GGButton(text = "TV guide", primary = false, onClick = { onOpen(Route.Guide(provider.id)) })
            GGButton(
                text = "Reload",
                primary = false,
                icon = Icons.Default.Refresh,
                onClick = { reloadKey++ },
            )
        }

        if (loading) Text(text = "Loading channels...", style = MaterialTheme.typography.bodyMedium)
        val err = error
        if (err != null) Text(text = err, style = MaterialTheme.typography.bodyLarge.copy(color = GG.danger))
        if (EpgStore.loading) {
            Text(text = "Loading TV guide...", style = MaterialTheme.typography.bodyMedium)
        } else if (EpgStore.message.isNotEmpty() && !loading && err == null) {
            Text(text = EpgStore.message, style = MaterialTheme.typography.bodyMedium)
        }

        if (visible.isNotEmpty()) {
            GGField(label = "Search channels", value = query, onValueChange = { query = it }, placeholder = "Channel name")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(text = "All", selected = group == "", onClick = { group = "" }) }
                item {
                    Chip(
                        text = "Favorites",
                        selected = group == FAV_GROUP,
                        onClick = { group = FAV_GROUP },
                    )
                }
                itemsIndexed(groups) { _, g ->
                    Chip(text = g, selected = group == g, onClick = { group = g })
                }
            }
            if (shown.isEmpty()) {
                Text(
                    text = if (group == FAV_GROUP) "No favorites yet. Tap the heart on a channel." else "No channel matches.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(shown) { index, ch ->
                    ChannelRow(
                        channel = ch,
                        onPlay = {
                            PlayerHost.request = PlayerHost.forChannels(shown, index)
                            onOpen(Route.Player)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onPlay: () -> Unit) {
    val fav = FavoriteStore.ids.contains(channel.id)
    // Reading the version makes this row refresh when the guide finishes loading.
    val guideVersion = EpgStore.version
    val now = if (guideVersion >= 0) EpgStore.now(channel) else null
    val next = if (guideVersion >= 0) EpgStore.next(channel) else null
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FocusSurface(onClick = onPlay, modifier = Modifier.weight(1f), focusScale = 1.015f) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GG.surfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    if (channel.logo.startsWith("http")) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = channel.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    if (now != null) {
                        Text(
                            text = "Now: ${now.title}" + (if (next != null) "   Next: ${next.title}" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    } else {
                        Text(text = channel.group, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
        }
        FocusSurface(onClick = { FavoriteStore.toggle(channel.id) }, shape = RoundedCornerShape(50)) {
            Icon(
                imageVector = if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                tint = if (fav) GG.accent else GG.textDim,
                modifier = Modifier.padding(12.dp).size(22.dp),
            )
        }
    }
}

/** Full TV guide: channels down the side, the next hours across, all rows scroll together. */
@Composable
fun GuideScreen(providerId: String, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val context = LocalContext.current
    val tv = LocalIsTv.current
    val provider = ProviderStore.get(providerId)
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var failed by remember { mutableStateOf<String?>(null) }
    val hScroll = rememberScrollState()

    LaunchedEffect(providerId) {
        if (provider != null) {
            try {
                channels = ChannelRepo.load(context, provider, requested = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = e.message ?: "Could not load channels"
            }
            EpgStore.load(provider)
        }
    }

    val guideVersion = EpgStore.version
    val hours = 6
    val dpPerMin = if (tv) 5f else 4f
    val windowStart = remember { (System.currentTimeMillis() / 1_800_000L) * 1_800_000L }
    val windowEnd = windowStart + hours * 3_600_000L
    val favs = FavoriteStore.ids

    val rows: List<Channel> = remember(channels, guideVersion, favs) {
        val withGuide = Parental.filter(channels).filter { c ->
            EpgStore.programmes(c).any { it.stop > windowStart && it.start < windowEnd }
        }
        withGuide.sortedBy { if (favs.contains(it.id)) 0 else 1 }.take(200)
    }

    Column(
        modifier = Modifier.fillMaxSize().then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SubHeader(title = "TV guide", onBack = onBack)
        if (EpgStore.loading) Text(text = "Loading TV guide...", style = MaterialTheme.typography.bodyMedium)
        val f = failed
        if (f != null) Text(text = f, style = MaterialTheme.typography.bodyLarge.copy(color = GG.danger))
        if (!EpgStore.loading && rows.isEmpty() && f == null) {
            Text(
                text = EpgStore.message.ifEmpty { "No guide data matches your channels. Check the guide link in the provider settings." },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (rows.isNotEmpty()) {
            val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            val nameWidth = if (tv) 200.dp else 110.dp
            // Time ruler
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(nameWidth))
                Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
                    for (i in 0 until hours * 2) {
                        Box(modifier = Modifier.width((30 * dpPerMin).dp)) {
                            Text(
                                text = timeFmt.format(Date(windowStart + i * 1_800_000L)),
                                style = MaterialTheme.typography.labelLarge.copy(color = GG.accent),
                            )
                        }
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(rows) { index, ch ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ch.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            modifier = Modifier.width(nameWidth).padding(end = 8.dp),
                        )
                        Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
                            var cursor = windowStart
                            for (p in EpgStore.programmes(ch)) {
                                val s = maxOf(p.start, windowStart)
                                val e = minOf(p.stop, windowEnd)
                                if (e <= s || e <= cursor) continue
                                val begin = maxOf(s, cursor)
                                if (begin > cursor) {
                                    Spacer(modifier = Modifier.width(((begin - cursor) / 60_000f * dpPerMin).dp))
                                }
                                val w = maxOf(6f, (e - begin) / 60_000f * dpPerMin - 3f)
                                FocusSurface(
                                    onClick = {
                                        PlayerHost.request = PlayerHost.forChannels(rows, index)
                                        onOpen(Route.Player)
                                    },
                                    modifier = Modifier.width(w.dp).height(if (tv) 64.dp else 54.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    focusScale = 1.0f,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                    ) {
                                        Text(
                                            text = p.title.ifBlank { "Programme" },
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = timeFmt.format(Date(p.start)),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                cursor = e
                            }
                        }
                    }
                }
            }
        }
    }
}

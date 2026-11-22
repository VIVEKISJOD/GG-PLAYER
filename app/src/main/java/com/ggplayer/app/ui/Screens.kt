package com.ggplayer.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ggplayer.app.BuildConfig
import com.ggplayer.app.R
import com.ggplayer.app.data.AddonStore
import com.ggplayer.app.data.HomeRepo
import com.ggplayer.app.data.MetaPreview
import com.ggplayer.app.data.typeLabel
import com.ggplayer.app.update.UpdateManager
import com.ggplayer.app.update.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onNavigate: (Section) -> Unit, onOpen: (Route) -> Unit) {
    val tv = LocalIsTv.current
    val update = UpdateManager.state
    val rows = HomeRepo.rows
    val noAddons = AddonStore.addons.isEmpty()

    LaunchedEffect(AddonStore.version) { HomeRepo.load() }

    val hero: MetaPreview? = remember(rows) {
        val all = rows.flatMap { it.items }
        all.firstOrNull { it.background != null } ?: all.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(if (tv) 26.dp else 20.dp),
    ) {
        BrandBar(
            onSearch = { onNavigate(Section.Library) },
            onSettings = { onNavigate(Section.Settings) },
        )

        if (update is UpdateState.Available) {
            UpdateBanner(version = update.info.versionName, onClick = { onOpen(Route.Updates) })
        }

        if (noAddons) {
            EmptyHome(onOpen = onOpen)
        } else {
            if (hero != null) {
                HeroCard(
                    item = hero,
                    onPlay = { onOpen(Route.Title(hero, true)) },
                    onDetails = { onOpen(Route.Title(hero, false)) },
                )
            }
            if (HomeRepo.loading && rows.isEmpty()) {
                Text(text = "Loading posters...", style = MaterialTheme.typography.bodyMedium)
            }
            if (!HomeRepo.loading && rows.isEmpty()) {
                InfoCard {
                    Text(
                        text = "Your addons did not return any catalog. Check Settings, Movie and series sources.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    GGButton(text = "Open sources", primary = false, onClick = { onOpen(Route.Addons) })
                }
            }
            rows.forEach { row ->
                PosterRow(title = row.title, items = row.items, onOpen = { onOpen(Route.Title(it, false)) })
            }
        }

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}  -  ${if (tv) "TV" else "Phone or tablet"} layout",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyHome(onOpen: (Route) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    InfoCard {
        Text(text = "Add a movie source to fill this page", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Posters come from Stremio addons. Cinemeta gives posters and details. " +
                "For playing, also add a stream addon of your own in Settings.",
            style = MaterialTheme.typography.bodyMedium,
        )
        GGButton(
            text = if (busy) "Adding..." else "Add Cinemeta (posters)",
            enabled = !busy,
            icon = Icons.Default.Add,
            onClick = {
                busy = true
                scope.launch {
                    val r = AddonStore.add(AddonStore.CINEMETA)
                    busy = false
                    note = if (r.isSuccess) null else (r.exceptionOrNull()?.message ?: "Could not add")
                }
            },
        )
        GGButton(text = "Add other addon", primary = false, onClick = { onOpen(Route.Addons) })
        val n = note
        if (n != null) Text(text = n, style = MaterialTheme.typography.bodyMedium.copy(color = GG.danger))
    }
}

@Composable
private fun BrandBar(onSearch: () -> Unit, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(
            painter = painterResource(R.drawable.gg_mark),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Text(
            text = "GG Player",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 0.dp).weight(1f),
        )
        IconCircleButton(icon = Icons.Default.Search, description = "Search", onClick = onSearch)
        IconCircleButton(icon = Icons.Default.Settings, description = "Settings", onClick = onSettings)
    }
}

@Composable
private fun UpdateBanner(version: String, onClick: () -> Unit) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = GG.surfaceHigh,
        borderColor = GG.accent,
        focusScale = 1.015f,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = GG.accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = "Update available", style = MaterialTheme.typography.titleMedium)
                Text(text = "Version $version is ready to download", style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "Open", style = MaterialTheme.typography.labelLarge.copy(color = GG.accent))
        }
    }
}

@Composable
private fun HeroCard(item: MetaPreview, onPlay: () -> Unit, onDetails: () -> Unit) {
    val tv = LocalIsTv.current
    val shape = RoundedCornerShape(24.dp)
    val image = item.background ?: item.poster
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (tv) 340.dp else 280.dp)
            .clip(shape)
            .background(GG.surface)
            .border(1.dp, GG.line, shape),
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = item.name, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
            val meta = listOf(item.year, item.rating.takeIf { it.isNotBlank() }?.let { "IMDb $it" }, item.genres.take(3).joinToString(", "))
                .filter { !it.isNullOrBlank() }.joinToString("  -  ")
            if (meta.isNotEmpty()) Text(text = meta, style = MaterialTheme.typography.bodyMedium)
            if (item.description.isNotBlank()) {
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GGButton(text = "Play", icon = Icons.Default.PlayArrow, onClick = onPlay)
                GGButton(text = "Details", primary = false, onClick = onDetails)
            }
        }
    }
}

/** Movies and Series page: pick the type, browse rows or search. */
@Composable
fun LibraryScreen(onOpen: (Route) -> Unit) {
    val tv = LocalIsTv.current
    var type by remember { mutableStateOf("movie") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MetaPreview>?>(null) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(AddonStore.version) { HomeRepo.load() }
    LaunchedEffect(query, type, AddonStore.version) {
        val q = query.trim()
        if (q.length < 2) {
            results = null
            searching = false
        } else {
            delay(500)
            searching = true
            results = HomeRepo.search(q, type)
            searching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Movies and Series", subtitle = "Posters from the addons you added.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GGButton(text = "Movies", primary = type == "movie", onClick = { type = "movie" })
            GGButton(text = "Series", primary = type == "series", onClick = { type = "series" })
        }
        GGField(label = "Search", value = query, onValueChange = { query = it }, placeholder = "Type a title")

        if (AddonStore.addons.isEmpty()) {
            InfoCard {
                Text(
                    text = "No addon added yet. Add a movie source to see posters here.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                GGButton(text = "Add source", onClick = { onOpen(Route.Addons) }, icon = Icons.Default.Add)
            }
        }

        val found = results
        if (query.trim().length >= 2) {
            if (searching) Text(text = "Searching...", style = MaterialTheme.typography.bodyMedium)
            if (found != null) {
                if (found.isEmpty()) {
                    Text(
                        text = "Nothing found. Your addons may not have search for ${typeLabel(type)}.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                val cols = if (tv) 6 else 3
                found.chunked(cols).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { m ->
                            PosterCard(
                                item = m,
                                onClick = { onOpen(Route.Title(m, false)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(cols - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        } else {
            val rows = HomeRepo.rows.filter { it.type == type }
            if (HomeRepo.loading && rows.isEmpty()) {
                Text(text = "Loading posters...", style = MaterialTheme.typography.bodyMedium)
            }
            rows.forEach { row ->
                PosterRow(title = row.title, items = row.items, onOpen = { onOpen(Route.Title(it, false)) })
            }
            if (!HomeRepo.loading && rows.isEmpty() && AddonStore.addons.isNotEmpty()) {
                Text(
                    text = "No ${typeLabel(type)} catalog found in your addons.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

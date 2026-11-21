package com.ggplayer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ggplayer.app.BuildConfig

/** Shared outer padding. TVs get a wider margin so nothing sits in the overscan area. */
@Composable
private fun pagePadding(): Modifier {
    val tv = LocalIsTv.current
    return if (tv) {
        Modifier.padding(horizontal = 48.dp, vertical = 32.dp)
    } else {
        Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
    }
}

@Composable
fun HomeScreen(onNavigate: (Section) -> Unit) {
    val tv = LocalIsTv.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        ScreenHeader(
            title = "GG Player",
            subtitle = "Live TV and on-demand, from providers you add yourself.",
        )

        if (tv) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SourceCard(
                    title = "Live TV",
                    body = "Add an IPTV provider to watch live channels.",
                    status = "Not built yet - step 2",
                    onClick = { onNavigate(Section.LiveTv) },
                    modifier = Modifier.weight(1f),
                )
                SourceCard(
                    title = "Movies & Series",
                    body = "Add an addon with a manifest.json link to browse and play.",
                    status = "Not built yet - step 5",
                    onClick = { onNavigate(Section.Library) },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SourceCard(
                    title = "Live TV",
                    body = "Add an IPTV provider to watch live channels.",
                    status = "Not built yet - step 2",
                    onClick = { onNavigate(Section.LiveTv) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SourceCard(
                    title = "Movies & Series",
                    body = "Add an addon with a manifest.json link to browse and play.",
                    status = "Not built yet - step 5",
                    onClick = { onNavigate(Section.Library) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}  -  ${if (tv) "TV" else "Phone or tablet"} layout",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SourceCard(
    title: String,
    body: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            StatusTag(text = status, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun LiveTvScreen() {
    EmptySection(
        title = "Live TV",
        message = "No provider added. M3U playlists and Xtream logins will be added here.",
        status = "Not built yet - steps 2 to 4",
    )
}

@Composable
fun LibraryScreen() {
    EmptySection(
        title = "Movies & Series",
        message = "No addon added. Paste a manifest.json link here to load catalogs and streams.",
        status = "Not built yet - steps 5 and 6",
    )
}

@Composable
private fun EmptySection(title: String, message: String, status: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(title = title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GG.line, RoundedCornerShape(10.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            StatusTag(text = status)
        }
    }
}

private data class SettingsRow(val title: String, val detail: String, val status: String?)

@Composable
fun SettingsScreen() {
    val rows = listOf(
        SettingsRow("Providers and addons", "Manage IPTV providers and manifest.json addons", "Step 2 and 5"),
        SettingsRow("Playback", "Decoder, resume, auto-play next, default aspect ratio", "Step 7"),
        SettingsRow("Audio and subtitles", "Preferred languages, subtitle size and style, delay", "Step 7"),
        SettingsRow("Network and buffer", "Buffer size, user agent, connection timeout", "Step 8"),
        SettingsRow("Parental control", "PIN lock for categories and adult content", "Step 10"),
        SettingsRow("Backup and restore", "Export and import providers, addons and settings", "Step 10"),
        SettingsRow("Updates", "Check for a new version and install it", "Step 9"),
        SettingsRow("About", "Version ${BuildConfig.VERSION_NAME}, build ${BuildConfig.VERSION_CODE}", null),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(title = "Settings", modifier = Modifier.padding(bottom = 8.dp))
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GG.line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = row.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = row.detail, style = MaterialTheme.typography.bodyMedium)
                }
                if (row.status != null) {
                    StatusTag(text = "Not built - ${row.status}", modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Box(modifier = Modifier.height(8.dp))
    }
}

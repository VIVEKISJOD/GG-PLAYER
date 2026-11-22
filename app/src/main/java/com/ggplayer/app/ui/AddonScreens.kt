package com.ggplayer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ggplayer.app.data.Addon
import com.ggplayer.app.data.AddonStore
import kotlinx.coroutines.launch

/** Settings > Movie and series sources: add, check and remove Stremio addons. */
@Composable
fun AddonsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var confirmRemove by remember { mutableStateOf<String?>(null) }
    val addons = AddonStore.addons

    fun add(url: String) {
        if (url.isBlank()) {
            message = Pair(false, "Paste the manifest.json link first.")
            return
        }
        busy = true
        message = null
        scope.launch {
            val r = AddonStore.add(url)
            busy = false
            if (r.isSuccess) {
                message = Pair(true, "Added ${r.getOrNull()?.name ?: "addon"}.")
                link = ""
            } else {
                message = Pair(false, r.exceptionOrNull()?.message ?: "Could not add this addon.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SubHeader(title = "Movie and series sources", onBack = onBack)
        Text(
            text = "Add Stremio addons by their manifest.json link. Catalog addons give the posters. " +
                "Stream addons give the video links. Only direct links can play, torrent-only streams cannot.",
            style = MaterialTheme.typography.bodyMedium,
        )

        GGField(
            label = "Addon link (manifest.json)",
            value = link,
            onValueChange = { link = it },
            placeholder = "https://example.com/manifest.json",
            keyboardType = KeyboardType.Uri,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GGButton(
                text = if (busy) "Checking..." else "Add addon",
                enabled = !busy,
                icon = Icons.Default.Add,
                onClick = { add(link) },
            )
        }
        if (addons.none { it.manifestUrl == AddonStore.CINEMETA }) {
            GGButton(
                text = "Add Cinemeta (posters and details)",
                primary = false,
                enabled = !busy,
                onClick = { add(AddonStore.CINEMETA) },
            )
        }
        val m = message
        if (m != null) {
            Text(
                text = m.second,
                style = MaterialTheme.typography.bodyLarge.copy(color = if (m.first) GG.ok else GG.danger),
            )
        }

        GroupLabel("Your addons")
        if (addons.isEmpty()) {
            InfoCard {
                Text(text = "No addon added yet.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        addons.forEach { a -> AddonCard(a, confirmRemove == a.manifestUrl) { confirmRemove = if (confirmRemove == a.manifestUrl) null else a.manifestUrl } }
        val target = confirmRemove
        if (target != null) {
            GGButton(
                text = "Tap to remove this addon",
                primary = false,
                danger = true,
                icon = Icons.Default.Delete,
                onClick = {
                    AddonStore.remove(target)
                    confirmRemove = null
                },
            )
        }
    }
}

@Composable
private fun AddonCard(a: Addon, selected: Boolean, onSelect: () -> Unit) {
    FocusSurface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (selected) GG.danger else GG.line,
        focusScale = 1.015f,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = a.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (a.version.isNotBlank()) StatusTag(text = "v${a.version}")
            }
            if (a.description.isNotBlank()) {
                Text(text = a.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (a.catalogs.isNotEmpty()) Badge("Posters")
                if (a.hasResource("meta")) Badge("Details")
                if (a.hasResource("stream")) Badge("Streams", match = 1)
                if (a.hasResource("subtitles")) Badge("Subtitles")
            }
            if (selected) {
                Text(
                    text = "Selected. Use the red button below to remove it.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GG.danger),
                )
            }
        }
    }
}

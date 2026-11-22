package com.ggplayer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.ggplayer.app.data.ChannelRepo
import com.ggplayer.app.data.Provider
import com.ggplayer.app.data.ProviderStore
import com.ggplayer.app.data.ProviderType
import kotlinx.coroutines.launch

/** Settings > IPTV providers: the list of providers, with add and edit. */
@Composable
fun ProvidersListScreen(onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val providers = ProviderStore.providers
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SubHeader(title = "IPTV providers", onBack = onBack)
        Text(
            text = "Channels, guide and playback for Live TV come from these providers.",
            style = MaterialTheme.typography.bodyMedium,
        )
        GGButton(
            text = "Add provider",
            onClick = { onOpen(Route.ProviderForm(null)) },
            icon = Icons.Default.Add,
        )
        if (providers.isEmpty()) {
            InfoCard {
                Text(
                    text = "No provider added. Use an M3U playlist link or an Xtream Codes login.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            providers.forEach { p ->
                FocusSurface(
                    onClick = { onOpen(Route.ProviderForm(p.id)) },
                    modifier = Modifier.fillMaxWidth(),
                    focusScale = 1.015f,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(text = p.summary(), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                        StatusTag(text = p.type.label, modifier = Modifier.padding(start = 12.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = GG.textDim,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Add or edit one provider. [id] is null when adding. */
@Composable
fun ProviderFormScreen(id: String?, onClose: () -> Unit) {
    val existing = remember(id) { if (id != null) ProviderStore.get(id) else null }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: ProviderType.M3U) }
    var m3u by remember { mutableStateOf(existing?.m3uUrl ?: "") }
    var server by remember { mutableStateOf(existing?.server ?: "") }
    var user by remember { mutableStateOf(existing?.username ?: "") }
    var pass by remember { mutableStateOf(existing?.password ?: "") }
    var epg by remember { mutableStateOf(existing?.epgUrl ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun build(): Provider = Provider(
        id = existing?.id ?: ProviderStore.newId(),
        name = name.trim(),
        type = type,
        m3uUrl = m3u.trim(),
        server = ProviderStore.cleanServer(server),
        username = user.trim(),
        password = pass.trim(),
        epgUrl = epg.trim(),
    )

    fun validate(): String? {
        if (name.isBlank()) return "Give the provider a name."
        if (type == ProviderType.M3U) {
            val u = m3u.trim()
            if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) {
                return "The playlist link must start with http:// or https://"
            }
        } else {
            if (server.isBlank()) return "Enter the server address."
            if (user.isBlank()) return "Enter the username."
            if (pass.isBlank()) return "Enter the password."
        }
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SubHeader(
            title = if (existing == null) "Add provider" else "Edit provider",
            onBack = onClose,
        )

        GGField(
            label = "Name",
            value = name,
            onValueChange = { name = it },
            placeholder = "My IPTV",
        )

        Text(text = "Type", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderType.values().forEach { option ->
                val selected = option == type
                GGButton(
                    text = option.label,
                    onClick = {
                        type = option
                        testResult = null
                    },
                    primary = selected,
                    icon = if (selected) Icons.Default.Check else null,
                )
            }
        }

        if (type == ProviderType.M3U) {
            GGField(
                label = "Playlist link (M3U)",
                value = m3u,
                onValueChange = { m3u = it },
                placeholder = "http://example.com/playlist.m3u",
                keyboardType = KeyboardType.Uri,
            )
        } else {
            GGField(
                label = "Server address",
                value = server,
                onValueChange = { server = it },
                placeholder = "http://example.com:8080",
                keyboardType = KeyboardType.Uri,
            )
            GGField(label = "Username", value = user, onValueChange = { user = it })
            GGField(label = "Password", value = pass, onValueChange = { pass = it }, password = true)
        }

        GGField(
            label = "TV guide link (XMLTV), optional",
            value = epg,
            onValueChange = { epg = it },
            placeholder = "http://example.com/guide.xml",
            keyboardType = KeyboardType.Uri,
        )

        val problem = error
        if (problem != null) {
            Text(text = problem, style = MaterialTheme.typography.bodyLarge.copy(color = GG.danger))
        }
        val result = testResult
        if (testing) {
            Text(text = "Testing...", style = MaterialTheme.typography.bodyMedium)
        } else if (result != null) {
            Text(
                text = result.second,
                style = MaterialTheme.typography.bodyLarge.copy(color = if (result.first) GG.ok else GG.danger),
            )
        }

        GGButton(
            text = "Test connection",
            primary = false,
            enabled = !testing,
            onClick = {
                val v = validate()
                error = v
                if (v == null) {
                    testing = true
                    testResult = null
                    val snapshot = build()
                    scope.launch {
                        val r = ProviderStore.test(snapshot)
                        testing = false
                        testResult = if (r.isSuccess) {
                            Pair(true, r.getOrNull() ?: "Works.")
                        } else {
                            Pair(false, r.exceptionOrNull()?.message ?: "Could not connect.")
                        }
                    }
                }
            },
        )
        GGButton(
            text = "Save",
            icon = Icons.Default.Check,
            onClick = {
                val v = validate()
                error = v
                if (v == null) {
                    val saved = build()
                    ProviderStore.save(saved)
                    ChannelRepo.invalidate(saved.id)
                    onClose()
                }
            },
        )
        if (existing != null) {
            GGButton(
                text = if (confirmDelete) "Tap again to delete" else "Delete provider",
                primary = false,
                danger = true,
                icon = Icons.Default.Delete,
                onClick = {
                    if (confirmDelete) {
                        ProviderStore.delete(existing.id)
                        onClose()
                    } else {
                        confirmDelete = true
                    }
                },
            )
        }
    }
}

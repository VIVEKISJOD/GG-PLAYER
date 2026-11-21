package com.ggplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.ggplayer.app.BuildConfig
import com.ggplayer.app.data.AccentChoice
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.data.LayoutMode
import com.ggplayer.app.data.ProviderStore
import com.ggplayer.app.data.StartScreen
import com.ggplayer.app.data.TextSize
import com.ggplayer.app.data.TvMargin
import com.ggplayer.app.data.cycle
import com.ggplayer.app.update.UpdateManager
import com.ggplayer.app.update.UpdateState
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(deviceIsTv: Boolean, onNavigate: (Section) -> Unit, onOpen: (Route) -> Unit) {
    val update = UpdateManager.state
    var confirmReset by remember { mutableStateOf(false) }
    val providerCount = ProviderStore.providers.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader(title = "Settings")

        GroupLabel("Library")
        SettingRow(
            title = "Providers",
            detail = if (providerCount == 0) "None added yet" else "$providerCount added",
            onClick = { onNavigate(Section.LiveTv) },
        )

        GroupLabel("App")
        SettingRow(
            title = "Updates",
            detail = "Version ${BuildConfig.VERSION_NAME}, check GitHub for a new one",
            onClick = { onOpen(Route.Updates) },
            trailing = {
                if (update is UpdateState.Available) {
                    StatusTag(text = "New", color = GG.accent)
                }
            },
        )
        ToggleRow(
            title = "Check for updates on start",
            detail = "Looks for a new release each time the app opens",
            checked = AppSettings.autoCheckUpdates,
            onChange = { AppSettings.updateAutoCheck(it) },
        )
        SettingRow(
            title = "Appearance and layout",
            detail = "Accent colour, text size, TV or phone layout, TV margin",
            onClick = { onOpen(Route.Appearance) },
        )
        ChoiceRow(
            title = "Start screen",
            detail = "What opens first",
            value = AppSettings.startScreen.label,
            onClick = { AppSettings.updateStartScreen(StartScreen.values().cycle(AppSettings.startScreen)) },
        )

        GroupLabel("Coming in step 3")
        StaticRow("Playback", "Decoder, resume, auto-play next, aspect ratio", "Not built yet")
        StaticRow("Audio and subtitles", "Languages, subtitle size and style, delay", "Not built yet")
        StaticRow("Network and buffer", "Buffer size, user agent, timeout", "Not built yet")
        StaticRow("Parental control", "PIN lock for categories", "Not built yet")
        StaticRow("Backup and restore", "Export and import providers and settings", "Not built yet")

        GroupLabel("About")
        StaticRow(
            title = "GG Player ${BuildConfig.VERSION_NAME}",
            detail = "Build ${BuildConfig.VERSION_CODE}. Device detected as ${if (deviceIsTv) "TV" else "phone or tablet"}.",
        )
        StaticRow(title = "Update source", detail = "github.com/${AppSettings.repo}")

        GGButton(
            text = if (confirmReset) "Tap again to reset" else "Reset settings",
            primary = false,
            danger = true,
            onClick = {
                if (confirmReset) {
                    AppSettings.resetAll()
                    confirmReset = false
                } else {
                    confirmReset = true
                }
            },
        )
        Box(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SubHeader(title = "Appearance and layout", onBack = onBack)
        ChoiceRow(
            title = "Accent colour",
            detail = "Tap to change",
            value = AppSettings.accent.label,
            onClick = { AppSettings.updateAccent(AccentChoice.values().cycle(AppSettings.accent)) },
        )
        ChoiceRow(
            title = "Text size",
            detail = "Makes all text bigger or smaller",
            value = AppSettings.textSize.label,
            onClick = { AppSettings.updateTextSize(TextSize.values().cycle(AppSettings.textSize)) },
        )
        ChoiceRow(
            title = "Layout",
            detail = "Auto picks TV layout on Fire TV and phone layout elsewhere",
            value = AppSettings.layoutMode.label,
            onClick = { AppSettings.updateLayoutMode(LayoutMode.values().cycle(AppSettings.layoutMode)) },
        )
        ChoiceRow(
            title = "TV safe margin",
            detail = "Space around the screen edge in TV layout",
            value = AppSettings.tvMargin.label,
            onClick = { AppSettings.updateTvMargin(TvMargin.values().cycle(AppSettings.tvMargin)) },
        )
    }
}

@Composable
fun UpdatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state = UpdateManager.state
    var installNote by remember { mutableStateOf<String?>(null) }
    var repoText by remember { mutableStateOf(AppSettings.repo) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SubHeader(title = "Updates", onBack = onBack)

        InfoCard {
            Text(
                text = "Installed: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "New versions come from github.com/${AppSettings.repo} Releases.",
                style = MaterialTheme.typography.bodyMedium,
            )

            when (state) {
                is UpdateState.Idle ->
                    Text("Not checked yet.", style = MaterialTheme.typography.bodyLarge)
                is UpdateState.Checking ->
                    Text("Checking...", style = MaterialTheme.typography.bodyLarge)
                is UpdateState.UpToDate ->
                    Text("You have the latest version.", style = MaterialTheme.typography.bodyLarge.copy(color = GG.ok))
                is UpdateState.Available -> {
                    Text(
                        text = "Version ${state.info.versionName} is available",
                        style = MaterialTheme.typography.titleMedium.copy(color = GG.accent),
                    )
                    if (state.info.sizeBytes > 0) {
                        Text(
                            text = "Size: ${state.info.sizeBytes / (1024 * 1024)} MB",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.info.notes.isNotBlank()) {
                        Text(
                            text = state.info.notes.take(400),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    GGButton(
                        text = "Download update",
                        icon = Icons.Default.Refresh,
                        onClick = { UpdateManager.download(context, state.info) },
                    )
                }
                is UpdateState.Downloading -> {
                    Text(
                        text = "Downloading ${state.percent}%",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GG.line),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(state.percent / 100f)
                                .fillMaxHeight()
                                .background(GG.accent),
                        )
                    }
                }
                is UpdateState.Ready -> {
                    Text(
                        text = "Version ${state.info.versionName} downloaded. Tap Install.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    GGButton(
                        text = "Install",
                        icon = Icons.Default.Check,
                        onClick = { installNote = UpdateManager.install(context, state.file) },
                    )
                    val note = installNote
                    if (note != null) {
                        Text(text = note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UpdateState.Failed ->
                    Text(state.message, style = MaterialTheme.typography.bodyLarge.copy(color = GG.danger))
            }
        }

        GGButton(
            text = "Check for updates",
            primary = false,
            enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading,
            icon = Icons.Default.Refresh,
            onClick = { UpdateManager.check(context) },
        )

        ToggleRow(
            title = "Check for updates on start",
            checked = AppSettings.autoCheckUpdates,
            onChange = { AppSettings.updateAutoCheck(it) },
        )

        GroupLabel("Update source")
        GGField(
            label = "GitHub repo (owner/name)",
            value = repoText,
            onValueChange = { repoText = it },
            placeholder = AppSettings.DEFAULT_REPO,
            keyboardType = KeyboardType.Uri,
        )
        GGButton(
            text = "Save source",
            primary = false,
            onClick = {
                AppSettings.updateRepo(repoText.trim())
                UpdateManager.reset()
            },
        )
        Text(
            text = "On Fire TV the first install needs permission: Settings, My Fire TV, Developer options, Install unknown apps, GG Player.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

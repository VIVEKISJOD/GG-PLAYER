package com.ggplayer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.data.AspectChoice
import com.ggplayer.app.data.Backup
import com.ggplayer.app.data.BufferChoice
import com.ggplayer.app.data.Lang
import com.ggplayer.app.data.SeekStep
import com.ggplayer.app.data.SubSize
import com.ggplayer.app.data.TimeoutChoice
import com.ggplayer.app.data.cycle
import com.ggplayer.app.data.realLanguages
import kotlinx.coroutines.launch

@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SubHeader(title = title, onBack = onBack)
        content()
    }
}

@Composable
fun PlaybackSettingsScreen(onBack: () -> Unit) {
    SettingsPage("Playback", onBack) {
        ToggleRow(
            title = "Resume where you left off",
            detail = "Movies and episodes continue from the last position",
            checked = AppSettings.resume,
            onChange = { AppSettings.updateResume(it) },
        )
        ChoiceRow(
            title = "Default aspect ratio",
            detail = "Can also be changed inside the player",
            value = AppSettings.aspect.label,
            onClick = { AppSettings.updateAspect(AspectChoice.values().cycle(AppSettings.aspect)) },
        )
        ChoiceRow(
            title = "Skip length",
            detail = "Left and right on the remote, and the skip buttons",
            value = AppSettings.seekStep.label,
            onClick = { AppSettings.updateSeekStep(SeekStep.values().cycle(AppSettings.seekStep)) },
        )
        Text(
            text = "Playback speed and channel up and down are in the player controls.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun AudioSettingsScreen(onBack: () -> Unit) {
    SettingsPage("Audio and subtitles", onBack) {
        ToggleRow(
            title = "Prefer my first language for audio",
            detail = "Picks the ${AppSettings.primaryLang.label} audio track when a video has several",
            checked = AppSettings.preferAudioLang,
            onChange = { AppSettings.updatePreferAudioLang(it) },
        )
        ToggleRow(
            title = "Subtitles on by default",
            detail = "Turns on a subtitle track in the language below",
            checked = AppSettings.subsOn,
            onChange = { AppSettings.updateSubsOn(it) },
        )
        ChoiceRow(
            title = "Subtitle language",
            value = AppSettings.subLang.label,
            onClick = { AppSettings.updateSubLang(realLanguages.cycle(AppSettings.subLang)) },
        )
        ChoiceRow(
            title = "Subtitle size",
            value = AppSettings.subSize.label,
            onClick = { AppSettings.updateSubSize(SubSize.values().cycle(AppSettings.subSize)) },
        )
        Text(
            text = "These apply the next time a video starts. Inside the player you can pick any audio or subtitle track, or load a subtitle link.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun NetworkSettingsScreen(onBack: () -> Unit) {
    var ua by remember { mutableStateOf(AppSettings.userAgent) }
    SettingsPage("Network and buffer", onBack) {
        ChoiceRow(
            title = "Buffer size",
            detail = "Bigger buffer helps weak connections but starts slower",
            value = AppSettings.buffer.label,
            onClick = { AppSettings.updateBuffer(BufferChoice.values().cycle(AppSettings.buffer)) },
        )
        ChoiceRow(
            title = "Connection timeout",
            detail = "How long to wait for a server before giving up",
            value = AppSettings.timeout.label,
            onClick = { AppSettings.updateTimeout(TimeoutChoice.values().cycle(AppSettings.timeout)) },
        )
        GGField(label = "User agent", value = ua, onValueChange = { ua = it }, placeholder = AppSettings.DEFAULT_UA)
        GGButton(
            text = "Save user agent",
            primary = false,
            icon = Icons.Default.Check,
            onClick = { AppSettings.updateUserAgent(ua.trim().ifEmpty { AppSettings.DEFAULT_UA }) },
        )
        GGButton(text = "Use default user agent", primary = false, onClick = {
            ua = AppSettings.DEFAULT_UA
            AppSettings.updateUserAgent(AppSettings.DEFAULT_UA)
        })
        Text(
            text = "Some IPTV providers only answer to a certain player name. Try VLC/3.0.18 if channels do not open.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun ParentalSettingsScreen(onBack: () -> Unit) {
    var entered by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(AppSettings.pin.isEmpty()) }
    var message by remember { mutableStateOf<String?>(null) }

    SettingsPage("Parental control", onBack) {
        Text(
            text = "Hides channel groups named adult, xxx, porn, 18+ and similar. With a PIN set, the filter can only be turned off after entering the PIN.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!unlocked) {
            GGField(
                label = "Enter PIN to change these options",
                value = entered,
                onValueChange = { entered = it.filter { c -> c.isDigit() }.take(8) },
                password = true,
                keyboardType = KeyboardType.NumberPassword,
            )
            GGButton(text = "Unlock", onClick = {
                if (entered == AppSettings.pin) {
                    unlocked = true
                    message = null
                } else {
                    message = "Wrong PIN."
                }
                entered = ""
            })
        } else {
            ToggleRow(
                title = "Hide adult channel groups",
                checked = AppSettings.hideAdult,
                onChange = { AppSettings.updateHideAdult(it) },
            )
            GGField(
                label = if (AppSettings.pin.isEmpty()) "Set a PIN (4 to 8 digits)" else "Change PIN (4 to 8 digits)",
                value = newPin,
                onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(8) },
                password = true,
                keyboardType = KeyboardType.NumberPassword,
            )
            GGButton(text = "Save PIN", primary = false, icon = Icons.Default.Check, onClick = {
                if (newPin.length < 4) {
                    message = "The PIN needs at least 4 digits."
                } else {
                    AppSettings.updatePin(newPin)
                    newPin = ""
                    message = "PIN saved."
                }
            })
            if (AppSettings.pin.isNotEmpty()) {
                GGButton(text = "Remove PIN", primary = false, danger = true, onClick = {
                    AppSettings.updatePin("")
                    message = "PIN removed."
                })
            }
        }
        val m = message
        if (m != null) Text(text = m, style = MaterialTheme.typography.bodyLarge.copy(color = GG.accent))
    }
}

@Composable
fun BackupSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var pasted by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var busy by remember { mutableStateOf(false) }

    SettingsPage("Backup and restore", onBack) {
        Text(
            text = "Copy a backup of your providers, addons, favorites and settings. Paste it into a note to keep it. To restore, paste it back here.",
            style = MaterialTheme.typography.bodyMedium,
        )
        GGButton(text = "Copy backup", icon = Icons.Default.Check, onClick = {
            clipboard.setText(AnnotatedString(Backup.export(context)))
            message = Pair(true, "Backup copied. Paste it somewhere safe.")
        })
        GroupLabel("Restore")
        GGField(
            label = "Backup text",
            value = pasted,
            onValueChange = { pasted = it },
            placeholder = "Paste the backup here",
        )
        GGButton(text = "Paste from clipboard", primary = false, onClick = {
            pasted = clipboard.getText()?.text ?: ""
        })
        GGButton(
            text = if (busy) "Restoring..." else "Restore",
            enabled = !busy && pasted.isNotBlank(),
            onClick = {
                busy = true
                scope.launch {
                    val r = Backup.restore(context, pasted)
                    busy = false
                    message = if (r.isSuccess) {
                        pasted = ""
                        Pair(true, r.getOrNull() ?: "Restored.")
                    } else {
                        Pair(false, "Could not restore: ${r.exceptionOrNull()?.message ?: "unknown error"}")
                    }
                }
            },
        )
        val m = message
        if (m != null) {
            Text(
                text = m.second,
                style = MaterialTheme.typography.bodyLarge.copy(color = if (m.first) GG.ok else GG.danger),
            )
        }
        Text(
            text = "The backup contains your provider passwords. Keep it private.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

package com.ggplayer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ggplayer.app.R
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.update.UpdateManager

/** Top-level sections. Names of Home, LiveTv and Library match data.StartScreen. */
enum class Section(val label: String) {
    Home("Home"),
    LiveTv("Live TV"),
    Library("Movies"),
    Settings("Settings"),
}

/** Screens that open on top of a section. Back closes them. */
sealed class Route {
    data class ProviderForm(val id: String?) : Route()
    object Updates : Route()
    object Appearance : Route()
}

@Composable
fun GGApp(deviceIsTv: Boolean) {
    val tv = LocalIsTv.current
    val context = LocalContext.current
    var section by remember { mutableStateOf(Section.valueOf(AppSettings.startScreen.name)) }
    var route by remember { mutableStateOf<Route?>(null) }

    // Look for a new GitHub release once when the app opens.
    LaunchedEffect(Unit) {
        if (AppSettings.autoCheckUpdates) UpdateManager.check(context, silent = true)
    }

    // Back closes an open screen, then returns to Home, then the system closes the app.
    BackHandler(enabled = route != null || section != Section.Home) {
        if (route != null) {
            route = null
        } else {
            section = Section.Home
        }
    }

    val select: (Section) -> Unit = {
        section = it
        route = null
    }

    val content: @Composable () -> Unit = {
        val r = route
        if (r == null) {
            when (section) {
                Section.Home -> HomeScreen(onNavigate = select, onOpen = { route = it })
                Section.LiveTv -> LiveTvScreen(onOpen = { route = it })
                Section.Library -> LibraryScreen()
                Section.Settings -> SettingsScreen(
                    deviceIsTv = deviceIsTv,
                    onNavigate = select,
                    onOpen = { route = it },
                )
            }
        } else if (r is Route.ProviderForm) {
            ProviderFormScreen(id = r.id, onClose = { route = null })
        } else if (r is Route.Updates) {
            UpdatesScreen(onBack = { route = null })
        } else {
            AppearanceScreen(onBack = { route = null })
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(GG.bg)) {
        if (tv) {
            Row(modifier = Modifier.fillMaxSize()) {
                SideRail(current = section, onSelect = select)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
                if (route !is Route.ProviderForm) {
                    BottomBar(current = section, onSelect = select)
                }
            }
        }
    }
}

@Composable
fun SectionIcon(section: Section, tint: Color, size: Int = 24) {
    val m = Modifier.size(size.dp)
    when (section) {
        Section.Home -> Icon(Icons.Default.Home, contentDescription = null, tint = tint, modifier = m)
        Section.LiveTv -> TvIcon(tint = tint, modifier = m)
        Section.Library -> MovieIcon(tint = tint, modifier = m)
        Section.Settings -> Icon(Icons.Default.Settings, contentDescription = null, tint = tint, modifier = m)
    }
}

/** Navigation for TV: a fixed rail on the left, driven by the remote. */
@Composable
private fun SideRail(current: Section, onSelect: (Section) -> Unit) {
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Put the remote focus on the rail when the app opens.
        runCatching { firstItem.requestFocus() }
    }

    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(GG.surface)
            .padding(horizontal = 18.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.gg_mark),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = "GG Player",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Section.values().forEachIndexed { index, item ->
            val selected = item == current
            FocusSurface(
                onClick = { onSelect(item) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier),
                color = if (selected) GG.surfaceHigh else GG.surface,
                focusScale = 1.03f,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionIcon(section = item, tint = if (selected) GG.accent else GG.textDim)
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (selected) GG.accent else GG.text,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
            }
        }
    }
}

/** Navigation for phones and tablets: icons with labels along the bottom. */
@Composable
private fun BottomBar(current: Section, onSelect: (Section) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(GG.surface)
            .padding(top = 8.dp, bottom = 6.dp),
    ) {
        Section.values().forEach { item ->
            val selected = item == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(item) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                SectionIcon(section = item, tint = if (selected) GG.accent else GG.textDim, size = 24)
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (selected) GG.accent else GG.textDim,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ggplayer.app.R

/** Top-level sections of the app. */
enum class Section(val label: String) {
    Home("Home"),
    LiveTv("Live TV"),
    Library("Movies & Series"),
    Settings("Settings"),
}

@Composable
fun GGApp() {
    val tv = LocalIsTv.current
    var section by rememberSaveable { mutableStateOf(Section.Home) }

    // Back returns to Home first; from Home the system closes the app.
    BackHandler(enabled = section != Section.Home) { section = Section.Home }

    Box(modifier = Modifier.fillMaxSize().background(GG.bg)) {
        if (tv) {
            Row(modifier = Modifier.fillMaxSize()) {
                SideRail(current = section, onSelect = { section = it })
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    SectionContent(section = section, onNavigate = { section = it })
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    SectionContent(section = section, onNavigate = { section = it })
                }
                BottomBar(current = section, onSelect = { section = it })
            }
        }
    }
}

@Composable
private fun SectionContent(section: Section, onNavigate: (Section) -> Unit) {
    when (section) {
        Section.Home -> HomeScreen(onNavigate = onNavigate)
        Section.LiveTv -> LiveTvScreen()
        Section.Library -> LibraryScreen()
        Section.Settings -> SettingsScreen()
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
            .width(260.dp)
            .fillMaxHeight()
            .background(GG.surface)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.gg_mark),
                contentDescription = null,
                modifier = Modifier.width(72.dp),
            )
            Text(
                text = "GG Player",
                style = MaterialTheme.typography.titleMedium,
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
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .background(if (selected) GG.gold else GG.surface),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (selected) GG.gold else GG.text,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

/** Navigation for phones and tablets: a plain text bar along the bottom. */
@Composable
private fun BottomBar(current: Section, onSelect: (Section) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GG.surface),
    ) {
        Section.values().forEach { item ->
            val selected = item == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(item) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (selected) GG.gold else GG.line),
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (selected) GG.gold else GG.textDim,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

package com.ggplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ggplayer.app.data.Lang
import com.ggplayer.app.data.MetaPreview
import com.ggplayer.app.data.StreamOption
import com.ggplayer.app.data.StreamParser
import com.ggplayer.app.data.AppSettings

/** Small rounded label. [match] 2 = first choice, 1 = second choice (accent colours), 0 = plain. */
@Composable
fun Badge(text: String, match: Int = 0, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val bg = if (match == 2) GG.accent else GG.surfaceHigh
    val fg = when (match) {
        2 -> GG.onAccent
        1 -> GG.accent
        else -> GG.textDim
    }
    Box(
        modifier = modifier
            .background(bg, shape)
            .border(1.dp, if (match >= 1) GG.accent else GG.line, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(color = fg),
            maxLines = 1,
        )
    }
}

/** Language, quality and source badges for one stream. Badges that match the settings are coloured. */
@Composable
fun StreamBadges(s: StreamOption, best: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (best) Badge("Best match", match = 2)
        if (s.languages.isEmpty() && !s.multiAudio) {
            Badge("Language unknown")
        }
        for (l: Lang in s.languages) {
            Badge(l.label, match = StreamParser.languageMatch(l))
        }
        if (s.multiAudio) Badge("Dual audio", match = if (s.languages.isEmpty()) 1 else 0)
        Badge(
            s.qualityLabel(),
            match = if (s.resolution == AppSettings.quality.res) 2 else 0,
        )
        Badge(s.addonName)
        if (s.isTorrent) Badge("Torrent, cannot play")
        if (s.isCam) Badge("CAM")
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) GG.accent else GG.surface,
        focusedColor = if (selected) GG.accent else GG.surfaceHigh,
        outline = if (selected) androidx.compose.ui.graphics.Color.White else GG.accent,
        borderColor = if (selected) GG.accent else GG.line,
        focusScale = 1.05f,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = if (selected) GG.onAccent else GG.text),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun PosterCard(item: MetaPreview, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FocusSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            focusScale = 1.06f,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(GG.surfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (item.poster != null) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                if (item.rating.isNotBlank()) {
                    Badge(
                        text = item.rating,
                        match = 2,
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    )
                }
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
fun PosterRow(title: String, items: List<MetaPreview>, onOpen: (MetaPreview) -> Unit) {
    val tv = LocalIsTv.current
    val w: Dp = if (tv) 170.dp else 118.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title = title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 4.dp),
        ) {
            itemsIndexed(items) { _, item ->
                PosterCard(item = item, onClick = { onOpen(item) }, modifier = Modifier.width(w))
            }
        }
    }
}

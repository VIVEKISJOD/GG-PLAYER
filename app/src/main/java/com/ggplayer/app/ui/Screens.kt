package com.ggplayer.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ggplayer.app.BuildConfig
import com.ggplayer.app.R
import com.ggplayer.app.data.Provider
import com.ggplayer.app.data.ProviderStore
import com.ggplayer.app.update.UpdateManager
import com.ggplayer.app.update.UpdateState

@Composable
fun HomeScreen(onNavigate: (Section) -> Unit, onOpen: (Route) -> Unit) {
    val tv = LocalIsTv.current
    val update = UpdateManager.state
    val providers = ProviderStore.providers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(if (tv) 28.dp else 22.dp),
    ) {
        BrandBar(onSettings = { onNavigate(Section.Settings) })

        if (update is UpdateState.Available) {
            UpdateBanner(version = update.info.versionName, onClick = { onOpen(Route.Updates) })
        }

        HeroCard(
            hasProviders = providers.isNotEmpty(),
            onPrimary = {
                if (providers.isEmpty()) onOpen(Route.ProviderForm(null)) else onNavigate(Section.LiveTv)
            },
        )

        // Categories
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title = "Categories")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(if (tv) 28.dp else 18.dp),
            ) {
                CategoryItem(label = "Live TV", onClick = { onNavigate(Section.LiveTv) }) { tint ->
                    TvIcon(tint = tint, modifier = Modifier.size(26.dp))
                }
                CategoryItem(label = "Movies", onClick = { onNavigate(Section.Library) }) { tint ->
                    MovieIcon(tint = tint, modifier = Modifier.size(26.dp))
                }
                CategoryItem(label = "Series", onClick = { onNavigate(Section.Library) }) { tint ->
                    SeriesIcon(tint = tint, modifier = Modifier.size(26.dp))
                }
                CategoryItem(label = "Add provider", onClick = { onOpen(Route.ProviderForm(null)) }) { tint ->
                    Icon(Icons.Default.Add, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                }
            }
        }

        // Providers
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                title = "Your providers",
                action = if (providers.isNotEmpty()) "View all" else null,
                onAction = { onNavigate(Section.LiveTv) },
            )
            if (providers.isEmpty()) {
                InfoCard {
                    Text(
                        text = "No provider yet. Add an M3U link or an Xtream login to get started.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    GGButton(
                        text = "Add provider",
                        onClick = { onOpen(Route.ProviderForm(null)) },
                        icon = Icons.Default.Add,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    providers.forEach { p ->
                        ProviderCard(
                            provider = p,
                            onClick = { onOpen(Route.ProviderForm(p.id)) },
                            modifier = Modifier.width(if (tv) 300.dp else 220.dp),
                        )
                    }
                }
            }
        }

        // Trending placeholder
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trending now",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                StatusTag(text = "Not built yet - step 3")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .width(if (tv) 150.dp else 84.dp)
                            .height(if (tv) 220.dp else 124.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GG.surface)
                            .border(1.dp, GG.line, RoundedCornerShape(12.dp)),
                    )
                }
            }
            Text(
                text = "Posters from your addons will appear here.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}  -  ${if (tv) "TV" else "Phone or tablet"} layout",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BrandBar(onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.gg_mark),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Text(
            text = "GG Player",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
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
private fun HeroCard(hasProviders: Boolean, onPrimary: () -> Unit) {
    val tv = LocalIsTv.current
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (tv) 260.dp else 230.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2B1A0B), GG.surface)))
            .border(1.dp, GG.line, shape),
    ) {
        Image(
            painter = painterResource(R.drawable.gg_mark),
            contentDescription = null,
            alpha = 0.55f,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(if (tv) 200.dp else 120.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp, end = if (tv) 260.dp else 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Your TV.", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Your way.",
                style = MaterialTheme.typography.headlineLarge.copy(color = GG.accent),
            )
            Text(
                text = "Live TV from the providers you add yourself.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            GGButton(
                text = if (hasProviders) "Open Live TV" else "Get started",
                onClick = onPrimary,
            )
        }
    }
}

@Composable
private fun CategoryItem(
    label: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FocusSurface(onClick = onClick, shape = CircleShape) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                icon(GG.accent)
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge.copy(color = GG.textDim), maxLines = 1)
    }
}

@Composable
fun ProviderCard(provider: Provider, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = provider.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(text = provider.summary(), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            StatusTag(text = provider.type.label, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun LibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pagePadding()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(title = "Movies and Series", subtitle = "Catalogs from the addons you add.")
        InfoCard {
            Text(
                text = "No addon added. Paste a manifest.json link here to load catalogs and streams.",
                style = MaterialTheme.typography.bodyLarge,
            )
            StatusTag(text = "Not built yet - step 3")
        }
    }
}

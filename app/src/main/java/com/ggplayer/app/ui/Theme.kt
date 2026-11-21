package com.ggplayer.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colours taken from the GG logo: slate background, gold and blue accents. */
object GG {
    val bg = Color(0xFF1C232C)
    val surface = Color(0xFF242D38)
    val surfaceHigh = Color(0xFF2E3946)
    val line = Color(0xFF354251)
    val text = Color(0xFFE8ECF0)
    val textDim = Color(0xFF8F9BA8)
    val gold = Color(0xFFC9A24B)
    val blue = Color(0xFF3C8DD9)
}

/** True when running on a remote-controlled TV (Fire TV, Android TV). */
val LocalIsTv = staticCompositionLocalOf { false }

@Composable
fun GGTheme(isTv: Boolean, content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = GG.gold,
        secondary = GG.blue,
        background = GG.bg,
        surface = GG.surface,
        onPrimary = Color(0xFF1A1405),
        onBackground = GG.text,
        onSurface = GG.text,
    )
    CompositionLocalProvider(LocalIsTv provides isTv) {
        MaterialTheme(
            colorScheme = colors,
            typography = ggTypography(isTv),
            content = content,
        )
    }
}

private fun ggTypography(tv: Boolean): Typography {
    val s = if (tv) 1.25f else 1f
    return Typography(
        headlineLarge = TextStyle(
            fontSize = (30 * s).sp,
            lineHeight = (36 * s).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = GG.text,
        ),
        titleMedium = TextStyle(
            fontSize = (18 * s).sp,
            lineHeight = (24 * s).sp,
            fontWeight = FontWeight.Medium,
            color = GG.text,
        ),
        bodyLarge = TextStyle(
            fontSize = (16 * s).sp,
            lineHeight = (24 * s).sp,
            color = GG.text,
        ),
        bodyMedium = TextStyle(
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp,
            color = GG.textDim,
        ),
        labelSmall = TextStyle(
            fontSize = (11 * s).sp,
            lineHeight = (14 * s).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = GG.textDim,
        ),
    )
}

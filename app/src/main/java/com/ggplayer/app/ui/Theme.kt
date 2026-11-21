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
import com.ggplayer.app.data.AppSettings

/** Cinevia-style palette: near black surfaces and one accent colour that the user can change. */
object GG {
    val bg = Color(0xFF0B0B0E)
    val surface = Color(0xFF15151A)
    val surfaceHigh = Color(0xFF1F1F26)
    val line = Color(0xFF2A2A33)
    val text = Color(0xFFF4F4F6)
    val textDim = Color(0xFF9B9BA6)
    val onAccent = Color(0xFF1A0E00)
    val danger = Color(0xFFE5534B)
    val ok = Color(0xFF4CB782)

    /** Reads settings state, so changing the accent in Settings recolours the app at once. */
    val accent: Color
        get() = AppSettings.accent.color
}

/** True when the TV layout is in use (Fire TV, Android TV, or forced in Settings). */
val LocalIsTv = staticCompositionLocalOf { false }

@Composable
fun GGTheme(isTv: Boolean, content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = GG.accent,
        secondary = GG.accent,
        background = GG.bg,
        surface = GG.surface,
        onPrimary = GG.onAccent,
        onBackground = GG.text,
        onSurface = GG.text,
    )
    CompositionLocalProvider(LocalIsTv provides isTv) {
        MaterialTheme(
            colorScheme = colors,
            typography = ggTypography(isTv, AppSettings.textSize.scale),
            content = content,
        )
    }
}

private fun ggTypography(tv: Boolean, userScale: Float): Typography {
    val s = (if (tv) 1.25f else 1f) * userScale
    return Typography(
        headlineLarge = TextStyle(
            fontSize = (32 * s).sp,
            lineHeight = (38 * s).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = GG.text,
        ),
        headlineMedium = TextStyle(
            fontSize = (24 * s).sp,
            lineHeight = (30 * s).sp,
            fontWeight = FontWeight.SemiBold,
            color = GG.text,
        ),
        titleLarge = TextStyle(
            fontSize = (20 * s).sp,
            lineHeight = (26 * s).sp,
            fontWeight = FontWeight.SemiBold,
            color = GG.text,
        ),
        titleMedium = TextStyle(
            fontSize = (16 * s).sp,
            lineHeight = (22 * s).sp,
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
        labelLarge = TextStyle(
            fontSize = (14 * s).sp,
            lineHeight = (18 * s).sp,
            fontWeight = FontWeight.SemiBold,
            color = GG.text,
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

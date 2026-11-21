package com.ggplayer.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ggplayer.app.data.AppSettings

/** Shared outer padding. TVs use the safe margin chosen in Settings so nothing sits in the overscan area. */
@Composable
fun pagePadding(): Modifier {
    val tv = LocalIsTv.current
    return if (tv) {
        Modifier.padding(horizontal = AppSettings.tvMargin.value.dp, vertical = 28.dp)
    } else {
        Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
    }
}

/**
 * The basic focusable surface used everywhere. On a TV the focused item gets an
 * accent outline and grows a little. On a phone it reacts to taps.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    color: Color = GG.surface,
    focusedColor: Color = GG.surfaceHigh,
    outline: Color = GG.accent,
    borderColor: Color = GG.line,
    focusScale: Float = 1.04f,
    content: @Composable BoxScope.() -> Unit,
) {
    val tv = LocalIsTv.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (focused) focusedColor else color,
        label = "surfaceBackground",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused && tv) focusScale else 1f,
        label = "surfaceScale",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (pressed) 0.8f else 1f)
            .clip(shape)
            .background(background)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) outline else borderColor,
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        content = content,
    )
}

/** Filled accent button (primary) or outlined button (secondary). */
@Composable
fun GGButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    danger: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val textColor = when {
        danger -> GG.danger
        primary -> GG.onAccent
        else -> GG.text
    }
    FocusSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(50),
        color = if (primary && !danger) GG.accent else GG.surface,
        focusedColor = if (primary && !danger) GG.accent else GG.surfaceHigh,
        outline = if (primary && !danger) Color.White else GG.accent,
        borderColor = if (primary && !danger) GG.accent else GG.line,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = textColor, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Round icon button. */
@Composable
fun IconCircleButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(onClick = onClick, modifier = modifier, shape = CircleShape) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = GG.text,
            modifier = Modifier.padding(10.dp).size(22.dp),
        )
    }
}

/** Small outlined label, used to mark things that are not built yet. */
@Composable
fun StatusTag(text: String, modifier: Modifier = Modifier, color: Color = GG.textDim) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .border(1.dp, GG.line, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = color))
    }
}

/** Title with an optional "View all" style action on the right. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            FocusSurface(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                borderColor = Color.Transparent,
                focusScale = 1.02f,
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge.copy(color = GG.accent),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Page title block. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Header for sub screens: back button and title. */
@Composable
fun SubHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconCircleButton(
            icon = Icons.Default.ArrowBack,
            description = "Back",
            onClick = onBack,
        )
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}

/** Plain bordered card for messages and empty states. */
@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GG.surface)
            .border(1.dp, GG.line, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

// ---- Settings style rows ----

@Composable
fun GGSwitch(checked: Boolean) {
    val track by animateColorAsState(
        targetValue = if (checked) GG.accent else GG.line,
        label = "switchTrack",
    )
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    detail: String? = null,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        focusScale = 1.015f,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (detail != null) {
                    Text(text = detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Box(modifier = Modifier.padding(start = 12.dp)) { trailing() }
        }
    }
}

@Composable
fun ToggleRow(title: String, detail: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(
        title = title,
        detail = detail,
        onClick = { onChange(!checked) },
        trailing = { GGSwitch(checked = checked) },
    )
}

/** Tap (or press OK) to move to the next value. Shows the current value on the right. */
@Composable
fun ChoiceRow(title: String, detail: String? = null, value: String, onClick: () -> Unit) {
    SettingRow(
        title = title,
        detail = detail,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, style = MaterialTheme.typography.labelLarge.copy(color = GG.accent))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GG.textDim,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
}

/** Row that cannot be selected. Used for "not built yet" items and plain information. */
@Composable
fun StaticRow(title: String, detail: String? = null, tag: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, GG.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (detail != null) {
                Text(text = detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (tag != null) {
            StatusTag(text = tag, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
    )
}

/** Text input that looks the same on phone and TV. */
@Composable
fun GGField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            cursorBrush = SolidColor(GG.accent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else keyboardType,
            ),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(GG.surface)
                        .border(
                            width = if (focused) 2.dp else 1.dp,
                            color = if (focused) GG.accent else GG.line,
                            shape = shape,
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(color = GG.textDim),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

// ---- Icons drawn by hand (no icon library needed) ----

@Composable
fun TvIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.24f),
            size = Size(w * 0.84f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = sw),
        )
        drawLine(tint, Offset(w * 0.5f, h * 0.24f), Offset(w * 0.3f, h * 0.08f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.24f), Offset(w * 0.7f, h * 0.08f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.3f, h * 0.92f), Offset(w * 0.7f, h * 0.92f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun MovieIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.16f),
            size = Size(w * 0.84f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = sw),
        )
        val play = Path().apply {
            moveTo(w * 0.42f, h * 0.34f)
            lineTo(w * 0.42f, h * 0.66f)
            lineTo(w * 0.68f, h * 0.5f)
            close()
        }
        drawPath(path = play, color = tint)
    }
}

@Composable
fun SeriesIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.22f, h * 0.1f),
            size = Size(w * 0.7f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = sw),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.38f),
            size = Size(w * 0.7f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = sw),
        )
    }
}

package com.yaleed.vpnresearch.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yaleed.vpnresearch.ui.theme.brandAccent

/**
 * Consistent equal-weight action rows for Root Lab / Research / VPN clusters.
 * Every child should carry `Modifier.weight(1f)` (see [DsActionButton]).
 * Enforces the 8dp gap; 48dp targets come from [DsActionButton].
 */
@Composable
fun DsButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Button hierarchy variants semantic — exactly one filled-primary per row. */
enum class DsButtonVariant { Primary, Error, Outlined, Tonal }

/**
 * One equal-weight action button: 48dp min target, vertical-centred icon+text,
 * single-line label. Use inside [DsButtonRow] with `Modifier.weight(1f)`, or
 * standalone/full-width with `Modifier.fillMaxWidth()`.
 */
@Composable
fun DsActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    variant: DsButtonVariant = DsButtonVariant.Outlined,
) {
    val iconSize = 20.dp
    val colors = when (variant) {
        DsButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        DsButtonVariant.Error -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
        DsButtonVariant.Tonal -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        DsButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
    val shape = MaterialTheme.shapes.small
    if (variant == DsButtonVariant.Outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(48.dp),
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = colors,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(48.dp),
            shape = shape,
            colors = colors,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One privacy-card container: tinted surface + hairline border + consistent radius. */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 16,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(contentPadding.dp)) { content() }
    }
}

/** UPPERCASE section label (Hick's law: chunk long screens into sections). */
@Composable
fun DsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

/** Colored status pill with a leading dot (color never carries meaning alone). */
@Composable
fun DsPill(text: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f),
        contentColor = tint,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(7.dp)
                    .background(color = tint, shape = CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

/** Label → value row (values in monospace, tabular for numbers). */
@Composable
fun DsStat(label: String, value: String, modifier: Modifier = Modifier, tint: Color? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = tint ?: MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** Small right-aligned secondary header used inside cards. */
@Composable
fun DsCardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Premium YaleVPN brand mark: shield (navy→emerald gradient) with a gold
 * "Y / tunnel" stroke and a green connection node at the base.
 */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val cx = s / 2f
            val cy = s / 2f
            val gradient = Brush.verticalGradient(
                0f to Color(0xFF10182B),
                s * 0.62f to Color(0xFF12202F),
                1f to Color(0xFF0C5C43),
            )
            val shield = Path().apply {
                val top = s * 0.16f
                val bot = s * 0.86f
                val wHalf = s * 0.42f
                moveTo(cx, top)
                cubicTo(cx + wHalf * 0.25f, top, cx + wHalf, top + s * 0.06f, cx + wHalf, top + s * 0.12f)
                lineTo(cx + wHalf * 0.92f, bot * 0.72f)
                cubicTo(cx + wHalf * 0.85f, bot, cx + wHalf * 0.35f, bot, cx, bot)
                cubicTo(cx - wHalf * 0.35f, bot, cx - wHalf * 0.85f, bot, cx - wHalf * 0.92f, bot * 0.72f)
                lineTo(cx - wHalf, top + s * 0.12f)
                cubicTo(cx - wHalf, top + s * 0.06f, cx - wHalf * 0.25f, top, cx, top)
                close()
            }
            drawPath(shield, brush = gradient)
            drawPath(
                shield,
                color = Color(0xFFE7B64F),
                style = Stroke(width = s * 0.055f, cap = StrokeCap.Round),
            )
            // Gold "Y" tunnel
            val tunnel = Path().apply {
                val tTop = s * 0.42f
                val cxHalf = s * 0.20f
                moveTo(cx - cxHalf, tTop)
                lineTo(cx, tTop + s * 0.24f)
                lineTo(cx + cxHalf, tTop)
                moveTo(cx, tTop + s * 0.24f)
                lineTo(cx, tTop + s * 0.58f)
            }
            drawPath(
                tunnel,
                color = Color(0xFFE7B64F),
                style = Stroke(width = s * 0.068f, cap = StrokeCap.Round),
            )
            // connection node at the base
            drawCircle(
                color = Color(0xFF3DDC84),
                radius = s * 0.045f,
                center = Offset(cx, s * 0.70f),
            )
        }
    }
}

/**
 * Premium top app bar: brand mark + "YaleVPN" wordmark on the left, theme
 * toggle on the right, hairline bottom border. Theme-aware.
 */
@Composable
fun DsTopBar(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "YaleVPN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = brandAccent(),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
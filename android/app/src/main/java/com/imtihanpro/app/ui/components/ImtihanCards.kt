package com.imtihanpro.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.R
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanMotion
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular

@Composable
fun SilverBlueCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    animateContent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalImtihanColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = ImtihanMotion.snappySpring(),
        label = "card_press_scale"
    )

    val container = if (elevated) colors.cardElevated else colors.cardSurface
    val shape = RoundedCornerShape(ImtihanRadii.card)
    val borderColor = colors.cardBorder.copy(
        alpha = if (elevated) 0.92f else if (colors.isDark) 0.72f else 0.92f
    )
    val border = BorderStroke(ImtihanDimensions.hairlineBorder, borderColor)
    val contentModifier = Modifier
        .padding(contentPadding)
        .then(if (animateContent) Modifier.animateContentSize(animationSpec = ImtihanMotion.standardTween()) else Modifier)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            shape = shape,
            color = container,
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(modifier = contentModifier, content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container,
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(modifier = contentModifier, content = content)
        }
    }
}

/**
 * Cobalt-blue focal hero card matching the visual reference's signature Continue Learning,
 * Subject header, and Mistake Master hero cards. Uses subtle gradient depth and crisp contrast.
 */
@Composable
fun ImtihanHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalImtihanColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = ImtihanMotion.snappySpring(),
        label = "hero_press_scale"
    )

    val gradient = if (colors.isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1F386E), Color(0xFF162447))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF3E67D6), Color(0xFF2848A3))
        )
    }
    val shape = RoundedCornerShape(ImtihanRadii.panel)
    val borderColor = if (colors.isDark) {
        colors.accentBlue.copy(alpha = 0.42f)
    } else {
        Color(0xFF254399)
    }

    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick ?: {},
        enabled = onClick != null,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}

@Composable
fun SilverBlueCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalImtihanColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasDeep)
    ) {
        content()
    }
}

@Composable
fun AnimatedProgressRing(
    progress: Float,
    size: Dp = 72.dp,
    strokeWidth: Dp = 7.dp,
    accentColor: Color? = null,
    centerLabel: String,
    centerCaption: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    val animatedSweep by animateFloatAsState(
        targetValue = (progress.coerceIn(0f, 1f)) * 360f,
        animationSpec = ImtihanMotion.emphasisTween(),
        label = "ring_sweep"
    )
    val ringColor = accentColor ?: colors.cyanPulse

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            drawArc(
                color = colors.cardBorder.copy(alpha = 0.42f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(colors.accentBlue, ringColor, colors.accentBlue)),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            if (!centerCaption.isNullOrBlank()) {
                Text(
                    text = centerCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun SubjectVectorIcon(
    iconKey: String,
    modifier: Modifier = Modifier.size(26.dp)
) {
    val resId = when (iconKey.lowercase()) {
        "physics" -> R.drawable.ic_sub_physics
        "chemistry" -> R.drawable.ic_sub_chemistry
        "math" -> R.drawable.ic_sub_math
        "biology" -> R.drawable.ic_sub_biology
        "cs" -> R.drawable.ic_sub_cs
        "english" -> R.drawable.ic_sub_english
        "urdu" -> R.drawable.ic_sub_urdu
        "islamiat" -> R.drawable.ic_sub_islamiat
        else -> R.drawable.ic_sub_physics
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = iconKey,
        modifier = modifier
    )
}

@Composable
fun ImtihanBrandEmblem(modifier: Modifier = Modifier.size(44.dp)) {
    Image(
        painter = painterResource(id = R.drawable.ic_imtihan_launcher_fg),
        contentDescription = "Imtihan Pro Emblem",
        modifier = modifier
    )
}

@Composable
fun ZeroPillMetaLine(
    text: String,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: colors.cyanPulse,
        modifier = modifier
    )
}

@Composable
fun ImtihanProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color? = null,
    fillColor: Color? = null
) {
    val colors = LocalImtihanColors.current
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp)),
        color = fillColor ?: colors.cyanPulse,
        trackColor = trackColor ?: colors.cardBorder.copy(alpha = 0.4f),
        strokeCap = StrokeCap.Round
    )
}

@Composable
fun OfflineModeBanner(visible: Boolean) {
    if (!visible) return
    val colors = LocalImtihanColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(ImtihanRadii.button),
        color = colors.warningGold.copy(alpha = if (colors.isDark) 0.14f else 0.12f),
        border = BorderStroke(1.dp, colors.warningGold.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = "Offline mode active",
                tint = colors.warningGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Offline mode · Showing downloaded & cached materials",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
fun ImtihanLoadingView(
    message: String = "Loading study workspace...",
    showSkeletons: Boolean = false
) {
    val colors = LocalImtihanColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ImtihanSpacing.md, vertical = ImtihanSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ImtihanSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = colors.accentBlue,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
        if (showSkeletons) {
            ImtihanSkeletonList(count = 3)
        }
    }
}

@Composable
fun ImtihanEmptyView(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    tint: Color? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = LocalImtihanColors.current
    val resolvedTint = tint ?: colors.accentBlue
    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = resolvedTint.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                    shape = RoundedCornerShape(ImtihanRadii.button),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ImtihanErrorView(message: String, onRetry: () -> Unit) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.danger.copy(alpha = 0.14f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Error",
                            tint = colors.danger,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unable to load content",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                shape = RoundedCornerShape(ImtihanRadii.button),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Try Again", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

package com.imtihanpro.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.R
import com.imtihanpro.app.ui.theme.LocalImtihanColors

@Composable
fun SilverBlueCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalImtihanColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_press_scale"
    )

    val container = if (elevated) colors.cardElevated else colors.cardSurface
    val shape = RoundedCornerShape(20.dp)
    val glassBrush = Brush.linearGradient(
        colors = listOf(
            colors.glassTopSheen.copy(alpha = if (colors.isDark) 0.45f else 0.85f),
            colors.cardBorder.copy(alpha = if (elevated) 0.72f else 0.45f),
            colors.cyanPulse.copy(alpha = if (elevated) 0.35f else 0.16f),
        )
    )
    val border = BorderStroke(1.dp, glassBrush)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            shape = shape,
            color = container.copy(alpha = if (colors.isDark) 0.92f else 0.98f),
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container.copy(alpha = if (colors.isDark) 0.92f else 0.98f),
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SilverBlueCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalImtihanColors.current
    val brush = Brush.verticalGradient(
        colors = listOf(colors.canvasDeep, colors.canvasMid, colors.canvasDeep)
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accentBlue.copy(alpha = if (colors.isDark) 0.14f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.09f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.88f, size.height * 0.09f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.cyanPulse.copy(alpha = if (colors.isDark) 0.08f else 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.72f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.12f, size.height * 0.72f)
            )
        }
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
        animationSpec = tween(durationMillis = 550),
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium,
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
        text = text.uppercase(),
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = colors.cardElevated,
        border = BorderStroke(1.dp, colors.warningGold.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = "Offline mode active · Showing cached & downloaded materials",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
fun ImtihanLoadingView(message: String = "Loading study workspace...") {
    val colors = LocalImtihanColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = colors.cyanPulse)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
    }
}

@Composable
fun ImtihanEmptyView(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ImtihanErrorView(message: String, onRetry: () -> Unit) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = colors.danger
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Unable to load content", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(message, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

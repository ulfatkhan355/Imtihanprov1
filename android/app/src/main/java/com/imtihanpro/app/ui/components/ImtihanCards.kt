package com.imtihanpro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.ui.theme.LocalImtihanColors

@Composable
fun SilverBlueCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalImtihanColors.current
    val container = if (elevated) colors.cardElevated else colors.cardSurface
    val shape = RoundedCornerShape(18.dp)
    val border = BorderStroke(1.dp, colors.cardBorder.copy(alpha = 0.75f))

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = container,
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container,
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
        colors = listOf(colors.canvasDeep, colors.canvasMid)
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
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
        color = fillColor ?: colors.accentBlue,
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
        shape = RoundedCornerShape(12.dp),
        color = colors.cardElevated,
        border = BorderStroke(1.dp, colors.warningGold.copy(alpha = 0.6f))
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
        CircularProgressIndicator(color = colors.accentBlue)
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

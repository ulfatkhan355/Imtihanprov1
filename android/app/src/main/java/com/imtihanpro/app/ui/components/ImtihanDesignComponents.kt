package com.imtihanpro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanMotion
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular

@Composable
fun ImtihanTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    showBottomDivider: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ImtihanDimensions.topBarHeight)
                .padding(horizontal = ImtihanSpacing.sm, vertical = ImtihanSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(
                        minWidth = ImtihanDimensions.minTouchTarget,
                        minHeight = ImtihanDimensions.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(ImtihanSpacing.sm))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ImtihanSpacing.xs),
                content = actions
            )
        }
        if (showBottomDivider) {
            HorizontalDivider(
                thickness = ImtihanDimensions.hairlineBorder,
                color = colors.cardBorder.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ImtihanSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ImtihanRadii.card),
        color = colors.cardSurface,
        border = BorderStroke(ImtihanDimensions.hairlineBorder, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { idx, label ->
                val selected = selectedIndex == idx
                val containerColor by animateColorAsState(
                    targetValue = if (selected) colors.accentBlue else Color.Transparent,
                    animationSpec = ImtihanMotion.fastTween(),
                    label = "segment_bg_$idx"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
                    animationSpec = ImtihanMotion.fastTween(),
                    label = "segment_fg_$idx"
                )
                Surface(
                    onClick = { onSelect(idx) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp),
                    shape = RoundedCornerShape(ImtihanRadii.button),
                    color = containerColor
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImtihanNavigationRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    trailingText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    val tint = iconTint ?: colors.accentBlue
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(ImtihanRadii.chip),
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accentBlue,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ImtihanSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentBlue
                )
            }
        }
    }
}

@Composable
fun ImtihanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = LocalImtihanColors.current
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
        shape = RoundedCornerShape(ImtihanRadii.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accentBlue,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = ImtihanSpacing.md, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(ImtihanSpacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(ImtihanDimensions.iconSmall)
            )
            Spacer(modifier = Modifier.width(ImtihanSpacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ImtihanSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = LocalImtihanColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
        shape = RoundedCornerShape(ImtihanRadii.button),
        border = BorderStroke(1.dp, colors.cardBorder),
        contentPadding = PaddingValues(horizontal = ImtihanSpacing.md, vertical = 12.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = colors.accentBlue,
                modifier = Modifier.size(ImtihanDimensions.iconSmall)
            )
            Spacer(modifier = Modifier.width(ImtihanSpacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ImtihanMetricTile(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ImtihanRadii.card),
        color = colors.cardSurface,
        border = BorderStroke(ImtihanDimensions.hairlineBorder, colors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(ImtihanRadii.chip),
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.tabular(),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ImtihanStatusBadge(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, tint.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ImtihanSkeletonList(count: Int = 3) {
    val colors = LocalImtihanColors.current
    val transition = rememberInfiniteTransition(label = "skeleton_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.cardElevated.copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.cardBorder.copy(alpha = pulseAlpha * 0.75f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.cardBorder.copy(alpha = pulseAlpha * 0.55f))
                )
            }
        }
    }
}

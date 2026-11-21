package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.ui.audio.AmbientSoundMode
import com.imtihanpro.app.ui.audio.AmbientSoundscapePlayer
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.AnimatedProgressRing
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.ImtihanTopBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular
import kotlinx.coroutines.delay

@Composable
fun FocusPomodoroScreen(
    feedback: ImtihanFeedbackController? = null,
    onLogFocusSession: (minutes: Int, completed: Boolean, onXpEarned: (Int) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalImtihanColors.current
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember(selectedMinutes) { mutableIntStateOf(selectedMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedAmbientMode by remember { mutableStateOf(AmbientSoundMode.NONE) }
    var lastXpBanner by remember { mutableStateOf<String?>(null) }

    val ambientPlayer = remember { AmbientSoundscapePlayer() }

    DisposableEffect(Unit) {
        onDispose {
            ambientPlayer.stop()
        }
    }

    LaunchedEffect(isRunning, selectedAmbientMode) {
        if (isRunning && selectedAmbientMode != AmbientSoundMode.NONE) {
            ambientPlayer.setMode(selectedAmbientMode)
        } else {
            ambientPlayer.stop()
        }
    }

    LaunchedEffect(isRunning, selectedMinutes) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
        }
        if (isRunning && remainingSeconds <= 0) {
            isRunning = false
            ambientPlayer.stop()
            feedback?.fanfare()
            onLogFocusSession(selectedMinutes, true) { xp ->
                lastXpBanner = "Sprint complete! +$xp XP & $selectedMinutes min added to streak."
            }
        }
    }

    val totalSeconds = (selectedMinutes * 60).coerceAtLeast(1)
    val elapsedSeconds = (totalSeconds - remainingSeconds).coerceAtLeast(0)
    val progress = elapsedSeconds.toFloat() / totalSeconds.toFloat()
    val formattedTime = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Focus Pomodoro Studio",
                subtitle = "Deep study sprint & ambient audio",
                onBack = {
                    ambientPlayer.stop()
                    onBack()
                },
                showBottomDivider = true,
                actions = {
                    Icon(
                        imageVector = Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        tint = colors.cyanPulse,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15 to "Quick 15m", 25 to "Pomodoro 25m", 50 to "Deep 50m").forEach { (mins, label) ->
                                    val active = selectedMinutes == mins
                                    Surface(
                                        onClick = {
                                            feedback?.tap()
                                            isRunning = false
                                            selectedMinutes = mins
                                            remainingSeconds = mins * 60
                                            lastXpBanner = null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp),
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = if (active) colors.accentBlue else colors.cardSurface,
                                        border = BorderStroke(1.dp, if (active) colors.cyanPulse else colors.cardBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium.tabular(),
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                color = if (active) Color.White else colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            AnimatedProgressRing(
                                progress = progress,
                                size = 188.dp,
                                strokeWidth = 11.dp,
                                accentColor = if (isRunning) colors.cyanPulse else colors.accentBlue,
                                centerLabel = formattedTime,
                                centerCaption = when {
                                    remainingSeconds == 0 -> "Sprint Done"
                                    isRunning -> "Focusing"
                                    elapsedSeconds > 0 -> "Paused"
                                    else -> "Ready"
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        feedback?.tap()
                                        if (remainingSeconds == 0) {
                                            remainingSeconds = selectedMinutes * 60
                                        }
                                        isRunning = !isRunning
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 50.dp),
                                    shape = RoundedCornerShape(ImtihanRadii.button),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRunning) "Pause Sprint" else if (elapsedSeconds > 0 && remainingSeconds > 0) "Resume" else "Start Sprint",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        feedback?.tap()
                                        isRunning = false
                                        remainingSeconds = selectedMinutes * 60
                                        lastXpBanner = null
                                    },
                                    modifier = Modifier.defaultMinSize(
                                        minWidth = ImtihanDimensions.minTouchTarget,
                                        minHeight = 50.dp
                                    ),
                                    shape = RoundedCornerShape(ImtihanRadii.button)
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Reset timer")
                                }
                            }

                            if (elapsedSeconds >= 60 && remainingSeconds > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        val minsFocused = (elapsedSeconds / 60).coerceAtLeast(1)
                                        isRunning = false
                                        ambientPlayer.stop()
                                        feedback?.correct()
                                        onLogFocusSession(minsFocused, false) { xp ->
                                            lastXpBanner = "Logged $minsFocused min focus sprint (+$xp XP)."
                                        }
                                        remainingSeconds = selectedMinutes * 60
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                    shape = RoundedCornerShape(ImtihanRadii.button)
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.success)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish Early & Save ${elapsedSeconds / 60}m")
                                }
                            }
                        }
                    }
                }

                if (!lastXpBanner.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            color = colors.success.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, colors.success.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success)
                                Text(
                                    text = lastXpBanner!!,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.success
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Ambient Study Soundscapes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Synthesized live on-device. Active while your sprint timer runs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                items(AmbientSoundMode.entries.size) { index ->
                    val mode = AmbientSoundMode.entries[index]
                    val active = selectedAmbientMode == mode
                    val badgeTint = if (active) colors.cyanPulse else colors.textSecondary
                    SilverBlueCard(
                        elevated = active,
                        onClick = {
                            feedback?.tap()
                            selectedAmbientMode = mode
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(ImtihanRadii.chip),
                                    color = badgeTint.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (mode == AmbientSoundMode.NONE) Icons.Outlined.VolumeOff else Icons.Outlined.GraphicEq,
                                            contentDescription = null,
                                            tint = badgeTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(mode.label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                            if (active) {
                                ImtihanStatusBadge(
                                    text = if (isRunning && mode != AmbientSoundMode.NONE) "Live" else "Selected",
                                    tint = colors.cyanPulse
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

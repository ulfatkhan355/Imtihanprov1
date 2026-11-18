package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.ui.audio.AmbientSoundMode
import com.imtihanpro.app.ui.audio.AmbientSoundscapePlayer
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.AnimatedProgressRing
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.ZeroPillMetaLine
import com.imtihanpro.app.ui.theme.LocalImtihanColors
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    ambientPlayer.stop()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Focus Pomodoro Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                    ZeroPillMetaLine("DEEP STUDY SPRINT + SOUNDWAVE SYNTHESIS")
                }
                Icon(
                    imageVector = Icons.Outlined.SelfImprovement,
                    contentDescription = null,
                    tint = colors.cyanPulse,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
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
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (active) colors.accentBlue else colors.cardSurface,
                                        border = BorderStroke(1.dp, if (active) colors.cyanPulse else colors.cardBorder)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            AnimatedProgressRing(
                                progress = progress,
                                size = 186.dp,
                                strokeWidth = 11.dp,
                                accentColor = if (isRunning) colors.cyanPulse else colors.accentBlue,
                                centerLabel = formattedTime,
                                centerCaption = when {
                                    remainingSeconds == 0 -> "SPRINT DONE"
                                    isRunning -> "FOCUSING"
                                    elapsedSeconds > 0 -> "PAUSED"
                                    else -> "READY"
                                }
                            )

                            Spacer(modifier = Modifier.height(22.dp))

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
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isRunning) "Pause Sprint" else if (elapsedSeconds > 0 && remainingSeconds > 0) "Resume" else "Start Sprint")
                                }

                                OutlinedButton(
                                    onClick = {
                                        feedback?.tap()
                                        isRunning = false
                                        remainingSeconds = selectedMinutes * 60
                                        lastXpBanner = null
                                    },
                                    modifier = Modifier.height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Reset timer")
                                }
                            }

                            if (elapsedSeconds >= 60 && remainingSeconds > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
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
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
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
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
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
                    Text(
                        text = "Zero-Asset Ambient Study Soundscapes",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Synthesized live on-device (adds 0 KB to APK). Active while your sprint timer runs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                items(AmbientSoundMode.entries.size) { index ->
                    val mode = AmbientSoundMode.entries[index]
                    val active = selectedAmbientMode == mode
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
                                Icon(
                                    imageVector = if (mode == AmbientSoundMode.NONE) Icons.Outlined.VolumeOff else Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    tint = if (active) colors.cyanPulse else colors.textSecondary
                                )
                                Column {
                                    Text(mode.label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                            if (active) {
                                ZeroPillMetaLine(if (isRunning && mode != AmbientSoundMode.NONE) "LIVE" else "SELECTED")
                            }
                        }
                    }
                }
            }
        }
    }
}

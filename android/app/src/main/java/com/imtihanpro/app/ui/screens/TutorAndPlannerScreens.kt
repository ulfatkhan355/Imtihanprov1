package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.MotivationSummaryUi
import com.imtihanpro.app.domain.model.StudyPlannerUi
import com.imtihanpro.app.domain.model.TutorResponseUi
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanHeroCard
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanPrimaryButton
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.ImtihanSecondaryButton
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.ImtihanTopBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Curriculum-Grounded AI Study Tutor Screen (Strict Server GEMINI_API_KEY + Honest Offline/503 Handling)
 */
@Composable
fun AiTutorScreen(
    chapterTitle: String?,
    classLevel: Int,
    boardCode: String,
    languageCode: String,
    tutorState: TutorResponseUi,
    onAskTutor: (action: String, prompt: String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var selectedAction by remember { mutableStateOf("explain_simple") }
    var promptDraft by remember { mutableStateOf("") }

    val quickActions = listOf(
        "explain_simple" to "Explain Simple",
        "step_by_step" to "Step-by-Step Numerical",
        "important_questions" to "Board Questions",
        "quick_summary" to "Quick Summary",
        "custom_question" to "Ask Anything"
    )
    val starterPrompts = remember(chapterTitle, classLevel) {
        val topic = chapterTitle ?: "this Class $classLevel chapter"
        listOf(
            "Summarize key KPK exam points for $topic",
            "Give 3 expected short board questions with answers",
            "Walk through a tough numerical step-by-step"
        )
    }

    SilverBlueCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            ImtihanTopBar(
                title = "AI Study Tutor",
                subtitle = "$boardCode Class $classLevel · ${chapterTitle ?: "General"} · $languageCode",
                onBack = onBack,
                showBottomDivider = true,
                actions = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = colors.warningGold,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickActions.forEach { (key, label) ->
                            val active = selectedAction == key
                            Surface(
                                onClick = { selectedAction = key },
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = if (active) colors.accentBlue else colors.cardSurface,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder),
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) Color.White else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = promptDraft,
                            onValueChange = { promptDraft = it },
                            label = { Text("Ask about this KPK Board chapter or paste a numerical") },
                            placeholder = { Text("e.g., Explain significant figures rules with 2 examples") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ImtihanPrimaryButton(
                            text = if (tutorState.isLoading) "Generating Explanation..." else "Ask Curriculum Tutor",
                            leadingIcon = Icons.Outlined.Send,
                            isLoading = tutorState.isLoading,
                            onClick = {
                                val effectivePrompt = promptDraft.ifBlank {
                                    "Provide ${selectedAction.replace('_', ' ')} for ${chapterTitle ?: "this KPK Class $classLevel chapter"}"
                                }
                                onAskTutor(selectedAction, effectivePrompt)
                            }
                        )
                    }
                }

                if (tutorState.isLoading) {
                    item {
                        ImtihanLoadingView(message = "Ground-checking KPK curriculum notes...")
                    }
                } else if (!tutorState.available && !tutorState.unavailableReason.isNullOrBlank()) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            ImtihanStatusBadge(text = "AI Tutor Unavailable", tint = colors.warningGold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tutorState.unavailableReason, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                } else if (!tutorState.answer.isNullOrBlank()) {
                    item {
                        SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                            ImtihanStatusBadge(
                                text = "Tutor Response · ${tutorState.model ?: "Gemini"}",
                                tint = colors.accentBlue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tutorState.answer, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            if (tutorState.groundedSources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Grounded Curriculum Sources", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                tutorState.groundedSources.forEach { src ->
                                    Text("• [${src.type}] ${src.title}", style = MaterialTheme.typography.bodySmall, color = colors.accentBlue)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Suggested Curriculum Prompts", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            starterPrompts.forEachIndexed { idx, starter ->
                                Surface(
                                    onClick = {
                                        promptDraft = starter
                                        onAskTutor(selectedAction, starter)
                                    },
                                    shape = RoundedCornerShape(ImtihanRadii.chip),
                                    color = colors.canvasDeep,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = starter,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.accentBlue,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Study Planner, Interactive Calendar, Streak & Exam Countdown Screen — matches reference screen #13
 */
@Composable
fun StudyPlannerScreen(
    planner: StudyPlannerUi,
    onAddTask: (title: String, plannedDateIso: String, minutes: Int, taskType: String) -> Unit,
    onToggleTask: (taskId: String, completed: Boolean) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onUpdateGoals: (dailyTargetMinutes: Int, examCountdownDateIso: String?, reminderHourLocal: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var taskTitle by remember { mutableStateOf("") }
    var targetMins by remember(planner.dailyTargetMinutes) { mutableIntStateOf(planner.dailyTargetMinutes) }
    var countdownDate by remember(planner.examCountdownDateIso) { mutableStateOf(planner.examCountdownDateIso ?: "") }

    var monthOffset by remember { mutableIntStateOf(0) }
    val todayCal = remember { Calendar.getInstance() }
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val monthTitleFormatter = remember { SimpleDateFormat("MMM yyyy", Locale.US) }
    var selectedDateIso by remember { mutableStateOf(isoFormatter.format(todayCal.time)) }

    val displayCal = remember(monthOffset) {
        (todayCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthHeaderLabel = remember(displayCal) { monthTitleFormatter.format(displayCal.time) }
    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekOffset = (displayCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    val scheduledDates = remember(planner.tasks) { planner.tasks.map { it.plannedDateIso }.toSet() }

    val dailyProgress = (planner.minutesLoggedToday.toFloat() / maxOf(1, planner.dailyTargetMinutes).toFloat()).coerceIn(0f, 1f)

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Study Planner",
                subtitle = "${planner.minutesLoggedToday} / ${planner.dailyTargetMinutes} min today",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Visual Interactive Calendar Card
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { monthOffset -= 1 },
                                modifier = Modifier.defaultMinSize(
                                    minWidth = ImtihanDimensions.minTouchTarget,
                                    minHeight = ImtihanDimensions.minTouchTarget
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.textPrimary)
                            }
                            Text(
                                text = monthHeaderLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            IconButton(
                                onClick = { monthOffset += 1 },
                                modifier = Modifier.defaultMinSize(
                                    minWidth = ImtihanDimensions.minTouchTarget,
                                    minHeight = ImtihanDimensions.minTouchTarget
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month", tint = colors.textPrimary)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val totalCells = firstDayOfWeekOffset + daysInMonth
                        val rowCount = (totalCells + 6) / 7
                        for (row in 0 until rowCount) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 7) {
                                    val cellIdx = row * 7 + col
                                    val dayNum = cellIdx - firstDayOfWeekOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val cellCal = (displayCal.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, dayNum)
                                        }
                                        val cellIso = isoFormatter.format(cellCal.time)
                                        val isSelected = cellIso == selectedDateIso
                                        val isExamDay = cellIso == planner.examCountdownDateIso
                                        val hasTask = cellIso in scheduledDates

                                        Surface(
                                            onClick = { selectedDateIso = cellIso },
                                            shape = CircleShape,
                                            color = when {
                                                isSelected -> colors.accentBlue
                                                isExamDay -> colors.warningGold.copy(alpha = 0.22f)
                                                hasTask -> colors.accentBlue.copy(alpha = 0.14f)
                                                else -> Color.Transparent
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                                    fontWeight = if (isSelected || isExamDay) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isExamDay -> colors.warningGold
                                                        else -> colors.textPrimary
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Goal Progress Card
                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Study Goal", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    text = "${planner.minutesLoggedToday} / ${planner.dailyTargetMinutes} minutes today",
                                    style = MaterialTheme.typography.titleLarge.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentBlue
                                )
                            }
                            ImtihanStatusBadge(
                                text = "${planner.currentStreakDays}d streak",
                                tint = colors.warningGold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ImtihanProgressBar(progress = dailyProgress, fillColor = colors.success)
                        if (!planner.examCountdownDateIso.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Board Exam Target Date: ${planner.examCountdownDateIso}",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Add Task for Selected Date
                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Today's Plan ($selectedDateIso)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = taskTitle,
                                onValueChange = { taskTitle = it },
                                placeholder = { Text("e.g., Revise Physics Ch 1 Numericals") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (taskTitle.isNotBlank()) {
                                        onAddTask(taskTitle, selectedDateIso, 25, "revision")
                                        taskTitle = ""
                                    }
                                },
                                enabled = taskTitle.isNotBlank(),
                                modifier = Modifier.defaultMinSize(
                                    minWidth = ImtihanDimensions.minTouchTarget,
                                    minHeight = 52.dp
                                ),
                                shape = RoundedCornerShape(ImtihanRadii.button),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = "Add task")
                            }
                        }
                    }
                }

                if (planner.tasks.isEmpty()) {
                    item {
                        ImtihanEmptyView(
                            title = "No Scheduled Tasks",
                            description = "Select a date on the calendar and add chapter readings, mock tests, or flashcard sessions."
                        )
                    }
                } else {
                    items(planner.tasks, key = { it.id }) { task ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Checkbox) { onToggleTask(task.id, !task.completed) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.completed,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(
                                        text = "${task.estimatedMinutes} min · ${task.taskType} · ${task.plannedDateIso.ifBlank { "Today" }}",
                                        style = MaterialTheme.typography.bodySmall.tabular(),
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTask(task.id) },
                                    modifier = Modifier.defaultMinSize(
                                        minWidth = ImtihanDimensions.minTouchTarget,
                                        minHeight = ImtihanDimensions.minTouchTarget
                                    )
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete task", tint = colors.danger)
                                }
                            }
                        }
                    }
                }

                // Study Goal & Board Countdown Settings
                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Daily Target & Board Countdown", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 45, 60, 90).forEach { mins ->
                                val active = targetMins == mins
                                Surface(
                                    onClick = { targetMins = mins },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp),
                                    shape = RoundedCornerShape(ImtihanRadii.chip),
                                    color = if (active) colors.accentBlue else colors.cardElevated,
                                    border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            style = MaterialTheme.typography.labelMedium.tabular(),
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = if (active) Color.White else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = countdownDate,
                            onValueChange = { countdownDate = it },
                            label = { Text("Board Exam Date (YYYY-MM-DD, optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ImtihanSecondaryButton(
                            text = "Save Goals",
                            onClick = { onUpdateGoals(targetMins, countdownDate.ifBlank { null }, planner.reminderHourLocal) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Motivation, Achievements, Weekly Challenges & Privacy-Safe Opt-In Leaderboard Screen
 */
@Composable
fun MotivationAndLeaderboardScreen(
    motivation: MotivationSummaryUi,
    onToggleLeaderboardOptIn: (Boolean, String?) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var aliasInput by remember(motivation.aliasHandle) { mutableStateOf(motivation.aliasHandle ?: "") }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Achievements & Leaderboard",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Level ${motivation.level}",
                                    style = MaterialTheme.typography.headlineSmall.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${motivation.xpTotal} XP · Next level at ${motivation.nextLevelXp} XP",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = ((motivation.xpTotal % 150) / 150f).coerceIn(0f, 1f)
                        ImtihanProgressBar(
                            progress = progress,
                            trackColor = Color.White.copy(alpha = 0.24f),
                            fillColor = Color(0xFFFDE047)
                        )
                    }
                }

                item {
                    Text("Challenges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                items(motivation.challenges, key = { it.id }) { ch ->
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ch.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            ImtihanStatusBadge(text = "+${ch.xpReward} XP", tint = colors.warningGold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ch.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ImtihanProgressBar(
                            progress = ch.currentCount.toFloat() / maxOf(1, ch.targetCount).toFloat(),
                            fillColor = if (ch.completed) colors.success else colors.accentBlue
                        )
                    }
                }

                item {
                    Text(
                        text = "Achievements (${motivation.achievements.count { it.unlocked }}/${motivation.achievements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                items(motivation.achievements, key = { it.code }) { ach ->
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (ach.unlocked) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Unlocked", tint = colors.success, modifier = Modifier.size(18.dp))
                                }
                                Text(ach.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            }
                            ImtihanStatusBadge(
                                text = if (ach.unlocked) "Unlocked" else "${ach.xpReward} XP",
                                tint = if (ach.unlocked) colors.success else colors.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ach.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Switch) {
                                    onToggleLeaderboardOptIn(!motivation.leaderboardOptIn, aliasInput.ifBlank { null })
                                }
                                .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Leaderboard, contentDescription = null, tint = colors.accentBlue, modifier = Modifier.size(18.dp))
                                    Text("Opt-In Board Leaderboard", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                }
                                Text("Privacy-safe: only your anonymized alias, XP & streak are visible.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Switch(
                                checked = motivation.leaderboardOptIn,
                                onCheckedChange = null
                            )
                        }
                        if (motivation.leaderboardOptIn) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = aliasInput,
                                onValueChange = { aliasInput = it },
                                label = { Text("Public Alias Handle") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (motivation.leaderboardOptIn && motivation.leaderboard.isNotEmpty()) {
                    items(motivation.leaderboard, key = { "${it.rank}_${it.aliasHandle}" }) { row ->
                        SilverBlueCard(elevated = row.isCurrentUser, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${row.rank} · ${row.aliasHandle}",
                                    style = MaterialTheme.typography.titleSmall.tabular(),
                                    fontWeight = if (row.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                    color = if (row.isCurrentUser) colors.accentBlue else colors.textPrimary
                                )
                                Text(
                                    text = "${row.xpTotal} XP · ${row.streakDays}d",
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    color = colors.warningGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Feedback & Content Error Report Screen
 */
@Composable
fun FeedbackReportScreen(
    subjectId: String?,
    chapterId: String?,
    statusMessage: String?,
    onSubmitFeedback: (category: String, title: String, description: String) -> Unit,
    onClearStatus: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var selectedCategory by remember { mutableStateOf("content_error") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf(
        "content_error" to "Content Error",
        "mcq_correction" to "MCQ Key Fix",
        "app_bug" to "App Bug",
        "feature_request" to "Feature Idea"
    )

    SilverBlueCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            ImtihanTopBar(
                title = "Report Issue & Feedback",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (key, label) ->
                            val active = selectedCategory == key
                            Surface(
                                onClick = { selectedCategory = key },
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = if (active) colors.accentBlue else colors.cardSurface,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder),
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) Color.White else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        if (!chapterId.isNullOrBlank() || !subjectId.isNullOrBlank()) {
                            ImtihanStatusBadge(
                                text = "Linked: ${subjectId ?: ""} ${chapterId ?: ""}".trim(),
                                tint = colors.accentBlue
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                onClearStatus()
                            },
                            label = { Text("Short Summary") },
                            placeholder = { Text("e.g., Typo in equation or wrong MCQ answer key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                onClearStatus()
                            },
                            label = { Text("Details & Suggested Correction") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ImtihanPrimaryButton(
                            text = "Submit Report",
                            enabled = title.isNotBlank() && description.isNotBlank(),
                            onClick = {
                                onSubmitFeedback(selectedCategory, title, description)
                                title = ""
                                description = ""
                            }
                        )
                        if (!statusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = colors.success.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, colors.success.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                                    Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = colors.success)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

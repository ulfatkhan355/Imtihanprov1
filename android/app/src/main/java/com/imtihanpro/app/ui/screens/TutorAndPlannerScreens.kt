package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.MotivationSummaryUi
import com.imtihanpro.app.domain.model.StudyPlannerUi
import com.imtihanpro.app.domain.model.TutorResponseUi
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.theme.LocalImtihanColors

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

    SilverBlueCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Study Tutor", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text(
                        text = "$boardCode Class $classLevel · ${chapterTitle ?: "General"} · $languageCode",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accentBlue
                    )
                }
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.warningGold, modifier = Modifier.padding(end = 12.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) colors.accentBlue else colors.cardSurface,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = promptDraft,
                        onValueChange = { promptDraft = it },
                        label = { Text("Ask about this KPK Board chapter or paste a numerical") },
                        placeholder = { Text("e.g., Explain significant figures rules with 2 examples") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val effectivePrompt = promptDraft.ifBlank {
                                "Provide ${selectedAction.replace('_', ' ')} for ${chapterTitle ?: "this KPK Class $classLevel chapter"}"
                            }
                            onAskTutor(selectedAction, effectivePrompt)
                        },
                        enabled = !tutorState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                    ) {
                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (tutorState.isLoading) "Generating Explanation..." else "Ask Curriculum Tutor")
                    }
                }

                if (tutorState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = colors.accentBlue)
                        }
                    }
                } else if (!tutorState.available && !tutorState.unavailableReason.isNullOrBlank()) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text("AI Tutor Currently Unavailable", style = MaterialTheme.typography.titleSmall, color = colors.warningGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(tutorState.unavailableReason, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                } else if (!tutorState.answer.isNullOrBlank()) {
                    item {
                        SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Tutor Response (${tutorState.model ?: "Gemini"})",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accentBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(tutorState.answer, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            if (tutorState.groundedSources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Grounded Curriculum Sources:", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                tutorState.groundedSources.forEach { src ->
                                    Text("• [${src.type}] ${src.title}", style = MaterialTheme.typography.bodySmall, color = colors.accentBlue)
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
 * Study Planner, Streak & Exam Countdown Screen
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

    val dailyProgress = (planner.minutesLoggedToday.toFloat() / maxOf(1, planner.dailyTargetMinutes).toFloat()).coerceIn(0f, 1f)

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Study Planner & Goals", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Study Goal", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    text = "${planner.minutesLoggedToday} / ${planner.dailyTargetMinutes} minutes today",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.accentBlue
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = colors.warningGold)
                                Text("${planner.currentStreakDays}d streak", style = MaterialTheme.typography.labelMedium, color = colors.warningGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ImtihanProgressBar(progress = dailyProgress, fillColor = colors.success)
                        if (!planner.examCountdownDateIso.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Board Exam Target Date: ${planner.examCountdownDateIso}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Study Goal & Board Countdown", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 45, 60, 90).forEach { mins ->
                                val active = targetMins == mins
                                Surface(
                                    onClick = { targetMins = mins },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) colors.accentBlue else colors.cardElevated,
                                    border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = countdownDate,
                            onValueChange = { countdownDate = it },
                            label = { Text("Board Exam Date (YYYY-MM-DD, optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onUpdateGoals(targetMins, countdownDate.ifBlank { null }, planner.reminderHourLocal) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Goals")
                        }
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Add Study Task", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
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
                                        onAddTask(taskTitle, "", 25, "revision")
                                        taskTitle = ""
                                    }
                                },
                                enabled = taskTitle.isNotBlank(),
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
                            description = "Add chapter readings, mock tests, or flashcard sessions above."
                        )
                    }
                } else {
                    items(planner.tasks, key = { it.id }) { task ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.completed,
                                    onCheckedChange = { onToggleTask(task.id, it) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text("${task.estimatedMinutes} min · ${task.taskType}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                                IconButton(onClick = { onDeleteTask(task.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete task", tint = colors.danger)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Achievements & Leaderboard", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Level ${motivation.level}", style = MaterialTheme.typography.headlineSmall, color = colors.accentBlue)
                                Text("${motivation.xpTotal} XP · Next level at ${motivation.nextLevelXp} XP", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = colors.warningGold, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val progress = ((motivation.xpTotal % 150) / 150f).coerceIn(0f, 1f)
                        ImtihanProgressBar(progress = progress, fillColor = colors.warningGold)
                    }
                }

                item {
                    Text("Challenges", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                items(motivation.challenges, key = { it.id }) { ch ->
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ch.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("+${ch.xpReward} XP", style = MaterialTheme.typography.labelMedium, color = colors.warningGold)
                        }
                        Text(ch.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        ImtihanProgressBar(
                            progress = ch.currentCount.toFloat() / maxOf(1, ch.targetCount).toFloat(),
                            fillColor = if (ch.completed) colors.success else colors.accentBlue
                        )
                    }
                }

                item {
                    Text("Achievements (${motivation.achievements.count { it.unlocked }}/${motivation.achievements.size})", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                items(motivation.achievements, key = { it.code }) { ach ->
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ach.title, style = MaterialTheme.typography.titleSmall, color = if (ach.unlocked) colors.success else colors.textPrimary)
                            Text(if (ach.unlocked) "Unlocked" else "${ach.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = if (ach.unlocked) colors.success else colors.textMuted)
                        }
                        Text(ach.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Leaderboard, contentDescription = null, tint = colors.accentBlue, modifier = Modifier.size(18.dp))
                                    Text("Opt-In Board Leaderboard", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                }
                                Text("Privacy-safe: only your anonymized alias, XP & streak are visible.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Switch(
                                checked = motivation.leaderboardOptIn,
                                onCheckedChange = { onToggleLeaderboardOptIn(it, aliasInput.ifBlank { null }) }
                            )
                        }
                        if (motivation.leaderboardOptIn) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("#${row.rank} · ${row.aliasHandle}", style = MaterialTheme.typography.titleSmall, color = if (row.isCurrentUser) colors.accentBlue else colors.textPrimary)
                                Text("${row.xpTotal} XP · ${row.streakDays}d", style = MaterialTheme.typography.labelMedium, color = colors.warningGold)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Report Issue & Feedback", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) colors.accentBlue else colors.cardSurface,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        if (!chapterId.isNullOrBlank() || !subjectId.isNullOrBlank()) {
                            Text(
                                text = "Linked context: ${subjectId ?: ""} ${chapterId ?: ""}".trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accentBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onSubmitFeedback(selectedCategory, title, description)
                                title = ""
                                description = ""
                            },
                            enabled = title.isNotBlank() && description.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                        ) {
                            Text("Submit Report")
                        }
                        if (!statusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = colors.success)
                        }
                    }
                }
            }
        }
    }
}

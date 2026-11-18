package com.imtihanpro.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.BoardOption
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ClassLevelOption
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.SearchResultsUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.AnimatedProgressRing
import com.imtihanpro.app.ui.components.ImtihanBrandEmblem
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.OfflineModeBanner
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.SubjectVectorIcon
import com.imtihanpro.app.ui.components.ZeroPillMetaLine
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.viewmodel.AppShellState
import kotlinx.coroutines.delay

/**
 * Screen 1: Splash Screen
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val colors = LocalImtihanColors.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    var emblemVisible by remember { mutableStateOf(false) }
    val emblemScale by animateFloatAsState(
        targetValue = if (emblemVisible) 1f else 0.72f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "splash_scale"
    )
    LaunchedEffect(Unit) {
        emblemVisible = true
        delay(850)
        latestOnFinished()
    }

    SilverBlueCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.cardElevated,
                border = BorderStroke(1.dp, colors.cyanPulse.copy(alpha = 0.45f)),
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = emblemScale
                        scaleY = emblemScale
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ImtihanBrandEmblem(modifier = Modifier.size(64.dp))
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            ZeroPillMetaLine("KPK BISE STUDY OS · 2050 EDITION")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Imtihan Pro",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Classes 9–12 · Offline-First · Zero Distractions",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Study Smart,\nScore Top in KPK!",
                style = MaterialTheme.typography.titleMedium,
                color = colors.cyanPulse,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(34.dp))
            ImtihanProgressBar(progress = 0.78f, modifier = Modifier.width(176.dp))
        }
    }
}

/**
 * Screen 2: First-Time Setup (Guest-First Mandatory, KPK BISE Sub-Board, Class 9-12, Study Group, Language)
 */
@Composable
fun FirstTimeSetupScreen(
    initialClass: Int = 9,
    initialBoard: String = BoardOption.KPK.code,
    initialSubBoard: String = "PESHAWAR",
    initialStudyGroup: String = "SCIENCE_BIOLOGY",
    initialLanguage: String = "English",
    initialName: String = "",
    canNavigateBack: Boolean = false,
    onBack: () -> Unit = {},
    onStartAsGuest: (
        classLevel: Int,
        boardCode: String,
        subBoardCode: String,
        studyGroupCode: String,
        languageCode: String,
        name: String,
        launchDiagnostic: Boolean
    ) -> Unit,
    onContinueWithGoogle: (
        classLevel: Int,
        boardCode: String,
        subBoardCode: String,
        studyGroupCode: String,
        languageCode: String,
        name: String
    ) -> Unit
) {
    val colors = LocalImtihanColors.current
    var selectedClass by remember(initialClass) { mutableIntStateOf(initialClass) }
    var selectedBoard by remember(initialBoard) { mutableStateOf(initialBoard) }
    var selectedSubBoard by remember(initialSubBoard) { mutableStateOf(initialSubBoard) }
    var selectedGroup by remember(initialStudyGroup, selectedClass) {
        val validGroups = Stage3To8Engines.studyGroupsForClass(selectedClass).map { it.code }
        mutableStateOf(
            if (initialStudyGroup in validGroups) initialStudyGroup
            else Stage3To8Engines.defaultStudyGroupForClass(selectedClass)
        )
    }
    var selectedLanguage by remember(initialLanguage) { mutableStateOf(initialLanguage) }
    var studentName by remember(initialName) { mutableStateOf(initialName) }
    var startWithDiagnostic by remember { mutableStateOf(false) }

    val availableGroups = remember(selectedClass) { Stage3To8Engines.studyGroupsForClass(selectedClass) }

    SilverBlueCanvas {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (canNavigateBack) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                        Text(
                            text = "Update Study Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Personalize KPK Study Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Start learning immediately as a guest. Your notes, quizzes, flashcards, and progress stay saved on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Class",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClassLevelOption.entries.forEach { option ->
                            val selected = selectedClass == option.level
                            Surface(
                                onClick = {
                                    selectedClass = option.level
                                    val valid = Stage3To8Engines.studyGroupsForClass(option.level).map { it.code }
                                    if (selectedGroup !in valid) {
                                        selectedGroup = Stage3To8Engines.defaultStudyGroupForClass(option.level)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.accentBlue else colors.cardElevated,
                                border = BorderStroke(1.dp, if (selected) colors.accentBlue else colors.cardBorder)
                            ) {
                                Text(
                                    text = option.displayLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (selectedClass) {
                            9 -> "Primary starter release: Class 9 KPK English-medium subjects & BISE model papers active."
                            10 -> "Class 10 KPK sample curriculum & Physics/Maths past papers available."
                            else -> "Class $selectedClass structure is ready; initial starter chapters prioritize Classes 9 & 10 KPK."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedClass >= 11) colors.warningGold else colors.success
                    )
                }
            }

            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Study Group",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableGroups.forEach { grp ->
                            val selected = selectedGroup == grp.code
                            Surface(
                                onClick = { selectedGroup = grp.code },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.accentBlue.copy(alpha = 0.2f) else colors.cardElevated,
                                border = BorderStroke(1.dp, if (selected) colors.accentBlue else colors.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(grp.label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(grp.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Curriculum Board & BISE Region",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoardOption.entries.forEach { board ->
                            val selected = selectedBoard == board.code
                            Surface(
                                onClick = { if (board.isActiveInStage1) selectedBoard = board.code },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.accentBlue.copy(alpha = 0.2f) else colors.cardElevated,
                                border = BorderStroke(1.dp, if (selected) colors.accentBlue else colors.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = board.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = if (board.isActiveInStage1) "Active" else "Planned",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (board.isActiveInStage1) colors.success else colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    if (selectedBoard == "KPK") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Choose KPK BISE Division",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accentBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Stage3To8Engines.KPK_SUB_BOARDS.forEach { sub ->
                                val active = selectedSubBoard == sub.code
                                Surface(
                                    onClick = { selectedSubBoard = sub.code },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) colors.accentBlue else colors.cardElevated,
                                    border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                                ) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Medium & Display Name",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Urdu").forEach { lang ->
                            val active = selectedLanguage == lang
                            Surface(
                                onClick = { selectedLanguage = lang },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) colors.accentBlue else colors.cardElevated,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                            ) {
                                Text(
                                    text = "$lang Medium",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        placeholder = { Text("Guest Learner") },
                        label = { Text("Display Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!canNavigateBack) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = startWithDiagnostic,
                                onCheckedChange = { startWithDiagnostic = it }
                            )
                            Text(
                                text = "Take 5-min baseline diagnostic test right after setup",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onStartAsGuest(
                            selectedClass,
                            selectedBoard,
                            selectedSubBoard,
                            selectedGroup,
                            selectedLanguage,
                            studentName,
                            startWithDiagnostic
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                ) {
                    Text("Continue as Guest", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onContinueWithGoogle(
                            selectedClass,
                            selectedBoard,
                            selectedSubBoard,
                            selectedGroup,
                            selectedLanguage,
                            studentName
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Text(
                        text = "Sign in with Google (Optional Sync)",
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Screen 3: Home Dashboard
 */
@Composable
fun HomeDashboardScreen(
    shellState: AppShellState,
    subjectsState: UiResourceState<List<SubjectUiModel>>,
    searchQuery: String,
    searchResults: SearchResultsUi = SearchResultsUi(),
    continueChapterTitle: String = "Chapter 1: Physical Quantities & Measurement",
    feedback: ImtihanFeedbackController? = null,
    onSearchChange: (String) -> Unit,
    onContinueLearning: () -> Unit,
    onViewAllSubjects: () -> Unit,
    onOpenSubject: (SubjectUiModel) -> Unit,
    onOpenSearchedChapter: (ChapterUiModel) -> Unit = {},
    onOpenSearchedPdf: (PdfResourceUi) -> Unit = {},
    onOpenMcqPractice: () -> Unit,
    onOpenAiTutor: () -> Unit = {},
    onOpenSmartInsights: () -> Unit = {},
    onOpenStudyPlanner: () -> Unit = {},
    onOpenMistakeMaster: () -> Unit = {},
    onOpenBookmarksFlashcards: () -> Unit = {},
    onOpenDownloads: () -> Unit,
    onOpenProfile: () -> Unit,
    onToggleSound: () -> Unit = {},
    onToggleMedium: () -> Unit = {},
    onStartRapidFire: () -> Unit = {},
    onOpenFocusTimer: () -> Unit = {},
    onShareStreak: () -> Unit = {},
    onBannerDisplayed: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    val metrics = shellState.realHomeMetrics
    val activeChapterTitle = metrics.continueChapterTitle ?: continueChapterTitle
    val activeGroupLabel = Stage3To8Engines.STUDY_GROUPS
        .firstOrNull { it.code == shellState.studyGroupCode }?.label ?: shellState.studyGroupCode
    val daysToExam = remember(shellState.examCountdownDateIso) {
        Stage3To8Engines.computeDaysUntilExam(shellState.examCountdownDateIso)
    }
    val isUrdu = shellState.languageCode.equals("Urdu", ignoreCase = true)

    val showHomeAd = Stage3To8Engines.shouldDisplayAd(
        policy = shellState.adPolicy,
        surface = "home",
        isInActiveQuizOrExam = false,
        isReadingNotesOrPdf = false,
        bannersShownToday = shellState.bannersShownToday
    )

    LaunchedEffect(showHomeAd) {
        if (showHomeAd) onBannerDisplayed()
    }

    SilverBlueCanvas {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OfflineModeBanner(visible = shellState.isOfflineSimulated)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ImtihanBrandEmblem(modifier = Modifier.size(40.dp))
                        Column {
                            Text(
                                text = "Assalam Alaikum · Class ${shellState.selectedClassLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.cyanPulse
                            )
                            Text(
                                text = shellState.profile.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                feedback?.tap()
                                onToggleMedium()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUrdu) colors.accentBlue else colors.cardElevated,
                            border = BorderStroke(1.dp, if (isUrdu) colors.cyanPulse else colors.cardBorder)
                        ) {
                            Text(
                                text = if (isUrdu) "اردو" else "EN",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isUrdu) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                        IconButton(onClick = {
                            onToggleSound()
                            feedback?.tap()
                        }) {
                            Icon(
                                imageVector = if (shellState.soundEffectsEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                contentDescription = if (shellState.soundEffectsEnabled) "Mute SFX" else "Enable SFX",
                                tint = if (shellState.soundEffectsEnabled) colors.cyanPulse else colors.textMuted
                            )
                        }
                        Surface(
                            onClick = onOpenProfile,
                            shape = CircleShape,
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.cyanPulse.copy(alpha = 0.55f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = shellState.profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.accentBlue
                                )
                            }
                        }
                    }
                }
            }

            item {
                ZeroPillMetaLine(
                    text = "${shellState.selectedBoardCode} · ${shellState.selectedSubBoardCode} · $activeGroupLabel · ${shellState.languageCode.uppercase()} MEDIUM"
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search subjects, chapters, topics or MCQs",
                            tint = colors.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    placeholder = { Text("Search subjects, chapters, topics, MCQs, PDFs...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.trim().length >= 2) {
                item {
                    SearchResultsPanel(
                        results = searchResults,
                        onOpenSubject = onOpenSubject,
                        onOpenChapter = onOpenSearchedChapter,
                        onOpenMcqPractice = onOpenMcqPractice,
                        onOpenPdf = onOpenSearchedPdf
                    )
                }
            }

            item {
                SilverBlueCard(
                    elevated = true,
                    onClick = {
                        feedback?.tap()
                        onContinueLearning()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ZeroPillMetaLine("RESUME STUDY COMMAND DECK")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeChapterTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (metrics.continueChapterPercent > 0) {
                                    "${metrics.continueChapterPercent}% Complete · Tap to continue"
                                } else {
                                    "Ready for Class ${shellState.selectedClassLevel} KPK board mastery"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = colors.accentBlue,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Resume chapter",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ImtihanProgressBar(progress = metrics.continueChapterPercent / 100f)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SilverBlueCard(
                        elevated = true,
                        onClick = {
                            feedback?.tap()
                            onStartRapidFire()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = colors.warningGold)
                            Column {
                                Text("5-Min Rapid Fire", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("Past-paper MCQ sprint", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                    SilverBlueCard(
                        elevated = true,
                        onClick = {
                            feedback?.tap()
                            onOpenFocusTimer()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = colors.cyanPulse)
                            Column {
                                Text("Focus Studio", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("Pomodoro + Rain audio", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subjects",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Surface(
                        onClick = onViewAllSubjects,
                        color = colors.canvasDeep.copy(alpha = 0f)
                    ) {
                        Text(
                            text = "View All >",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accentBlue,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (subjectsState) {
                    is UiResourceState.Loading -> ImtihanLoadingView()
                    is UiResourceState.Error -> ImtihanErrorView(subjectsState.message, onRetry = {})
                    is UiResourceState.Empty -> ImtihanEmptyView(subjectsState.title, subjectsState.description)
                    is UiResourceState.Success -> {
                        val filtered = subjectsState.data.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filtered, key = { it.id }) { subject ->
                                SilverBlueCard(
                                    onClick = {
                                        feedback?.tap()
                                        onOpenSubject(subject)
                                    },
                                    modifier = Modifier.width(156.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SubjectVectorIcon(
                                            iconKey = subject.iconKey,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "${subject.progressPercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.cyanPulse
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${subject.totalChapters} Chapters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ImtihanProgressBar(progress = subject.progressPercent / 100f)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SilverBlueCard(
                    onClick = onOpenSmartInsights,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedProgressRing(
                            progress = shellState.profile.overallProgressPercent / 100f,
                            size = 78.dp,
                            strokeWidth = 7.dp,
                            centerLabel = "${shellState.profile.overallProgressPercent}%",
                            centerCaption = "SYLLABUS"
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            ZeroPillMetaLine(
                                text = daysToExam?.let { "$it DAYS TO BISE EXAM" } ?: "BISE EXAM COUNTDOWN READY"
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${metrics.currentStreakDays}d Streak · ${metrics.xpTotal} XP",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Today's Goal: ${shellState.profile.dailyGoalPercent}% (${shellState.dailyTargetMinutes}m target)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                feedback?.tap()
                                onShareStreak()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.cyanPulse)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Streak Card", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onOpenStudyPlanner,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accentBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(daysToExam?.let { "${it}d Left" } ?: "Exam Date", maxLines = 1)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Smart Study Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard(
                            title = "Practice & Mocks",
                            subtitle = "OMR & board patterns",
                            icon = Icons.Outlined.Quiz,
                            tint = colors.accentBlue,
                            onClick = onOpenMcqPractice,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "AI Study Tutor",
                            subtitle = "Curriculum-grounded helper",
                            icon = Icons.Outlined.AutoAwesome,
                            tint = colors.warningGold,
                            onClick = onOpenAiTutor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard(
                            title = "Smart Insights",
                            subtitle = "Honest readiness radar",
                            icon = Icons.Outlined.Insights,
                            tint = colors.success,
                            onClick = onOpenSmartInsights,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Study Planner",
                            subtitle = "Daily goals & tasks",
                            icon = Icons.Outlined.CalendarToday,
                            tint = colors.accentBlue,
                            onClick = onOpenStudyPlanner,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard(
                            title = "Mistake Master",
                            subtitle = "Fix missed exam MCQs",
                            icon = Icons.Outlined.ReportProblem,
                            tint = colors.danger,
                            onClick = onOpenMistakeMaster,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "3D Flashcards",
                            subtitle = "Flip & swipe SM-2 deck",
                            icon = Icons.Outlined.BookmarkBorder,
                            tint = colors.warningGold,
                            onClick = onOpenBookmarksFlashcards,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard(
                            title = "Offline Downloads",
                            subtitle = "Saved notes, books & PDFs",
                            icon = Icons.Outlined.CloudDownload,
                            tint = colors.success,
                            onClick = onOpenDownloads,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (showHomeAd) {
                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sponsored Learning Partner",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Text(
                            text = shellState.adPolicy.providerNote.ifBlank {
                                "Non-intrusive academic partner slot (never shown during quizzes or reading)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderTagChip(text: String) {
    val colors = LocalImtihanColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchResultsPanel(
    results: SearchResultsUi,
    onOpenSubject: (SubjectUiModel) -> Unit,
    onOpenChapter: (ChapterUiModel) -> Unit,
    onOpenMcqPractice: () -> Unit,
    onOpenPdf: (PdfResourceUi) -> Unit
) {
    val colors = LocalImtihanColors.current
    val totalCount = results.subjects.size + results.chapters.size + results.topics.size + results.mcqs.size + results.pdfs.size
    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (totalCount == 0) "No curriculum matches for \"${results.query}\"" else "Search Results ($totalCount)",
            style = MaterialTheme.typography.titleSmall,
            color = colors.accentBlue
        )
        if (results.subjects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            results.subjects.take(3).forEach { sub ->
                TextButton(onClick = { onOpenSubject(sub) }) {
                    Text("Subject: ${sub.name} (Class ${sub.classLevel})", color = colors.textPrimary)
                }
            }
        }
        if (results.chapters.isNotEmpty()) {
            results.chapters.take(4).forEach { ch ->
                TextButton(onClick = { onOpenChapter(ch) }) {
                    Text("Chapter ${ch.chapterNumber}: ${ch.title}", color = colors.textPrimary)
                }
            }
        }
        if (results.topics.isNotEmpty()) {
            results.topics.take(3).forEach { (chTitle, top) ->
                TextButton(onClick = onOpenMcqPractice) {
                    Text("Topic: ${top.title} ($chTitle)", color = colors.textSecondary)
                }
            }
        }
        if (results.mcqs.isNotEmpty()) {
            Text(
                text = "${results.mcqs.size} matching practice MCQ(s) found",
                style = MaterialTheme.typography.labelMedium,
                color = colors.success,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (results.pdfs.isNotEmpty()) {
            results.pdfs.take(2).forEach { pdf ->
                TextButton(onClick = { onOpenPdf(pdf) }) {
                    Text("PDF: ${pdf.title}", color = colors.accentBlue)
                }
            }
        }
    }
}

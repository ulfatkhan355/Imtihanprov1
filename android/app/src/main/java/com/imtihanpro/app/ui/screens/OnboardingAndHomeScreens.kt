package com.imtihanpro.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.imtihanpro.app.ui.components.ImtihanMetricTile
import com.imtihanpro.app.ui.components.ImtihanPrimaryButton
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.ImtihanSecondaryButton
import com.imtihanpro.app.ui.components.ImtihanSectionHeader
import com.imtihanpro.app.ui.components.ImtihanSkeletonList
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.OfflineModeBanner
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.SubjectVectorIcon
import com.imtihanpro.app.ui.components.ZeroPillMetaLine
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.viewmodel.AppShellState
import kotlinx.coroutines.delay

/**
 * Screen 1: Splash Screen (Apple-Inspired Minimal Silver Blue Launch)
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val colors = LocalImtihanColors.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    var emblemVisible by remember { mutableStateOf(false) }
    val emblemScale by animateFloatAsState(
        targetValue = if (emblemVisible) 1f else 0.78f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "splash_scale"
    )
    LaunchedEffect(Unit) {
        emblemVisible = true
        delay(800)
        latestOnFinished()
    }

    SilverBlueCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ImtihanSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .size(92.dp)
                    .graphicsLayer {
                        scaleX = emblemScale
                        scaleY = emblemScale
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ImtihanBrandEmblem(modifier = Modifier.size(58.dp))
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Imtihan Pro",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Study · Practice · Excel",
                style = MaterialTheme.typography.titleSmall,
                color = colors.accentBlue
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Class 11 KPK Board Preparation",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            ImtihanProgressBar(progress = 0.75f, modifier = Modifier.width(160.dp))
        }
    }
}

/**
 * Screen 2: Onboarding & KPK Setup (Guest-First, Class 11 Active + 9/10/12 Coming Soon, 8 KPK BISE Boards)
 */
@Composable
fun FirstTimeSetupScreen(
    initialClass: Int = 11,
    initialBoard: String = BoardOption.KPK.code,
    initialSubBoard: String = "PESHAWAR",
    initialStudyGroup: String = "PRE_MEDICAL",
    initialLanguage: String = "English",
    initialDailyTargetMinutes: Int = 45,
    initialName: String = "",
    canNavigateBack: Boolean = false,
    onBack: () -> Unit = {},
    onUpdateDailyTarget: (Int) -> Unit = {},
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
    var selectedClass by remember(initialClass) {
        mutableIntStateOf(11)
    }
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
    var selectedDailyGoal by remember(initialDailyTargetMinutes) { mutableIntStateOf(initialDailyTargetMinutes) }
    var studentName by remember(initialName) { mutableStateOf(initialName) }
    var startWithDiagnostic by remember { mutableStateOf(false) }
    var comingSoonNotice by remember { mutableStateOf<String?>(null) }

    val availableGroups = remember(selectedClass) { Stage3To8Engines.studyGroupsForClass(selectedClass) }

    SilverBlueCanvas {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(ImtihanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ImtihanSpacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canNavigateBack) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (canNavigateBack) "Study Preferences" else "Welcome to Imtihan Pro",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Choose your KPK board, group & daily goal. No account needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    if (!canNavigateBack) {
                        TextButton(
                            onClick = {
                                onUpdateDailyTarget(selectedDailyGoal)
                                onStartAsGuest(
                                    selectedClass,
                                    selectedBoard,
                                    selectedSubBoard,
                                    selectedGroup,
                                    selectedLanguage,
                                    studentName,
                                    false
                                )
                            }
                        ) {
                            Text("Skip", color = colors.accentBlue)
                        }
                    }
                }
            }

            // 2x2 Class Selection (Class 11 Active; 9, 10, 12 Coming Soon)
            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Your Class",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        ImtihanStatusBadge(text = "HSSC-I Ready", tint = colors.success)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val classRows = ClassLevelOption.entries.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        classRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { option ->
                                    val available = option.isAvailableInCurrentRelease
                                    val selected = available && selectedClass == option.level
                                    Surface(
                                        onClick = {
                                            if (available) {
                                                comingSoonNotice = null
                                                selectedClass = option.level
                                                val valid = Stage3To8Engines.studyGroupsForClass(option.level).map { it.code }
                                                if (selectedGroup !in valid) {
                                                    selectedGroup = Stage3To8Engines.defaultStudyGroupForClass(option.level)
                                                }
                                            } else {
                                                comingSoonNotice = "${option.displayLabel} curriculum is coming soon. Class 11 KPK is fully active right now."
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 68.dp),
                                        shape = RoundedCornerShape(ImtihanRadii.button),
                                        color = when {
                                            selected -> colors.accentBlue
                                            available -> colors.cardElevated
                                            else -> colors.cardSurface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) colors.accentBlue else colors.cardBorder
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${option.level}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    selected -> MaterialTheme.colorScheme.onPrimary
                                                    available -> colors.textPrimary
                                                    else -> colors.textMuted
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (available) option.displayLabel else "${option.displayLabel} · Coming Soon",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when {
                                                    selected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                    available -> colors.accentBlue
                                                    else -> colors.textMuted
                                                },
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (comingSoonNotice != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = comingSoonNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warningGold
                        )
                    }
                }
            }

            // 8 KPK BISE Boards in crisp 2-column grid matching mockup
            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Your Board (KPK BISE)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All 8 Khyber Pakhtunkhwa BISE divisions supported",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val boardRows = Stage3To8Engines.KPK_SUB_BOARDS.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        boardRows.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { sub ->
                                    val active = selectedSubBoard == sub.code
                                    Surface(
                                        onClick = {
                                            selectedBoard = BoardOption.KPK.code
                                            selectedSubBoard = sub.code
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                        shape = RoundedCornerShape(ImtihanRadii.button),
                                        color = if (active) colors.accentBlue else colors.cardElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (active) colors.accentBlue else colors.cardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountBalance,
                                                contentDescription = null,
                                                tint = if (active) MaterialTheme.colorScheme.onPrimary else colors.accentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = sub.name.replace("BISE ", ""),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Study Group
            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Study Group",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableGroups.forEach { grp ->
                            val selected = selectedGroup == grp.code
                            Surface(
                                onClick = { selectedGroup = grp.code },
                                shape = RoundedCornerShape(ImtihanRadii.button),
                                color = if (selected) colors.accentBlue.copy(alpha = 0.14f) else colors.cardElevated,
                                border = BorderStroke(1.dp, if (selected) colors.accentBlue else colors.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = grp.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = grp.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = colors.accentBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Language, Daily Goal & Optional Name
            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Medium, Study Goal & Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Urdu").forEach { lang ->
                            val active = selectedLanguage == lang
                            Surface(
                                onClick = { selectedLanguage = lang },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                shape = RoundedCornerShape(ImtihanRadii.button),
                                color = if (active) colors.accentBlue else colors.cardElevated,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (lang == "Urdu") "اردو (Urdu)" else "English Medium",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Daily Study Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 45, 60, 90).forEach { mins ->
                            val active = selectedDailyGoal == mins
                            Surface(
                                onClick = { selectedDailyGoal = mins },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 42.dp),
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = if (active) colors.accentBlue else colors.cardElevated,
                                border = BorderStroke(1.dp, if (active) colors.accentBlue else colors.cardBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        placeholder = { Text("Guest Learner") },
                        label = { Text("Display Name (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(ImtihanRadii.button),
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
                                text = "Start with 5-minute diagnostic baseline quiz",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item {
                ImtihanPrimaryButton(
                    text = if (canNavigateBack) "Save Preferences" else "Start Learning as Guest",
                    onClick = {
                        onUpdateDailyTarget(selectedDailyGoal)
                        onStartAsGuest(
                            selectedClass,
                            selectedBoard,
                            selectedSubBoard,
                            selectedGroup,
                            selectedLanguage,
                            studentName,
                            startWithDiagnostic
                        )
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ImtihanSecondaryButton(
                    text = "Sign in with Google (Optional Sync)",
                    onClick = {
                        onUpdateDailyTarget(selectedDailyGoal)
                        onContinueWithGoogle(
                            selectedClass,
                            selectedBoard,
                            selectedSubBoard,
                            selectedGroup,
                            selectedLanguage,
                            studentName
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Screen 3: Home Dashboard (Apple-Inspired Minimalist Hierarchy Matching Reference Mockup)
 */
@Composable
fun HomeDashboardScreen(
    shellState: AppShellState,
    subjectsState: UiResourceState<List<SubjectUiModel>>,
    searchQuery: String,
    searchResults: SearchResultsUi = SearchResultsUi(),
    continueChapterTitle: String = "Chapter 1: Measurements",
    feedback: ImtihanFeedbackController? = null,
    onSearchChange: (String) -> Unit,
    onContinueLearning: () -> Unit,
    onViewAllSubjects: () -> Unit,
    onOpenSubject: (SubjectUiModel) -> Unit,
    onOpenSearchedChapter: (ChapterUiModel) -> Unit = {},
    onOpenSearchedPdf: (PdfResourceUi) -> Unit = {},
    onOpenMcqPractice: () -> Unit,
    onOpenPapers: () -> Unit = {},
    onOpenTextbooks: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenClassBoardSetup: () -> Unit = {},
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
    val daysToExam = remember(shellState.examCountdownDateIso) {
        Stage3To8Engines.computeDaysUntilExam(shellState.examCountdownDateIso)
    }
    val isUrdu = shellState.languageCode.equals("Urdu", ignoreCase = true)
    val subBoardDisplay = Stage3To8Engines.KPK_SUB_BOARDS
        .firstOrNull { it.code == shellState.selectedSubBoardCode }
        ?.name?.replace("BISE ", "") ?: shellState.selectedSubBoardCode

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

    val studyTimeDisplay = remember(metrics.todayStudyMinutes, shellState.profile.studyHours) {
        val mins = metrics.todayStudyMinutes
        when {
            mins >= 60 -> "${"%.1f".format(mins / 60f)}h"
            mins > 0 -> "${mins}m"
            shellState.profile.studyHours > 0 -> "${shellState.profile.studyHours}h"
            else -> "0m"
        }
    }

    SilverBlueCanvas {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(ImtihanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ImtihanSpacing.md)
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
                        ImtihanBrandEmblem(modifier = Modifier.size(38.dp))
                        Column {
                            Text(
                                text = "Assalam O Alaikum,",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = shellState.profile.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                            shape = RoundedCornerShape(ImtihanRadii.chip),
                            color = if (isUrdu) colors.accentBlue else colors.cardSurface,
                            border = BorderStroke(1.dp, if (isUrdu) colors.accentBlue else colors.cardBorder)
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
                                tint = if (shellState.soundEffectsEnabled) colors.accentBlue else colors.textMuted
                            )
                        }
                        Surface(
                            onClick = onOpenProfile,
                            shape = CircleShape,
                            color = colors.accentBlue.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, colors.accentBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = shellState.profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentBlue
                                )
                            }
                        }
                    }
                }
            }

            // Class & Board selector row matching mockup
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectorHeaderChip(
                        text = "Class ${shellState.selectedClassLevel}",
                        onClick = onOpenClassBoardSetup,
                        modifier = Modifier.weight(1f)
                    )
                    SelectorHeaderChip(
                        text = subBoardDisplay,
                        onClick = onOpenClassBoardSetup,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }

            // 4-Tile Study Overview Strip (100% Real Recorded Telemetry)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImtihanMetricTile(
                        icon = Icons.Outlined.Schedule,
                        iconTint = colors.accentBlue,
                        value = studyTimeDisplay,
                        label = "Study",
                        modifier = Modifier.weight(1f)
                    )
                    ImtihanMetricTile(
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = colors.success,
                        value = "${metrics.totalQuestionsAttempted}",
                        label = "Q's",
                        modifier = Modifier.weight(1f)
                    )
                    ImtihanMetricTile(
                        icon = Icons.Outlined.TrackChanges,
                        iconTint = colors.warningGold,
                        value = if (metrics.totalQuestionsAttempted > 0) "${metrics.averageAccuracyPercent}%" else "—",
                        label = "Accuracy",
                        modifier = Modifier.weight(1f)
                    )
                    ImtihanMetricTile(
                        icon = Icons.Outlined.LocalFireDepartment,
                        iconTint = colors.danger,
                        value = "${metrics.currentStreakDays}d",
                        label = "Streak",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Global Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search subjects, chapters, topics or PDFs",
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
                    placeholder = { Text("Search subjects, chapters, topics, PDFs...") },
                    singleLine = true,
                    shape = RoundedCornerShape(ImtihanRadii.card),
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

            // Continue Learning Hero Card
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
                            ZeroPillMetaLine("CONTINUE LEARNING")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeChapterTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (metrics.continueChapterPercent > 0) {
                                    "${metrics.continueChapterPercent}% completed · Tap to resume"
                                } else {
                                    "Class ${shellState.selectedClassLevel} KPK · Ready to study"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = colors.accentBlue,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Continue chapter",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ImtihanProgressBar(progress = metrics.continueChapterPercent / 100f)
                }
            }

            // Quick Actions (6 Core Actions per v2.0 Spec)
            item {
                ImtihanSectionHeader(title = "Quick Actions")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionCompactTile(
                            title = "MCQs",
                            icon = Icons.Outlined.Quiz,
                            tint = colors.accentBlue,
                            onClick = onOpenMcqPractice,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCompactTile(
                            title = "Mock Exams",
                            icon = Icons.Outlined.Timer,
                            tint = colors.warningGold,
                            onClick = onOpenMcqPractice,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCompactTile(
                            title = "Past Papers",
                            icon = Icons.Outlined.Description,
                            tint = colors.danger,
                            onClick = onOpenPapers,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionCompactTile(
                            title = "Textbooks",
                            icon = Icons.Outlined.MenuBook,
                            tint = colors.success,
                            onClick = onOpenTextbooks,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCompactTile(
                            title = "Notes",
                            icon = Icons.Outlined.EditNote,
                            tint = colors.accentBlue,
                            onClick = onOpenNotes,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCompactTile(
                            title = "Flashcards",
                            icon = Icons.Outlined.Style,
                            tint = colors.warningGold,
                            onClick = onOpenBookmarksFlashcards,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Daily Challenge + Focus Studio
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SilverBlueCard(
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
                                Text("Daily Challenge", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("5-min rapid MCQ", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                    SilverBlueCard(
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
                            Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = colors.accentBlue)
                            Column {
                                Text("Focus Studio", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("Pomodoro timer", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Subjects Overview
            item {
                ImtihanSectionHeader(
                    title = "Subjects",
                    actionLabel = "View All",
                    onAction = onViewAllSubjects
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (subjectsState) {
                    is UiResourceState.Loading -> ImtihanSkeletonList(count = 2)
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
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Text(
                                            text = "${subject.progressPercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.accentBlue
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
                                        text = "${subject.completedChapters}/${subject.totalChapters} Chapters",
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

            // Syllabus Progress, Countdown & Share Streak
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
                            size = 76.dp,
                            strokeWidth = 7.dp,
                            centerLabel = "${shellState.profile.overallProgressPercent}%",
                            centerCaption = "SYLLABUS"
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            ZeroPillMetaLine(
                                text = daysToExam?.let { "$it DAYS TO BISE EXAM" } ?: "EXAM COUNTDOWN & GOALS"
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${metrics.currentStreakDays}d Streak · ${metrics.xpTotal} XP",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Daily Goal: ${shellState.profile.dailyGoalPercent}% of ${shellState.dailyTargetMinutes}m",
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
                            shape = RoundedCornerShape(ImtihanRadii.button)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accentBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Streak", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onOpenStudyPlanner,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(ImtihanRadii.button)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accentBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(daysToExam?.let { "${it}d Left" } ?: "Study Plan", maxLines = 1)
                        }
                    }
                }
            }

            // Smart Study Workspace Tools
            item {
                ImtihanSectionHeader(title = "Smart Tools & Revision")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard(
                            title = "AI Study Tutor",
                            subtitle = "Grounded explanations",
                            icon = Icons.Outlined.AutoAwesome,
                            tint = colors.warningGold,
                            onClick = onOpenAiTutor,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Mistake Master",
                            subtitle = "Fix weak areas faster",
                            icon = Icons.Outlined.ReportProblem,
                            tint = colors.danger,
                            onClick = onOpenMistakeMaster,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard(
                            title = "Study Planner",
                            subtitle = "Tasks & timetable",
                            icon = Icons.Outlined.CalendarToday,
                            tint = colors.accentBlue,
                            onClick = onOpenStudyPlanner,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Offline Library",
                            subtitle = "Saved books & PDFs",
                            icon = Icons.Outlined.CloudDownload,
                            tint = colors.success,
                            onClick = onOpenDownloads,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (showHomeAd) {
                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Academic Partner",
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
private fun SelectorHeaderChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        shape = RoundedCornerShape(ImtihanRadii.button),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Change class or board",
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun QuickActionCompactTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 76.dp),
        shape = RoundedCornerShape(ImtihanRadii.card),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(ImtihanRadii.chip),
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
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

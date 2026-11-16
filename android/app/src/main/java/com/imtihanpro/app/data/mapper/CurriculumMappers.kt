package com.imtihanpro.app.data.mapper

import com.imtihanpro.app.data.remote.ApiAdPlacementConfigDto
import com.imtihanpro.app.data.remote.ApiChapterDto
import com.imtihanpro.app.data.remote.ApiContentBlockDto
import com.imtihanpro.app.data.remote.ApiExamPatternDto
import com.imtihanpro.app.data.remote.ApiMcqQuestionDto
import com.imtihanpro.app.data.remote.ApiPastPaperDto
import com.imtihanpro.app.data.remote.ApiPdfMetadataDto
import com.imtihanpro.app.data.remote.ApiSubjectDto
import com.imtihanpro.app.data.remote.ApiSubjectivePromptDto
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.ChapterProgressStatus
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.domain.model.ExamPatternUi
import com.imtihanpro.app.domain.model.McqOptionUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.PastPaperUi
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.SubjectivePromptUi
import com.imtihanpro.app.domain.model.TopicUiModel
import kotlin.math.roundToInt

object CurriculumMappers {

    fun toSubjectUi(
        dto: ApiSubjectDto,
        completedChapters: Int
    ): SubjectUiModel {
        val progress = if (dto.totalChapters <= 0) 0 else {
            ((completedChapters.toFloat() / dto.totalChapters.toFloat()) * 100f).toInt().coerceIn(0, 100)
        }
        return SubjectUiModel(
            id = dto.id,
            classLevel = dto.classLevel,
            boardCode = dto.boardCode,
            name = dto.name,
            iconKey = dto.iconKey,
            totalChapters = dto.totalChapters,
            completedChapters = completedChapters,
            progressPercent = progress,
            hasSeedContent = dto.coverageStatus == "active_seed",
            studyGroups = dto.studyGroups,
            description = dto.description,
            textbookPdfId = dto.textbookPdfId
        )
    }

    fun toChapterUi(
        dto: ApiChapterDto,
        completedChapterIds: Set<String>,
        inProgressChapterIds: Set<String>,
        downloadedChapterIds: Set<String>
    ): ChapterUiModel {
        val status = when {
            dto.id in completedChapterIds -> ChapterProgressStatus.COMPLETED
            dto.id in inProgressChapterIds -> ChapterProgressStatus.IN_PROGRESS
            dto.isLockedByDefault -> ChapterProgressStatus.LOCKED
            else -> ChapterProgressStatus.AVAILABLE
        }
        return ChapterUiModel(
            id = dto.id,
            subjectId = dto.subjectId,
            chapterNumber = dto.chapterNumber,
            title = dto.title,
            subtitle = dto.subtitle,
            versionLabel = dto.currentVersion,
            status = status,
            mcqCount = dto.mcqCount,
            solvedCount = dto.solvedQuestionsCount,
            estimatedSizeKb = dto.estimatedSizeKb,
            isDownloadedOffline = dto.id in downloadedChapterIds,
            topics = dto.topics.map {
                TopicUiModel(it.id, it.chapterId, it.orderIndex, it.title, it.estimatedMinutes)
            },
            difficultyTag = dto.difficultyTag,
            sourceReference = dto.sourceReference
        )
    }

    fun toContentBlockUi(dto: ApiContentBlockDto): ContentBlockUi {
        return when (dto) {
            is ApiContentBlockDto.HeadingDto -> ContentBlockUi.Heading(
                id = dto.id,
                sectionCode = dto.sectionCode,
                text = dto.text,
                isSubheading = dto.isSubheading
            )
            is ApiContentBlockDto.ParagraphDto -> ContentBlockUi.Paragraph(dto.id, dto.text)
            is ApiContentBlockDto.MathExpressionDto -> ContentBlockUi.MathExpression(
                dto.id, dto.latex, dto.plainNotation, dto.caption
            )
            is ApiContentBlockDto.TableDto -> ContentBlockUi.Table(
                dto.id, dto.title, dto.headers, dto.rows
            )
            is ApiContentBlockDto.ListDto -> ContentBlockUi.BulletOrNumberedList(
                dto.id, dto.ordered, dto.title, dto.items
            )
            is ApiContentBlockDto.DefinitionDto -> ContentBlockUi.ImportantDefinition(
                dto.id, dto.term, dto.definition, dto.formulaHint
            )
            is ApiContentBlockDto.ExampleDto -> ContentBlockUi.WorkedExample(
                dto.id, dto.title, dto.problemStatement, dto.steps, dto.finalAnswer
            )
            is ApiContentBlockDto.DiagramDto -> ContentBlockUi.Diagram(
                dto.id, dto.title, dto.caption, dto.assetKey, dto.altText
            )
            is ApiContentBlockDto.ExamPointDto -> ContentBlockUi.HighlightedExamPoint(
                dto.id, dto.title, dto.points, dto.boardFrequency
            )
            is ApiContentBlockDto.McqDto -> ContentBlockUi.InlineMcq(
                id = dto.id,
                question = dto.question,
                options = dto.options.map { McqOptionUi(it.first, it.second) },
                correctOptionId = dto.correctOptionId,
                explanation = dto.explanation
            )
            is ApiContentBlockDto.SolvedQuestionDto -> ContentBlockUi.SolvedQuestion(
                id = dto.id,
                questionType = dto.questionType,
                marks = dto.marks,
                question = dto.question,
                answerSteps = dto.answerSteps,
                yearReference = dto.yearReference
            )
        }
    }

    fun toMcqUi(dto: ApiMcqQuestionDto): McqQuestionUi {
        return McqQuestionUi(
            id = dto.id,
            chapterId = dto.chapterId,
            question = dto.question,
            options = dto.options.map { McqOptionUi(it.first, it.second) },
            correctOptionId = dto.correctOptionId,
            explanation = dto.explanation,
            subjectId = dto.subjectId,
            difficulty = dto.difficulty,
            topicTitle = dto.topicTitle,
            pastPaperYear = dto.pastPaperYear
        )
    }

    fun toPastPaperUi(dto: ApiPastPaperDto, downloadedIds: Set<String>): PastPaperUi {
        return PastPaperUi(
            id = dto.id,
            subjectName = dto.subjectName,
            year = dto.year,
            isSolved = dto.isSolved,
            fileSizeMb = dto.fileSizeMb,
            fileSizeLabel = "${dto.fileSizeMb} MB",
            pageCount = dto.pageCount,
            isDownloaded = dto.id in downloadedIds || dto.pdfMetadataId in downloadedIds,
            pdfMetadataId = dto.pdfMetadataId
        )
    }

    fun toPdfResourceUi(dto: ApiPdfMetadataDto, downloadedIds: Set<String>): PdfResourceUi {
        val mb = ((dto.fileSizeBytes / (1024.0 * 1024.0)) * 10.0).roundToInt() / 10.0
        return PdfResourceUi(
            id = dto.id,
            title = dto.title,
            category = dto.category,
            classLevel = dto.classLevel,
            boardCode = dto.boardCode,
            subjectId = dto.subjectId,
            sizeMb = maxOf(0.1, mb),
            pageCount = dto.pageCount,
            version = dto.version,
            isDownloaded = dto.id in downloadedIds
        )
    }

    fun toExamPatternUi(dto: ApiExamPatternDto): ExamPatternUi = ExamPatternUi(
        id = dto.id,
        title = dto.title,
        mode = dto.mode,
        classLevel = dto.classLevel,
        boardCode = dto.boardCode,
        subjectId = dto.subjectId,
        questionCount = dto.questionCount,
        durationMinutes = dto.durationMinutes,
        marksPerMcq = dto.marksPerMcq,
        negativeMarkingPenalty = dto.negativeMarkingPenalty,
        passPercentage = dto.passPercentage,
        includesShortQuestionsCount = dto.includesShortQuestionsCount,
        includesLongQuestionsCount = dto.includesLongQuestionsCount,
        sectionSummary = dto.sectionSummary
    )

    fun toSubjectivePromptUi(dto: ApiSubjectivePromptDto): SubjectivePromptUi = SubjectivePromptUi(
        id = dto.id,
        questionType = dto.questionType,
        marks = dto.marks,
        question = dto.question,
        answerSteps = dto.answerSteps,
        yearReference = dto.yearReference
    )

    fun toAdPolicyUi(dto: ApiAdPlacementConfigDto): AdPlacementPolicyUi = AdPlacementPolicyUi(
        enabled = dto.enabled,
        bannerOnHome = dto.bannerOnHome,
        bannerOnPapers = dto.bannerOnPapers,
        suppressDuringQuizAndExam = dto.suppressDuringQuizAndExam,
        suppressDuringNotesReading = dto.suppressDuringNotesReading,
        dailyBannerCap = dto.dailyBannerCap,
        providerNote = dto.providerNote
    )
}

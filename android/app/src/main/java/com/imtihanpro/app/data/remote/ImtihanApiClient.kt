package com.imtihanpro.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(
        val message: String,
        val statusCode: Int? = null,
        val code: String = "NETWORK_ERROR"
    ) : ApiResult<Nothing>
}

/**
 * Pure domain/sync validation and monotonic progress merge rules matching shared-contracts/rules.ts.
 */
object ImtihanSyncRules {

    fun mergeProgressMonotonic(
        local: List<ApiChapterProgressSyncDto>,
        remote: List<ApiChapterProgressSyncDto>
    ): List<ApiChapterProgressSyncDto> {
        val merged = linkedMapOf<String, ApiChapterProgressSyncDto>()
        for (item in remote) {
            merged[item.chapterId] = item
        }
        for (item in local) {
            val prev = merged[item.chapterId]
            if (prev == null) {
                merged[item.chapterId] = item
            } else {
                val newerIso = if (item.lastAccessedAtIso >= prev.lastAccessedAtIso) {
                    item.lastAccessedAtIso
                } else {
                    prev.lastAccessedAtIso
                }
                merged[item.chapterId] = ApiChapterProgressSyncDto(
                    chapterId = item.chapterId,
                    subjectId = item.subjectId.ifBlank { prev.subjectId },
                    completionPercent = maxOf(item.completionPercent, prev.completionPercent),
                    lastBlockIndex = maxOf(item.lastBlockIndex, prev.lastBlockIndex),
                    isCompleted = item.isCompleted || prev.isCompleted,
                    lastAccessedAtIso = newerIso
                )
            }
        }
        return merged.values.toList()
    }

    fun validateWorkerBaseUrl(rawUrl: String): Pair<Boolean, String?> {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false to "API URL is required."
        val parsed = trimmed.toHttpUrlOrNull() ?: return false to "Invalid HTTP(S) URL."
        val host = parsed.host.lowercase()
        val isLocal = host == "localhost" ||
            host == "127.0.0.1" ||
            host == "10.0.2.2" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.")
        if (parsed.scheme == "http" && !isLocal) {
            return false to "Public endpoints must use HTTPS (HTTP allowed only for local LAN/emulator testing)."
        }
        return true to null
    }
}

object ApiContentJsonParser {

    fun parseTopics(array: JSONArray?): List<ApiTopicDto> {
        if (array == null) return emptyList()
        val list = mutableListOf<ApiTopicDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiTopicDto(
                    id = o.optString("id", "top_$i"),
                    chapterId = o.optString("chapterId", ""),
                    orderIndex = o.optInt("orderIndex", i + 1),
                    title = o.optString("title", ""),
                    estimatedMinutes = o.optInt("estimatedMinutes", 15)
                )
            )
        }
        return list
    }

    fun serializeTopics(topics: List<ApiTopicDto>): String {
        val arr = JSONArray()
        topics.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("chapterId", t.chapterId)
                    .put("orderIndex", t.orderIndex)
                    .put("title", t.title)
                    .put("estimatedMinutes", t.estimatedMinutes)
            )
        }
        return arr.toString()
    }

    fun parseSubjects(array: JSONArray): List<ApiSubjectDto> {
        val list = mutableListOf<ApiSubjectDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiSubjectDto(
                    id = o.getString("id"),
                    classLevel = o.optInt("classLevel", 9),
                    boardCode = o.optString("boardCode", "KPK"),
                    code = o.optString("code", ""),
                    name = o.getString("name"),
                    iconKey = o.optString("iconKey", "physics"),
                    totalChapters = o.optInt("totalChapters", 0),
                    publishedChapters = o.optInt("publishedChapters", 0),
                    coverageStatus = o.optString("coverageStatus", "structure_ready"),
                    studyGroups = jsonStringList(o.optJSONArray("studyGroups")),
                    description = o.optString("description", ""),
                    textbookPdfId = o.optString("textbookPdfId").takeIf { it.isNotEmpty() && it != "null" }
                )
            )
        }
        return list
    }

    fun parseChapters(array: JSONArray): List<ApiChapterDto> {
        val list = mutableListOf<ApiChapterDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiChapterDto(
                    id = o.getString("id"),
                    subjectId = o.getString("subjectId"),
                    chapterNumber = o.optInt("chapterNumber", i + 1),
                    title = o.getString("title"),
                    subtitle = o.optString("subtitle", ""),
                    currentVersion = o.optString("currentVersion", "v1.0.0"),
                    publishStatus = o.optString("publishStatus", "draft"),
                    mcqCount = o.optInt("mcqCount", 0),
                    solvedQuestionsCount = o.optInt("solvedQuestionsCount", 0),
                    isLockedByDefault = o.optBoolean("isLockedByDefault", false),
                    estimatedSizeKb = o.optInt("estimatedSizeKb", 250),
                    topics = parseTopics(o.optJSONArray("topics")),
                    difficultyTag = o.optString("difficultyTag").takeIf { it.isNotEmpty() && it != "null" },
                    sourceReference = o.optString("sourceReference").takeIf { it.isNotEmpty() && it != "null" }
                )
            )
        }
        return list
    }

    fun parseChapterContent(o: JSONObject): ApiChapterContentResponseDto {
        val blocksArr = o.optJSONArray("blocks") ?: JSONArray()
        return ApiChapterContentResponseDto(
            id = o.optString("id", ""),
            chapterId = o.getString("chapterId"),
            mode = o.optString("mode", "detailed_notes"),
            version = o.optString("version", "v1.0.0"),
            publishStatus = o.optString("publishStatus", "published"),
            blocks = parseContentBlocks(blocksArr),
            updatedAtIso = o.optString("updatedAtIso", "")
        )
    }

    fun parseContentBlocks(array: JSONArray): List<ApiContentBlockDto> {
        val result = mutableListOf<ApiContentBlockDto>()
        for (i in 0 until array.length()) {
            val b = array.getJSONObject(i)
            val id = b.optString("id", "blk_$i")
            val orderIndex = b.optInt("orderIndex", i + 1)
            val block: ApiContentBlockDto? = when (b.optString("type")) {
                "heading", "subheading" -> ApiContentBlockDto.HeadingDto(
                    id = id,
                    orderIndex = orderIndex,
                    sectionCode = b.optString("sectionCode").takeIf { it.isNotEmpty() && it != "null" },
                    text = b.optString("text", ""),
                    isSubheading = b.optBoolean("isSubheading", b.optString("type") == "subheading")
                )
                "paragraph" -> ApiContentBlockDto.ParagraphDto(
                    id = id,
                    orderIndex = orderIndex,
                    text = b.optString("text", "")
                )
                "math_expression" -> ApiContentBlockDto.MathExpressionDto(
                    id = id,
                    orderIndex = orderIndex,
                    latex = b.optString("latex", ""),
                    plainNotation = b.optString("plainNotation", ""),
                    caption = b.optString("caption").takeIf { it.isNotEmpty() && it != "null" }
                )
                "table" -> {
                    val headers = jsonStringList(b.optJSONArray("headers"))
                    val rowsArr = b.optJSONArray("rows") ?: JSONArray()
                    val rows = mutableListOf<List<String>>()
                    for (r in 0 until rowsArr.length()) {
                        rows.add(jsonStringList(rowsArr.optJSONArray(r)))
                    }
                    ApiContentBlockDto.TableDto(
                        id = id,
                        orderIndex = orderIndex,
                        title = b.optString("title").takeIf { it.isNotEmpty() && it != "null" },
                        headers = headers,
                        rows = rows
                    )
                }
                "list" -> ApiContentBlockDto.ListDto(
                    id = id,
                    orderIndex = orderIndex,
                    ordered = b.optBoolean("ordered", false),
                    title = b.optString("title").takeIf { it.isNotEmpty() && it != "null" },
                    items = jsonStringList(b.optJSONArray("items"))
                )
                "definition" -> ApiContentBlockDto.DefinitionDto(
                    id = id,
                    orderIndex = orderIndex,
                    term = b.optString("term", ""),
                    definition = b.optString("definition", ""),
                    formulaHint = b.optString("formulaHint").takeIf { it.isNotEmpty() && it != "null" }
                )
                "example" -> ApiContentBlockDto.ExampleDto(
                    id = id,
                    orderIndex = orderIndex,
                    title = b.optString("title", "Worked Example"),
                    problemStatement = b.optString("problemStatement", ""),
                    steps = jsonStringList(b.optJSONArray("steps")),
                    finalAnswer = b.optString("finalAnswer", "")
                )
                "diagram" -> ApiContentBlockDto.DiagramDto(
                    id = id,
                    orderIndex = orderIndex,
                    title = b.optString("title", ""),
                    caption = b.optString("caption", ""),
                    assetKey = b.optString("assetKey", ""),
                    altText = b.optString("altText", "")
                )
                "exam_point" -> ApiContentBlockDto.ExamPointDto(
                    id = id,
                    orderIndex = orderIndex,
                    title = b.optString("title", "Exam Highlight"),
                    points = jsonStringList(b.optJSONArray("points")),
                    boardFrequency = b.optString("boardFrequency").takeIf { it.isNotEmpty() && it != "null" }
                )
                "mcq" -> ApiContentBlockDto.McqDto(
                    id = id,
                    orderIndex = orderIndex,
                    question = b.optString("question", ""),
                    options = parseOptions(b.optJSONArray("options")),
                    correctOptionId = b.optString("correctOptionId", "A"),
                    explanation = b.optString("explanation", ""),
                    difficulty = b.optString("difficulty", "medium")
                )
                "solved_question" -> ApiContentBlockDto.SolvedQuestionDto(
                    id = id,
                    orderIndex = orderIndex,
                    questionType = b.optString("questionType", "short"),
                    marks = b.optInt("marks", 4),
                    question = b.optString("question", ""),
                    answerSteps = jsonStringList(b.optJSONArray("answerSteps")),
                    yearReference = b.optString("yearReference").takeIf { it.isNotEmpty() && it != "null" }
                )
                else -> null
            }
            if (block != null) result.add(block)
        }
        return result
    }

    fun serializeContentBlocks(blocks: List<ApiContentBlockDto>): String {
        val arr = JSONArray()
        for (b in blocks) {
            val o = JSONObject()
            o.put("id", b.id)
            o.put("orderIndex", b.orderIndex)
            when (b) {
                is ApiContentBlockDto.HeadingDto -> {
                    o.put("type", if (b.isSubheading) "subheading" else "heading")
                    o.put("sectionCode", b.sectionCode)
                    o.put("text", b.text)
                    o.put("isSubheading", b.isSubheading)
                }
                is ApiContentBlockDto.ParagraphDto -> {
                    o.put("type", "paragraph")
                    o.put("text", b.text)
                }
                is ApiContentBlockDto.MathExpressionDto -> {
                    o.put("type", "math_expression")
                    o.put("latex", b.latex)
                    o.put("plainNotation", b.plainNotation)
                    o.put("caption", b.caption)
                }
                is ApiContentBlockDto.TableDto -> {
                    o.put("type", "table")
                    o.put("title", b.title)
                    o.put("headers", JSONArray(b.headers))
                    val rows = JSONArray()
                    b.rows.forEach { rows.put(JSONArray(it)) }
                    o.put("rows", rows)
                }
                is ApiContentBlockDto.ListDto -> {
                    o.put("type", "list")
                    o.put("ordered", b.ordered)
                    o.put("title", b.title)
                    o.put("items", JSONArray(b.items))
                }
                is ApiContentBlockDto.DefinitionDto -> {
                    o.put("type", "definition")
                    o.put("term", b.term)
                    o.put("definition", b.definition)
                    o.put("formulaHint", b.formulaHint)
                }
                is ApiContentBlockDto.ExampleDto -> {
                    o.put("type", "example")
                    o.put("title", b.title)
                    o.put("problemStatement", b.problemStatement)
                    o.put("steps", JSONArray(b.steps))
                    o.put("finalAnswer", b.finalAnswer)
                }
                is ApiContentBlockDto.DiagramDto -> {
                    o.put("type", "diagram")
                    o.put("title", b.title)
                    o.put("caption", b.caption)
                    o.put("assetKey", b.assetKey)
                    o.put("altText", b.altText)
                }
                is ApiContentBlockDto.ExamPointDto -> {
                    o.put("type", "exam_point")
                    o.put("title", b.title)
                    o.put("points", JSONArray(b.points))
                    o.put("boardFrequency", b.boardFrequency)
                }
                is ApiContentBlockDto.McqDto -> {
                    o.put("type", "mcq")
                    o.put("question", b.question)
                    val opts = JSONArray()
                    b.options.forEach { (id, txt) ->
                        opts.put(JSONObject().put("id", id).put("text", txt))
                    }
                    o.put("options", opts)
                    o.put("correctOptionId", b.correctOptionId)
                    o.put("explanation", b.explanation)
                    o.put("difficulty", b.difficulty)
                }
                is ApiContentBlockDto.SolvedQuestionDto -> {
                    o.put("type", "solved_question")
                    o.put("questionType", b.questionType)
                    o.put("marks", b.marks)
                    o.put("question", b.question)
                    o.put("answerSteps", JSONArray(b.answerSteps))
                    o.put("yearReference", b.yearReference)
                }
            }
            arr.put(o)
        }
        return arr.toString()
    }

    fun parseMcqs(array: JSONArray): List<ApiMcqQuestionDto> {
        val list = mutableListOf<ApiMcqQuestionDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(parseSingleMcq(o))
        }
        return list
    }

    private fun parseSingleMcq(o: JSONObject): ApiMcqQuestionDto {
        return ApiMcqQuestionDto(
            id = o.getString("id"),
            chapterId = o.getString("chapterId"),
            subjectId = o.optString("subjectId", ""),
            question = o.getString("question"),
            options = parseOptions(o.optJSONArray("options")),
            correctOptionId = o.optString("correctOptionId", "A"),
            explanation = o.optString("explanation", ""),
            difficulty = o.optString("difficulty", "medium"),
            topicTitle = o.optString("topicTitle", "Core Concepts"),
            pastPaperYear = if (o.has("pastPaperYear") && !o.isNull("pastPaperYear")) o.optInt("pastPaperYear") else null
        )
    }

    fun parsePastPapers(array: JSONArray): List<ApiPastPaperDto> {
        val list = mutableListOf<ApiPastPaperDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiPastPaperDto(
                    id = o.getString("id"),
                    boardCode = o.optString("boardCode", "KPK"),
                    classLevel = o.optInt("classLevel", 9),
                    subjectId = o.optString("subjectId", ""),
                    subjectName = o.optString("subjectName", "Subject"),
                    year = o.optInt("year", 2022),
                    isSolved = o.optBoolean("isSolved", false),
                    pdfMetadataId = o.optString("pdfMetadataId", ""),
                    fileSizeMb = o.optDouble("fileSizeMb", 1.5),
                    pageCount = o.optInt("pageCount", 10)
                )
            )
        }
        return list
    }

    fun parsePdfMetadataList(array: JSONArray): List<ApiPdfMetadataDto> {
        val list = mutableListOf<ApiPdfMetadataDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiPdfMetadataDto(
                    id = o.getString("id"),
                    title = o.optString("title", ""),
                    category = o.optString("category", "past_paper"),
                    classLevel = o.optInt("classLevel", 9),
                    boardCode = o.optString("boardCode", "KPK"),
                    subjectId = o.optString("subjectId", ""),
                    objectKey = o.optString("objectKey", ""),
                    fileSizeBytes = o.optLong("fileSizeBytes", 0L),
                    pageCount = o.optInt("pageCount", 10),
                    checksumSha256 = o.optString("checksumSha256", ""),
                    offlineCacheable = o.optBoolean("offlineCacheable", true),
                    version = o.optString("version", "v1.0")
                )
            )
        }
        return list
    }

    fun parseAuthSession(o: JSONObject): ApiAuthSessionDto {
        val user = o.optJSONObject("user")
        return ApiAuthSessionDto(
            mode = o.optString("mode", "guest"),
            guestDeviceId = o.optString("guestDeviceId", ""),
            token = o.optString("token").takeIf { it.isNotEmpty() && it != "null" },
            userId = user?.optString("id")?.takeIf { it.isNotEmpty() && it != "null" },
            displayName = user?.optString("displayName")?.takeIf { it.isNotEmpty() && it != "null" },
            email = user?.optString("email")?.takeIf { it.isNotEmpty() && it != "null" }
        )
    }

    fun parseStudentProgress(o: JSONObject): ApiStudentProgressDto {
        val arr = o.optJSONArray("chapterProgress") ?: JSONArray()
        val list = mutableListOf<ApiChapterProgressSyncDto>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            list.add(
                ApiChapterProgressSyncDto(
                    chapterId = c.getString("chapterId"),
                    subjectId = c.optString("subjectId", ""),
                    completionPercent = c.optInt("completionPercent", 0),
                    lastBlockIndex = c.optInt("lastBlockIndex", 0),
                    isCompleted = c.optBoolean("isCompleted", false),
                    lastAccessedAtIso = c.optString("lastAccessedAtIso", "")
                )
            )
        }
        val resolvedId = o.optString("localProfileId", "").ifBlank { o.optString("studentId", "") }
        return ApiStudentProgressDto(
            studentId = resolvedId,
            selectedClass = o.optInt("selectedClass", 9),
            selectedBoard = o.optString("selectedBoard", "KPK"),
            studyHoursTotal = o.optDouble("studyHoursTotal", 0.0),
            dailyGoalPercent = o.optInt("dailyGoalPercent", 0),
            chapterProgress = list,
            selectedSubBoard = o.optString("selectedSubBoard").takeIf { it.isNotEmpty() && it != "null" },
            studyGroup = o.optString("studyGroup").takeIf { it.isNotEmpty() && it != "null" },
            language = o.optString("language", "en"),
            dailyTargetMinutes = o.optInt("dailyTargetMinutes", 45),
            examCountdownDateIso = o.optString("examCountdownDateIso").takeIf { it.isNotEmpty() && it != "null" },
            diagnosticCompleted = o.optBoolean("diagnosticCompleted", false),
            xpTotal = o.optInt("xpTotal", 0),
            currentStreakDays = o.optInt("currentStreakDays", 0)
        )
    }

    fun parsePublicConfig(o: JSONObject): ApiPublicRuntimeConfigDto {
        val subBoardsArr = o.optJSONArray("kpkSubBoards") ?: JSONArray()
        val subBoards = mutableListOf<ApiKpkSubBoardDto>()
        for (i in 0 until subBoardsArr.length()) {
            val s = subBoardsArr.getJSONObject(i)
            subBoards.add(
                ApiKpkSubBoardDto(
                    code = s.optString("code", ""),
                    name = s.optString("name", ""),
                    headquarters = s.optString("headquarters", "")
                )
            )
        }
        val groupsArr = o.optJSONArray("studyGroups") ?: JSONArray()
        val groups = mutableListOf<ApiStudyGroupDto>()
        for (i in 0 until groupsArr.length()) {
            val g = groupsArr.getJSONObject(i)
            val clsArr = g.optJSONArray("applicableClasses") ?: JSONArray()
            val classes = (0 until clsArr.length()).map { clsArr.optInt(it) }
            groups.add(
                ApiStudyGroupDto(
                    code = g.optString("code", ""),
                    label = g.optString("label", ""),
                    applicableClasses = classes,
                    description = g.optString("description", "")
                )
            )
        }
        val adObj = o.optJSONObject("adPlacement")
        val adPolicy = if (adObj != null) {
            ApiAdPlacementConfigDto(
                enabled = adObj.optBoolean("enabled", false),
                bannerOnHome = adObj.optBoolean("bannerOnHome", false),
                bannerOnPapers = adObj.optBoolean("bannerOnPapers", false),
                suppressDuringQuizAndExam = adObj.optBoolean("suppressDuringQuizAndExam", true),
                suppressDuringNotesReading = adObj.optBoolean("suppressDuringNotesReading", true),
                dailyBannerCap = adObj.optInt("dailyBannerCap", 2),
                providerNote = adObj.optString("providerNote", "")
            )
        } else {
            ApiAdPlacementConfigDto()
        }
        return ApiPublicRuntimeConfigDto(
            defaultBoardCode = o.optString("defaultBoardCode", "KPK"),
            defaultClassLevel = o.optInt("defaultClassLevel", 9),
            kpkSubBoards = subBoards,
            studyGroups = groups,
            adPlacement = adPolicy,
            aiTutorConfigured = o.optBoolean("aiTutorConfigured", false)
        )
    }

    fun parseExamPatterns(array: JSONArray): List<ApiExamPatternDto> {
        val list = mutableListOf<ApiExamPatternDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiExamPatternDto(
                    id = o.getString("id"),
                    title = o.optString("title", ""),
                    mode = o.optString("mode", "full_mock_exam"),
                    classLevel = o.optInt("classLevel", 9),
                    boardCode = o.optString("boardCode", "KPK"),
                    subjectId = o.optString("subjectId").takeIf { it.isNotEmpty() && it != "null" },
                    questionCount = o.optInt("questionCount", 12),
                    durationMinutes = o.optInt("durationMinutes", 20),
                    marksPerMcq = o.optDouble("marksPerMcq", 1.0),
                    negativeMarkingPenalty = o.optDouble("negativeMarkingPenalty", 0.0),
                    passPercentage = o.optInt("passPercentage", 40),
                    includesShortQuestionsCount = o.optInt("includesShortQuestionsCount", 0),
                    includesLongQuestionsCount = o.optInt("includesLongQuestionsCount", 0),
                    sectionSummary = o.optString("sectionSummary", "")
                )
            )
        }
        return list
    }

    fun parseExamSession(o: JSONObject): ApiExamSessionDto {
        val subjArr = o.optJSONArray("subjectivePrompts") ?: JSONArray()
        val prompts = mutableListOf<ApiSubjectivePromptDto>()
        for (i in 0 until subjArr.length()) {
            val s = subjArr.getJSONObject(i)
            prompts.add(
                ApiSubjectivePromptDto(
                    id = s.optString("id", "sp_$i"),
                    questionType = s.optString("questionType", "short"),
                    marks = s.optInt("marks", 4),
                    question = s.optString("question", ""),
                    answerSteps = jsonStringList(s.optJSONArray("answerSteps")),
                    yearReference = s.optString("yearReference").takeIf { it.isNotEmpty() && it != "null" }
                )
            )
        }
        return ApiExamSessionDto(
            sessionId = o.getString("sessionId"),
            mode = o.optString("mode", "chapter_test"),
            title = o.optString("title", "Mock Exam"),
            subjectId = o.optString("subjectId", ""),
            chapterId = o.optString("chapterId").takeIf { it.isNotEmpty() && it != "null" },
            durationMinutes = o.optInt("durationMinutes", 15),
            marksPerMcq = o.optDouble("marksPerMcq", 1.0),
            negativeMarkingPenalty = o.optDouble("negativeMarkingPenalty", 0.0),
            passPercentage = o.optInt("passPercentage", 40),
            mcqs = parseMcqs(o.optJSONArray("mcqs") ?: JSONArray()),
            subjectivePrompts = prompts
        )
    }

    fun parseQuizHistory(array: JSONArray): List<ApiQuizHistoryItemDto> {
        val list = mutableListOf<ApiQuizHistoryItemDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiQuizHistoryItemDto(
                    id = o.getString("id"),
                    sessionId = o.optString("sessionId", ""),
                    subjectId = o.optString("subjectId", ""),
                    subjectName = o.optString("subjectName", "Subject"),
                    chapterId = o.optString("chapterId").takeIf { it.isNotEmpty() && it != "null" },
                    chapterTitle = o.optString("chapterTitle").takeIf { it.isNotEmpty() && it != "null" },
                    examMode = o.optString("examMode", "chapter_test"),
                    totalQuestions = o.optInt("totalQuestions", 0),
                    correctCount = o.optInt("correctCount", 0),
                    incorrectCount = o.optInt("incorrectCount", 0),
                    unansweredCount = o.optInt("unansweredCount", 0),
                    percentage = o.optInt("percentage", 0),
                    marksObtained = o.optDouble("marksObtained", 0.0),
                    totalMarks = o.optDouble("totalMarks", 0.0),
                    passed = o.optBoolean("passed", false),
                    elapsedSeconds = o.optInt("elapsedSeconds", 0),
                    submittedAtIso = o.optString("submittedAtIso", "")
                )
            )
        }
        return list
    }

    fun parseSearchResults(o: JSONObject): ApiSearchResultsDto {
        val topicsArr = o.optJSONArray("topics") ?: JSONArray()
        val topics = mutableListOf<ApiSearchTopicMatchDto>()
        for (i in 0 until topicsArr.length()) {
            val t = topicsArr.getJSONObject(i)
            topics.add(
                ApiSearchTopicMatchDto(
                    id = t.optString("id", "top_$i"),
                    chapterId = t.optString("chapterId", ""),
                    title = t.optString("title", ""),
                    subjectId = t.optString("subjectId", ""),
                    chapterTitle = t.optString("chapterTitle", "")
                )
            )
        }
        return ApiSearchResultsDto(
            query = o.optString("query", ""),
            subjects = parseSubjects(o.optJSONArray("subjects") ?: JSONArray()),
            chapters = parseChapters(o.optJSONArray("chapters") ?: JSONArray()),
            topics = topics,
            mcqs = parseMcqs(o.optJSONArray("mcqs") ?: JSONArray()),
            pdfs = parsePdfMetadataList(o.optJSONArray("pdfs") ?: JSONArray())
        )
    }

    fun parseBookmarks(array: JSONArray): List<ApiStudentBookmarkDto> {
        val list = mutableListOf<ApiStudentBookmarkDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiStudentBookmarkDto(
                    id = o.getString("id"),
                    itemType = o.optString("itemType", "chapter"),
                    itemId = o.optString("itemId", ""),
                    chapterId = o.optString("chapterId").takeIf { it.isNotEmpty() && it != "null" },
                    subjectId = o.optString("subjectId").takeIf { it.isNotEmpty() && it != "null" },
                    title = o.optString("title", ""),
                    subtitle = o.optString("subtitle", ""),
                    createdAtIso = o.optString("createdAtIso", "")
                )
            )
        }
        return list
    }

    fun parseMistakes(array: JSONArray): List<ApiMistakeRecordDto> {
        val list = mutableListOf<ApiMistakeRecordDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val mcqObj = o.optJSONObject("mcq")
            list.add(
                ApiMistakeRecordDto(
                    id = o.optString("id", ""),
                    mcqId = o.getString("mcqId"),
                    subjectId = o.optString("subjectId", ""),
                    chapterId = o.optString("chapterId", ""),
                    topicTitle = o.optString("topicTitle", "Core Concepts"),
                    question = o.optString("question", ""),
                    lastWrongOptionId = o.optString("lastWrongOptionId").takeIf { it.isNotEmpty() && it != "null" },
                    correctOptionId = o.optString("correctOptionId", "A"),
                    wrongCount = o.optInt("wrongCount", 1),
                    mastered = o.optBoolean("mastered", false),
                    lastAttemptedAtIso = o.optString("lastAttemptedAtIso", ""),
                    mcq = mcqObj?.let { parseSingleMcq(it) }
                )
            )
        }
        return list
    }

    fun parseFlashcards(array: JSONArray): List<ApiFlashcardDto> {
        val list = mutableListOf<ApiFlashcardDto>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                ApiFlashcardDto(
                    id = o.getString("id"),
                    subjectId = o.optString("subjectId", ""),
                    chapterId = o.optString("chapterId", ""),
                    front = o.optString("front", ""),
                    back = o.optString("back", ""),
                    formulaHint = o.optString("formulaHint").takeIf { it.isNotEmpty() && it != "null" },
                    topicTitle = o.optString("topicTitle", "Key Concept"),
                    intervalDays = o.optInt("intervalDays", 1),
                    easeFactor = o.optDouble("easeFactor", 2.5),
                    repetitions = o.optInt("repetitions", 0),
                    nextReviewAtIso = o.optString("nextReviewAtIso", ""),
                    lastReviewedAtIso = o.optString("lastReviewedAtIso").takeIf { it.isNotEmpty() && it != "null" }
                )
            )
        }
        return list
    }

    fun parseTutorResponse(o: JSONObject): ApiTutorResponseDto {
        val srcArr = o.optJSONArray("groundedSources") ?: JSONArray()
        val sources = mutableListOf<ApiTutorGroundedSourceDto>()
        for (i in 0 until srcArr.length()) {
            val s = srcArr.getJSONObject(i)
            sources.add(
                ApiTutorGroundedSourceDto(
                    type = s.optString("type", "chapter_note"),
                    title = s.optString("title", ""),
                    referenceId = s.optString("referenceId", "")
                )
            )
        }
        return ApiTutorResponseDto(
            available = o.optBoolean("available", true),
            provider = o.optString("provider", "gemini"),
            model = o.optString("model").takeIf { it.isNotEmpty() && it != "null" },
            answer = o.optString("answer").takeIf { it.isNotEmpty() && it != "null" },
            groundedSources = sources,
            unavailableReason = o.optString("unavailableReason").takeIf { it.isNotEmpty() && it != "null" },
            generatedAtIso = o.optString("generatedAtIso", "")
        )
    }

    fun parseStudyPlanner(o: JSONObject): ApiStudyPlannerStateDto {
        val tasksArr = o.optJSONArray("tasks") ?: JSONArray()
        val tasks = mutableListOf<ApiStudyPlanTaskDto>()
        for (i in 0 until tasksArr.length()) {
            val t = tasksArr.getJSONObject(i)
            tasks.add(parseStudyTask(t))
        }
        return ApiStudyPlannerStateDto(
            dailyTargetMinutes = o.optInt("dailyTargetMinutes", 45),
            examCountdownDateIso = o.optString("examCountdownDateIso").takeIf { it.isNotEmpty() && it != "null" },
            remindersEnabled = o.optBoolean("remindersEnabled", false),
            reminderHourLocal = o.optInt("reminderHourLocal", 19),
            currentStreakDays = o.optInt("currentStreakDays", 0),
            longestStreakDays = o.optInt("longestStreakDays", 0),
            minutesLoggedToday = o.optInt("minutesLoggedToday", 0),
            tasks = tasks
        )
    }

    fun parseStudyTask(t: JSONObject): ApiStudyPlanTaskDto = ApiStudyPlanTaskDto(
        id = t.getString("id"),
        title = t.optString("title", ""),
        subjectId = t.optString("subjectId").takeIf { it.isNotEmpty() && it != "null" },
        chapterId = t.optString("chapterId").takeIf { it.isNotEmpty() && it != "null" },
        plannedDateIso = t.optString("plannedDateIso", ""),
        estimatedMinutes = t.optInt("estimatedMinutes", 30),
        taskType = t.optString("taskType", "study"),
        completed = t.optBoolean("completed", false),
        completedAtIso = t.optString("completedAtIso").takeIf { it.isNotEmpty() && it != "null" }
    )

    fun parseMotivationSummary(o: JSONObject): ApiMotivationSummaryDto {
        val achArr = o.optJSONArray("achievements") ?: JSONArray()
        val achievements = (0 until achArr.length()).map { i ->
            val a = achArr.getJSONObject(i)
            ApiAchievementDto(
                id = a.optString("id", ""),
                code = a.optString("code", ""),
                title = a.optString("title", ""),
                description = a.optString("description", ""),
                xpReward = a.optInt("xpReward", 20),
                unlocked = a.optBoolean("unlocked", false),
                unlockedAtIso = a.optString("unlockedAtIso").takeIf { it.isNotEmpty() && it != "null" }
            )
        }
        val chArr = o.optJSONArray("challenges") ?: JSONArray()
        val challenges = (0 until chArr.length()).map { i ->
            val c = chArr.getJSONObject(i)
            ApiChallengeDto(
                id = c.optString("id", ""),
                title = c.optString("title", ""),
                description = c.optString("description", ""),
                cadence = c.optString("cadence", "daily"),
                targetCount = c.optInt("targetCount", 1),
                currentCount = c.optInt("currentCount", 0),
                xpReward = c.optInt("xpReward", 25),
                completed = c.optBoolean("completed", false),
                expiresAtIso = c.optString("expiresAtIso", "")
            )
        }
        val lbArr = o.optJSONArray("leaderboard") ?: JSONArray()
        val leaderboard = (0 until lbArr.length()).map { i ->
            val l = lbArr.getJSONObject(i)
            ApiLeaderboardEntryDto(
                rank = l.optInt("rank", i + 1),
                aliasHandle = l.optString("aliasHandle", "Scholar"),
                boardCode = l.optString("boardCode", "KPK"),
                classLevel = l.optInt("classLevel", 9),
                xpTotal = l.optInt("xpTotal", 0),
                streakDays = l.optInt("streakDays", 0),
                isCurrentUser = l.optBoolean("isCurrentUser", false)
            )
        }
        return ApiMotivationSummaryDto(
            xpTotal = o.optInt("xpTotal", 0),
            level = o.optInt("level", 1),
            nextLevelXp = o.optInt("nextLevelXp", 150),
            currentStreakDays = o.optInt("currentStreakDays", 0),
            leaderboardOptIn = o.optBoolean("leaderboardOptIn", false),
            aliasHandle = o.optString("aliasHandle").takeIf { it.isNotEmpty() && it != "null" },
            achievements = achievements,
            challenges = challenges,
            leaderboard = leaderboard
        )
    }

    fun parseOptions(array: JSONArray?): List<Pair<String, String>> {
        if (array == null) return emptyList()
        val opts = mutableListOf<Pair<String, String>>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            opts.add(o.optString("id", "A") to o.optString("text", ""))
        }
        return opts
    }

    fun serializeOptions(options: List<Pair<String, String>>): String {
        val arr = JSONArray()
        options.forEach { (id, text) ->
            arr.put(JSONObject().put("id", id).put("text", text))
        }
        return arr.toString()
    }

    private fun jsonStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val res = mutableListOf<String>()
        for (i in 0 until array.length()) {
            res.add(array.optString(i, ""))
        }
        return res
    }
}

/**
 * Real Stage 2-8 OkHttp REST & binary PDF client targeting the Cloudflare Worker (imtihan-pro-api).
 */
class ImtihanApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .readTimeout(ApiConfig.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .writeTimeout(ApiConfig.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .build()
) {
    companion object {
        /**
         * Creates an OkHttp client with a 12 MB disk HTTP cache so public curriculum GETs
         * automatically leverage `Cache-Control` and `ETag` (`304 Not Modified`) from Cloudflare Workers.
         */
        fun createWithCache(cacheRootDir: File): ImtihanApiClient {
            val httpCacheDir = File(cacheRootDir, "imtihan_http_cache")
            val cache = Cache(httpCacheDir, 12L * 1024L * 1024L)
            val client = OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(ApiConfig.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .readTimeout(ApiConfig.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .writeTimeout(ApiConfig.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            return ImtihanApiClient(httpClient = client)
        }
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun Request.Builder.withStudentAuth(guestDeviceId: String, bearerToken: String?): Request.Builder {
        if (guestDeviceId.isNotBlank()) header("X-Guest-Device-Id", guestDeviceId)
        if (!bearerToken.isNullOrBlank()) header("Authorization", "Bearer $bearerToken")
        return this
    }

    suspend fun fetchPublicConfig(baseUrl: String): ApiResult<ApiPublicRuntimeConfigDto> {
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/config"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parsePublicConfig(envelope.getJSONObject("data"))
        }
    }

    suspend fun fetchSubjects(
        baseUrl: String,
        classLevel: Int,
        boardCode: String,
        studyGroup: String? = null
    ): ApiResult<List<ApiSubjectDto>> {
        val groupQuery = if (!studyGroup.isNullOrBlank()) "&studyGroup=${encode(studyGroup)}" else ""
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/subjects?board=${encode(boardCode)}&classLevel=$classLevel$groupQuery"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parseSubjects(envelope.getJSONArray("data"))
        }
    }

    suspend fun fetchChapters(
        baseUrl: String,
        subjectId: String
    ): ApiResult<List<ApiChapterDto>> {
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/subjects/${encode(subjectId)}/chapters"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parseChapters(envelope.getJSONArray("data"))
        }
    }

    suspend fun fetchChapterContent(
        baseUrl: String,
        chapterId: String,
        mode: String = "detailed_notes"
    ): ApiResult<ApiChapterContentResponseDto> {
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/chapters/${encode(chapterId)}/content?mode=${encode(mode)}"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parseChapterContent(envelope.getJSONObject("data"))
        }
    }

    suspend fun fetchChapterMcqs(
        baseUrl: String,
        chapterId: String,
        topicTitle: String? = null,
        difficulty: String? = null
    ): ApiResult<List<ApiMcqQuestionDto>> {
        val topicParam = if (!topicTitle.isNullOrBlank()) "topic=${encode(topicTitle)}" else ""
        val diffParam = if (!difficulty.isNullOrBlank()) "difficulty=${encode(difficulty)}" else ""
        val qs = listOf(topicParam, diffParam).filter { it.isNotEmpty() }.joinToString("&")
        val suffix = if (qs.isNotEmpty()) "?$qs" else ""
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/chapters/${encode(chapterId)}/mcqs$suffix"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parseMcqs(envelope.getJSONArray("data"))
        }
    }

    suspend fun searchCatalog(
        baseUrl: String,
        query: String,
        boardCode: String? = null,
        classLevel: Int? = null
    ): ApiResult<ApiSearchResultsDto> {
        val params = mutableListOf("q=${encode(query)}")
        if (!boardCode.isNullOrBlank()) params.add("board=${encode(boardCode)}")
        if (classLevel != null) params.add("classLevel=$classLevel")
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/search?${params.joinToString("&")}"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parseSearchResults(envelope.getJSONObject("data"))
        }
    }

    suspend fun fetchPastPapers(
        baseUrl: String,
        classLevel: Int,
        boardCode: String,
        isSolved: Boolean? = null
    ): ApiResult<List<ApiPastPaperDto>> {
        val solvedQuery = if (isSolved != null) "&isSolved=$isSolved" else ""
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/past-papers?board=${encode(boardCode)}&classLevel=$classLevel$solvedQuery"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parsePastPapers(envelope.getJSONArray("data"))
        }
    }

    suspend fun fetchPdfs(
        baseUrl: String,
        category: String? = null,
        classLevel: Int? = null,
        boardCode: String? = null,
        subjectId: String? = null
    ): ApiResult<List<ApiPdfMetadataDto>> {
        val params = mutableListOf<String>()
        if (!category.isNullOrBlank()) params.add("category=${encode(category)}")
        if (classLevel != null) params.add("classLevel=$classLevel")
        if (!boardCode.isNullOrBlank()) params.add("board=${encode(boardCode)}")
        if (!subjectId.isNullOrBlank()) params.add("subjectId=${encode(subjectId)}")
        val suffix = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/pdfs$suffix"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parsePdfMetadataList(envelope.getJSONArray("data"))
        }
    }

    suspend fun fetchExamPatterns(
        baseUrl: String,
        boardCode: String,
        classLevel: Int,
        subjectId: String? = null
    ): ApiResult<List<ApiExamPatternDto>> {
        val sub = if (!subjectId.isNullOrBlank()) "&subjectId=${encode(subjectId)}" else ""
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/exams/patterns?board=${encode(boardCode)}&classLevel=$classLevel$sub"
        return executeJson(Request.Builder().url(url).get().build()) { envelope ->
            ApiContentJsonParser.parseExamPatterns(envelope.getJSONArray("data"))
        }
    }

    suspend fun startExamSession(
        baseUrl: String,
        mode: String,
        boardCode: String,
        classLevel: Int,
        patternId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        topicTitle: String? = null,
        difficulty: String = "mixed",
        questionCount: Int = 10
    ): ApiResult<ApiExamSessionDto> {
        val payload = JSONObject()
            .put("mode", mode)
            .put("boardCode", boardCode)
            .put("classLevel", classLevel)
            .put("difficulty", difficulty)
            .put("questionCount", questionCount)
        if (!patternId.isNullOrBlank()) payload.put("patternId", patternId)
        if (!subjectId.isNullOrBlank()) payload.put("subjectId", subjectId)
        if (!chapterId.isNullOrBlank()) payload.put("chapterId", chapterId)
        if (!topicTitle.isNullOrBlank()) payload.put("topicTitle", topicTitle)

        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/exams/start")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseExamSession(envelope.getJSONObject("data"))
        }
    }

    suspend fun initGuestSession(
        baseUrl: String,
        guestDeviceId: String,
        displayName: String,
        selectedClass: Int,
        selectedBoard: String,
        selectedSubBoard: String = "PESHAWAR",
        studyGroup: String = "SCIENCE_BIOLOGY",
        language: String = "en"
    ): ApiResult<ApiAuthSessionDto> {
        val payload = JSONObject()
            .put("guestDeviceId", guestDeviceId)
            .put("displayName", displayName)
            .put("selectedClass", selectedClass)
            .put("selectedBoard", selectedBoard)
            .put("selectedSubBoard", selectedSubBoard)
            .put("studyGroup", studyGroup)
            .put("language", language)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/auth/guest")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseAuthSession(envelope.getJSONObject("data"))
        }
    }

    suspend fun exchangeGoogleIdToken(
        baseUrl: String,
        idToken: String,
        guestDeviceId: String,
        selectedClass: Int,
        selectedBoard: String,
        selectedSubBoard: String = "PESHAWAR",
        studyGroup: String = "SCIENCE_BIOLOGY",
        language: String = "en"
    ): ApiResult<ApiAuthSessionDto> {
        val payload = JSONObject()
            .put("idToken", idToken)
            .put("guestDeviceId", guestDeviceId)
            .put("selectedClass", selectedClass)
            .put("selectedBoard", selectedBoard)
            .put("selectedSubBoard", selectedSubBoard)
            .put("studyGroup", studyGroup)
            .put("language", language)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/auth/google/exchange")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseAuthSession(envelope.getJSONObject("data"))
        }
    }

    suspend fun syncProgress(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        selectedClass: Int,
        selectedBoard: String,
        studyHoursTotal: Double,
        dailyGoalPercent: Int,
        chapters: List<ApiChapterProgressSyncDto>,
        selectedSubBoard: String? = null,
        studyGroup: String? = null,
        language: String = "en",
        dailyTargetMinutes: Int? = null,
        examCountdownDateIso: String? = null,
        diagnosticCompleted: Boolean = false
    ): ApiResult<ApiStudentProgressDto> {
        val arr = JSONArray()
        for (ch in chapters) {
            arr.put(
                JSONObject()
                    .put("chapterId", ch.chapterId)
                    .put("subjectId", ch.subjectId)
                    .put("completionPercent", ch.completionPercent)
                    .put("lastBlockIndex", ch.lastBlockIndex)
                    .put("isCompleted", ch.isCompleted)
                    .put("lastAccessedAtIso", ch.lastAccessedAtIso)
            )
        }
        val payload = JSONObject()
            .put("guestDeviceId", guestDeviceId)
            .put("selectedClass", selectedClass)
            .put("selectedBoard", selectedBoard)
            .put("language", language)
            .put("studyHoursTotal", studyHoursTotal)
            .put("dailyGoalPercent", dailyGoalPercent)
            .put("diagnosticCompleted", diagnosticCompleted)
            .put("chapterProgress", arr)
        if (!selectedSubBoard.isNullOrBlank()) payload.put("selectedSubBoard", selectedSubBoard)
        if (!studyGroup.isNullOrBlank()) payload.put("studyGroup", studyGroup)
        if (dailyTargetMinutes != null) payload.put("dailyTargetMinutes", dailyTargetMinutes)
        if (!examCountdownDateIso.isNullOrBlank()) payload.put("examCountdownDateIso", examCountdownDateIso)

        val builder = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/progress/sync")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
        return executeJson(builder.build()) { envelope ->
            ApiContentJsonParser.parseStudentProgress(envelope.getJSONObject("data"))
        }
    }

    suspend fun submitQuiz(
        baseUrl: String,
        sessionId: String,
        guestDeviceId: String,
        bearerToken: String?,
        subjectId: String,
        chapterId: String?,
        answers: List<Pair<String, String?>>,
        elapsedSeconds: Int,
        submittedAtIso: String,
        examMode: String = "chapter_test",
        marksPerMcq: Double = 1.0,
        negativeMarkingPenalty: Double = 0.0,
        passPercentage: Int = 40,
        topicTitle: String? = null
    ): ApiResult<ApiQuizSubmissionResultDto> {
        val ansArr = JSONArray()
        for ((mcqId, optId) in answers) {
            ansArr.put(
                JSONObject()
                    .put("mcqId", mcqId)
                    .put("selectedOptionId", optId ?: JSONObject.NULL)
                    .put("markedForReview", false)
            )
        }
        val payload = JSONObject()
            .put("sessionId", sessionId)
            .put("guestDeviceId", guestDeviceId)
            .put("subjectId", subjectId)
            .put("chapterId", chapterId ?: JSONObject.NULL)
            .put("answers", ansArr)
            .put("elapsedSeconds", elapsedSeconds)
            .put("submittedAtIso", submittedAtIso)
            .put("examMode", examMode)
            .put("marksPerMcq", marksPerMcq)
            .put("negativeMarkingPenalty", negativeMarkingPenalty)
            .put("passPercentage", passPercentage)
        if (!topicTitle.isNullOrBlank()) payload.put("topicTitle", topicTitle)

        val builder = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/quizzes/submissions")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
        return executeJson(builder.build()) { envelope ->
            val d = envelope.getJSONObject("data")
            ApiQuizSubmissionResultDto(
                submissionId = d.optString("submissionId", ""),
                totalQuestions = d.optInt("totalQuestions", 0),
                correctCount = d.optInt("correctCount", 0),
                incorrectCount = d.optInt("incorrectCount", 0),
                unansweredCount = d.optInt("unansweredCount", 0),
                percentage = d.optInt("percentage", 0),
                elapsedSeconds = d.optInt("elapsedSeconds", elapsedSeconds),
                marksObtained = d.optDouble("marksObtained", d.optInt("correctCount", 0).toDouble()),
                totalMarks = d.optDouble("totalMarks", d.optInt("totalQuestions", 0).toDouble()),
                passed = d.optBoolean("passed", false),
                passPercentage = d.optInt("passPercentage", passPercentage),
                xpEarned = d.optInt("xpEarned", 0)
            )
        }
    }

    suspend fun fetchQuizHistory(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?
    ): ApiResult<List<ApiQuizHistoryItemDto>> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/quizzes")
            .withStudentAuth(guestDeviceId, bearerToken)
            .get()
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseQuizHistory(envelope.getJSONArray("data"))
        }
    }

    suspend fun fetchBookmarks(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?
    ): ApiResult<List<ApiStudentBookmarkDto>> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/bookmarks")
            .withStudentAuth(guestDeviceId, bearerToken)
            .get()
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseBookmarks(envelope.getJSONArray("data"))
        }
    }

    suspend fun upsertBookmark(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        itemType: String,
        itemId: String,
        chapterId: String?,
        subjectId: String?,
        title: String,
        subtitle: String
    ): ApiResult<Unit> {
        val payload = JSONObject()
            .put("itemType", itemType)
            .put("itemId", itemId)
            .put("chapterId", chapterId ?: JSONObject.NULL)
            .put("subjectId", subjectId ?: JSONObject.NULL)
            .put("title", title)
            .put("subtitle", subtitle)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/bookmarks")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { Unit }
    }

    suspend fun deleteBookmark(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        idOrItemId: String
    ): ApiResult<Unit> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/bookmarks/${encode(idOrItemId)}")
            .withStudentAuth(guestDeviceId, bearerToken)
            .delete()
            .build()
        return executeJson(req) { Unit }
    }

    suspend fun fetchMistakes(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        includeMastered: Boolean = false
    ): ApiResult<List<ApiMistakeRecordDto>> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/mistakes?includeMastered=$includeMastered")
            .withStudentAuth(guestDeviceId, bearerToken)
            .get()
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseMistakes(envelope.getJSONArray("data"))
        }
    }

    suspend fun resolveMistake(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        mcqId: String
    ): ApiResult<Unit> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/mistakes/${encode(mcqId)}/resolve")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { Unit }
    }

    suspend fun fetchFlashcards(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        classLevel: Int? = null,
        dueOnly: Boolean = false
    ): ApiResult<List<ApiFlashcardDto>> {
        val params = mutableListOf("dueOnly=$dueOnly")
        if (classLevel != null) params.add("classLevel=$classLevel")
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/flashcards?${params.joinToString("&")}")
            .withStudentAuth(guestDeviceId, bearerToken)
            .get()
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseFlashcards(envelope.getJSONArray("data"))
        }
    }

    suspend fun reviewFlashcard(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        flashcardId: String,
        quality: Int
    ): ApiResult<Unit> {
        val payload = JSONObject()
            .put("flashcardId", flashcardId)
            .put("quality", quality.coerceIn(0, 5))
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/flashcards/review")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { Unit }
    }

    suspend fun askTutor(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        chapterId: String?,
        subjectId: String?,
        classLevel: Int,
        boardCode: String,
        language: String,
        action: String,
        prompt: String
    ): ApiResult<ApiTutorResponseDto> {
        val payload = JSONObject()
            .put("chapterId", chapterId ?: JSONObject.NULL)
            .put("subjectId", subjectId ?: JSONObject.NULL)
            .put("classLevel", classLevel)
            .put("boardCode", boardCode)
            .put("language", language)
            .put("action", action)
            .put("prompt", prompt)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/tutor/ask")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseTutorResponse(envelope.getJSONObject("data"))
        }
    }

    suspend fun fetchStudyPlanner(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?
    ): ApiResult<ApiStudyPlannerStateDto> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/planner")
            .withStudentAuth(guestDeviceId, bearerToken)
            .get()
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseStudyPlanner(envelope.getJSONObject("data"))
        }
    }

    suspend fun createStudyPlanTask(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        title: String,
        subjectId: String?,
        chapterId: String?,
        plannedDateIso: String,
        estimatedMinutes: Int,
        taskType: String
    ): ApiResult<ApiStudyPlanTaskDto> {
        val payload = JSONObject()
            .put("title", title)
            .put("subjectId", subjectId ?: JSONObject.NULL)
            .put("chapterId", chapterId ?: JSONObject.NULL)
            .put("plannedDateIso", plannedDateIso)
            .put("estimatedMinutes", estimatedMinutes)
            .put("taskType", taskType)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/planner/tasks")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseStudyTask(envelope.getJSONObject("data"))
        }
    }

    suspend fun toggleStudyPlanTask(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        taskId: String,
        completed: Boolean
    ): ApiResult<Unit> {
        val payload = JSONObject().put("completed", completed)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/planner/tasks/${encode(taskId)}")
            .withStudentAuth(guestDeviceId, bearerToken)
            .patch(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { Unit }
    }

    suspend fun fetchMotivationSummary(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?
    ): ApiResult<ApiMotivationSummaryDto> {
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/motivation")
            .withStudentAuth(guestDeviceId, bearerToken)
            .get()
            .build()
        return executeJson(req) { envelope ->
            ApiContentJsonParser.parseMotivationSummary(envelope.getJSONObject("data"))
        }
    }

    suspend fun setLeaderboardOptIn(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        optIn: Boolean,
        aliasHandle: String?
    ): ApiResult<Unit> {
        val payload = JSONObject().put("optIn", optIn)
        if (!aliasHandle.isNullOrBlank()) payload.put("aliasHandle", aliasHandle)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/students/me/leaderboard-opt-in")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { Unit }
    }

    suspend fun submitFeedback(
        baseUrl: String,
        guestDeviceId: String,
        bearerToken: String?,
        category: String,
        subjectId: String?,
        chapterId: String?,
        mcqId: String?,
        title: String,
        description: String
    ): ApiResult<Unit> {
        val payload = JSONObject()
            .put("guestDeviceId", guestDeviceId)
            .put("category", category)
            .put("subjectId", subjectId ?: JSONObject.NULL)
            .put("chapterId", chapterId ?: JSONObject.NULL)
            .put("mcqId", mcqId ?: JSONObject.NULL)
            .put("title", title)
            .put("description", description)
        val req = Request.Builder()
            .url("${ApiConfig.normalizeBaseUrl(baseUrl)}/feedback")
            .withStudentAuth(guestDeviceId, bearerToken)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return executeJson(req) { Unit }
    }

    /**
     * Streams a real PDF from Worker `/v1/pdfs/{pdfId}/download` (backed by Backblaze B2)
     * into a temporary file, validates the `%PDF-` signature, and atomically renames to targetFile.
     */
    suspend fun downloadPdfToFile(
        baseUrl: String,
        pdfId: String,
        targetFile: File
    ): ApiResult<File> = withContext(Dispatchers.IO) {
        val url = "${ApiConfig.normalizeBaseUrl(baseUrl)}/pdfs/${encode(pdfId)}/download?mode=stream"
        val request = Request.Builder().url(url).get().build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyText = response.body?.string().orEmpty()
                    return@withContext parseErrorFailure(response.code, bodyText)
                }
                val body = response.body
                    ?: return@withContext ApiResult.Failure("Empty PDF response from server.", response.code, "EMPTY_BODY")

                targetFile.parentFile?.mkdirs()
                val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                var totalBytes = 0L
                val header = ByteArray(5)
                var headerRead = 0

                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            if (headerRead < 5) {
                                val toCopy = minOf(5 - headerRead, read)
                                System.arraycopy(buffer, 0, header, headerRead, toCopy)
                                headerRead += toCopy
                            }
                            output.write(buffer, 0, read)
                            totalBytes += read
                        }
                    }
                }

                val magic = String(header, 0, headerRead, Charsets.US_ASCII)
                if (headerRead < 5 || magic != "%PDF-") {
                    tmpFile.delete()
                    return@withContext ApiResult.Failure(
                        "Downloaded stream is not a valid PDF document (invalid header).",
                        502,
                        "INVALID_PDF_STREAM"
                    )
                }
                if (totalBytes <= 0L) {
                    tmpFile.delete()
                    return@withContext ApiResult.Failure("Downloaded PDF is empty.", 502, "EMPTY_PDF")
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tmpFile.renameTo(targetFile)) {
                    tmpFile.copyTo(targetFile, overwrite = true)
                    tmpFile.delete()
                }
                ApiResult.Success(targetFile)
            }
        } catch (err: Exception) {
            ApiResult.Failure(err.message ?: "Unable to download PDF.", null, "NETWORK_EXCEPTION")
        }
    }

    private suspend fun <T> executeJson(
        request: Request,
        parser: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext parseErrorFailure(response.code, rawBody)
                }
                val json = JSONObject(rawBody)
                val isOk = when {
                    json.has("ok") -> json.optBoolean("ok", true)
                    json.has("success") -> json.optBoolean("success", true)
                    else -> true
                }
                if (!isOk) {
                    val errObj = json.optJSONObject("error")
                    return@withContext ApiResult.Failure(
                        message = errObj?.optString("message") ?: "API request failed.",
                        statusCode = response.code,
                        code = errObj?.optString("code") ?: "API_ERROR"
                    )
                }
                ApiResult.Success(parser(json))
            }
        } catch (err: Exception) {
            ApiResult.Failure(
                message = err.message ?: "Network connection unavailable.",
                statusCode = null,
                code = "NETWORK_EXCEPTION"
            )
        }
    }

    private fun parseErrorFailure(statusCode: Int, rawBody: String): ApiResult.Failure {
        return try {
            val json = JSONObject(rawBody)
            val errObj = json.optJSONObject("error")
            ApiResult.Failure(
                message = errObj?.optString("message")?.ifBlank { null }
                    ?: "HTTP $statusCode error from Worker API.",
                statusCode = statusCode,
                code = errObj?.optString("code")?.ifBlank { null } ?: "HTTP_$statusCode"
            )
        } catch (_: Exception) {
            ApiResult.Failure(
                message = "HTTP $statusCode error from Worker API.",
                statusCode = statusCode,
                code = "HTTP_$statusCode"
            )
        }
    }
}

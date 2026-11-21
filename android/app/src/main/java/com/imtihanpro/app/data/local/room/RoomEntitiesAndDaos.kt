package com.imtihanpro.app.data.local.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_subjects")
data class CachedSubjectEntity(
    @PrimaryKey val id: String,
    val classLevel: Int,
    val boardCode: String,
    val code: String,
    val name: String,
    val iconKey: String,
    val totalChapters: Int,
    val publishedChapters: Int,
    val coverageStatus: String,
    val updatedAtEpochMs: Long,
    val studyGroupsCsv: String = "",
    val pairingSchemeSummary: String? = null,
    val offlinePackSizeMb: Double = 0.6
)

@Entity(tableName = "cached_chapter_meta")
data class CachedChapterMetaEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val chapterNumber: Int,
    val title: String,
    val subtitle: String,
    val currentVersion: String,
    val publishStatus: String,
    val mcqCount: Int,
    val solvedQuestionsCount: Int,
    val isLockedByDefault: Boolean,
    val estimatedSizeKb: Int,
    val topicsJson: String = "[]",
    val titleUrdu: String? = null,
    val subtitleUrdu: String? = null,
    val boardWeightageMarks: Int = 0,
    val pairingHint: String? = null,
    val nightBeforeSummary: String? = null
)

@Entity(tableName = "cached_chapters")
data class CachedChapterEntity(
    @PrimaryKey val chapterId: String,
    val subjectId: String,
    val chapterNumber: Int,
    val title: String,
    val contentVersion: String,
    val serializedBlocksJson: String,
    val downloadedAtEpochMs: Long,
    val sizeKb: Int
)

@Entity(tableName = "cached_mcqs")
data class CachedMcqEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val subjectId: String,
    val question: String,
    val optionsJson: String,
    val correctOptionId: String,
    val explanation: String,
    val difficulty: String = "medium",
    val topicTitle: String = "Core Concepts",
    val questionUrdu: String? = null,
    val optionsUrduJson: String = "[]",
    val explanationUrdu: String? = null
)

@Entity(tableName = "cached_past_papers")
data class CachedPastPaperEntity(
    @PrimaryKey val id: String,
    val boardCode: String,
    val classLevel: Int,
    val subjectId: String,
    val subjectName: String,
    val year: Int,
    val isSolved: Boolean,
    val pdfMetadataId: String,
    val fileSizeMb: Double,
    val pageCount: Int
)

@Entity(tableName = "guest_progress")
data class GuestProgressEntity(
    @PrimaryKey val chapterId: String,
    val subjectId: String,
    val completionPercent: Int,
    val lastReadBlockIndex: Int,
    val isCompleted: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "downloaded_pdfs")
data class DownloadedPdfEntity(
    @PrimaryKey val pdfId: String,
    val title: String,
    val categoryLabel: String,
    val localFilePath: String,
    val sizeMb: Double,
    val downloadedAtEpochMs: Long
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val pdfId: String,
    val paperId: String,
    val title: String,
    val categoryLabel: String,
    val remoteUrl: String,
    val state: String,
    val pausedReason: String = "NONE",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etag: String? = null,
    val expectedSha256: String? = null,
    val retryCount: Int = 0,
    val nextRetryAtMs: Long = 0L,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val localPath: String? = null,
    val tmpPath: String = "",
    val allowMobileData: Boolean = false,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

@Entity(tableName = "pdf_reading_state")
data class PdfReadingStateEntity(
    @PrimaryKey val pdfId: String,
    val lastPage: Int,
    val totalPages: Int,
    val zoomPercent: Int,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "pdf_bookmarks")
data class PdfBookmarkEntity(
    @PrimaryKey val id: String,
    val pdfId: String,
    val pageNumber: Int,
    val label: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "pdf_annotations")
data class PdfAnnotationEntity(
    @PrimaryKey val id: String,
    val pdfId: String,
    val pageNumber: Int,
    val noteText: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "pending_sync_queue")
data class PendingSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val attemptCount: Int = 0
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String?,
    val chapterTitle: String?,
    val examMode: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val percentage: Int,
    val marksObtained: Double,
    val totalMarks: Double,
    val passed: Boolean,
    val elapsedSeconds: Int,
    val submittedAtIso: String,
    val submittedAtEpochMs: Long
)

@Entity(tableName = "mistake_questions")
data class MistakeQuestionEntity(
    @PrimaryKey val mcqId: String,
    val subjectId: String,
    val chapterId: String,
    val topicTitle: String,
    val question: String,
    val optionsJson: String,
    val lastWrongOptionId: String?,
    val correctOptionId: String,
    val explanation: String,
    val wrongCount: Int,
    val mastered: Boolean,
    val lastAttemptedAtEpochMs: Long
)

@Entity(tableName = "student_bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val itemType: String, // "chapter" | "note_block" | "mcq" | "solved_question" | "pdf"
    val itemId: String,
    val chapterId: String?,
    val subjectId: String?,
    val title: String,
    val subtitle: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "flashcard_reviews")
data class FlashcardReviewEntity(
    @PrimaryKey val flashcardId: String,
    val subjectId: String,
    val chapterId: String,
    val front: String,
    val back: String,
    val formulaHint: String?,
    val topicTitle: String,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitions: Int,
    val nextReviewEpochMs: Long,
    val lastReviewedEpochMs: Long?,
    val frontUrdu: String? = null,
    val backUrdu: String? = null
)

@Entity(tableName = "study_plan_tasks")
data class StudyPlanTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subjectId: String?,
    val chapterId: String?,
    val plannedDateIso: String,
    val estimatedMinutes: Int,
    val taskType: String,
    val completed: Boolean,
    val completedAtIso: String?,
    val createdAtEpochMs: Long
)

@Entity(tableName = "study_activity_days")
data class StudyActivityDayEntity(
    @PrimaryKey val dayYmd: String,
    val minutesLogged: Int,
    val quizzesCompleted: Int,
    val chaptersStudied: Int
)

@Entity(tableName = "pending_feedback")
data class PendingFeedbackEntity(
    @PrimaryKey val id: String,
    val category: String,
    val subjectId: String?,
    val chapterId: String?,
    val mcqId: String?,
    val title: String,
    val description: String,
    val synced: Boolean,
    val createdAtEpochMs: Long
)

@Dao
interface CurriculumCacheDao {
    @Query("SELECT * FROM cached_subjects WHERE classLevel = :classLevel AND boardCode = :boardCode ORDER BY name ASC")
    fun observeSubjects(classLevel: Int, boardCode: String): Flow<List<CachedSubjectEntity>>

    @Query("SELECT * FROM cached_subjects WHERE classLevel = :classLevel AND boardCode = :boardCode")
    suspend fun getSubjectsOnce(classLevel: Int, boardCode: String): List<CachedSubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(items: List<CachedSubjectEntity>)

    @Query("SELECT * FROM cached_chapter_meta WHERE subjectId = :subjectId ORDER BY chapterNumber ASC")
    fun observeChapterMeta(subjectId: String): Flow<List<CachedChapterMetaEntity>>

    @Query("SELECT * FROM cached_chapter_meta")
    suspend fun getAllChapterMetaOnce(): List<CachedChapterMetaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapterMeta(items: List<CachedChapterMetaEntity>)

    @Query("SELECT * FROM cached_mcqs WHERE chapterId = :chapterId")
    fun observeMcqs(chapterId: String): Flow<List<CachedMcqEntity>>

    @Query("SELECT * FROM cached_mcqs WHERE subjectId = :subjectId")
    suspend fun getMcqsBySubjectOnce(subjectId: String): List<CachedMcqEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMcqs(items: List<CachedMcqEntity>)

    @Query("SELECT * FROM cached_past_papers WHERE classLevel = :classLevel ORDER BY year DESC")
    fun observePastPapers(classLevel: Int): Flow<List<CachedPastPaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPastPapers(items: List<CachedPastPaperEntity>)
}

@Dao
interface OfflineContentDao {
    @Query("SELECT * FROM cached_chapters WHERE subjectId = :subjectId ORDER BY chapterNumber ASC")
    fun observeCachedChapters(subjectId: String): Flow<List<CachedChapterEntity>>

    @Query("SELECT * FROM cached_chapters WHERE chapterId = :chapterId LIMIT 1")
    fun observeCachedChapter(chapterId: String): Flow<CachedChapterEntity?>

    @Query("SELECT * FROM cached_chapters WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getCachedChapterOnce(chapterId: String): CachedChapterEntity?

    @Query("SELECT chapterId FROM cached_chapters")
    fun observeDownloadedChapterIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedChapter(entity: CachedChapterEntity)

    @Query("DELETE FROM cached_chapters WHERE chapterId = :chapterId")
    suspend fun removeCachedChapter(chapterId: String)

    @Query("SELECT * FROM downloaded_pdfs ORDER BY downloadedAtEpochMs DESC")
    fun observeDownloadedPdfs(): Flow<List<DownloadedPdfEntity>>

    @Query("SELECT * FROM downloaded_pdfs WHERE pdfId = :pdfId LIMIT 1")
    suspend fun getDownloadedPdfOnce(pdfId: String): DownloadedPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedPdf(entity: DownloadedPdfEntity)

    @Query("DELETE FROM downloaded_pdfs WHERE pdfId = :pdfId")
    suspend fun removeDownloadedPdf(pdfId: String)

    @Query("DELETE FROM downloaded_pdfs")
    suspend fun clearAllDownloads()
}

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE pdfId = :pdfId LIMIT 1")
    fun observeById(pdfId: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM download_tasks WHERE pdfId = :pdfId LIMIT 1")
    suspend fun getByIdOnce(pdfId: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE state IN ('RUNNING', 'QUEUED', 'VERIFYING') ORDER BY CASE state WHEN 'RUNNING' THEN 0 WHEN 'VERIFYING' THEN 1 ELSE 2 END, createdAtEpochMs ASC LIMIT 1")
    suspend fun getNextRunnableOnce(): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE state = 'QUEUED' ORDER BY createdAtEpochMs ASC")
    suspend fun getQueuedOnce(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE state = 'PAUSED' AND pausedReason IN ('WAITING_WIFI', 'WAITING_NETWORK', 'RETRY_BACKOFF') ORDER BY updatedAtEpochMs ASC")
    suspend fun getAutoResumablePausedOnce(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE state IN ('RUNNING', 'VERIFYING')")
    suspend fun getOrphanedRunningOnce(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE state = 'FAILED'")
    suspend fun getFailedOnce(): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadTaskEntity)

    @Query(
        "UPDATE download_tasks SET bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, speedBytesPerSec = :speedBps, etag = COALESCE(:etag, etag), updatedAtEpochMs = :updatedAt WHERE pdfId = :pdfId"
    )
    suspend fun updateProgress(
        pdfId: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        speedBps: Long,
        etag: String?,
        updatedAt: Long
    )

    @Query(
        "UPDATE download_tasks SET totalBytes = :totalBytes, etag = COALESCE(:etag, etag), expectedSha256 = COALESCE(:expectedSha256, expectedSha256), updatedAtEpochMs = :updatedAt WHERE pdfId = :pdfId"
    )
    suspend fun updateMetadata(
        pdfId: String,
        totalBytes: Long,
        etag: String?,
        expectedSha256: String?,
        updatedAt: Long
    )

    @Query(
        "UPDATE download_tasks SET state = :state, pausedReason = :pausedReason, errorCode = :errorCode, errorMessage = :errorMessage, speedBytesPerSec = 0, updatedAtEpochMs = :updatedAt WHERE pdfId = :pdfId"
    )
    suspend fun updateState(
        pdfId: String,
        state: String,
        pausedReason: String,
        errorCode: String?,
        errorMessage: String?,
        updatedAt: Long
    )

    @Query("UPDATE download_tasks SET allowMobileData = :allowMobileData, state = 'QUEUED', pausedReason = 'NONE', errorCode = NULL, errorMessage = NULL, updatedAtEpochMs = :updatedAt WHERE pdfId = :pdfId")
    suspend fun allowMobileDataAndQueue(pdfId: String, allowMobileData: Boolean, updatedAt: Long)

    @Query("DELETE FROM download_tasks WHERE pdfId = :pdfId")
    suspend fun deleteById(pdfId: String)

    @Query("DELETE FROM download_tasks")
    suspend fun deleteAll()
}

@Dao
interface GuestProgressDao {
    @Query("SELECT * FROM guest_progress")
    fun observeAllProgress(): Flow<List<GuestProgressEntity>>

    @Query("SELECT * FROM guest_progress")
    suspend fun getAllProgressOnce(): List<GuestProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: GuestProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllProgress(entities: List<GuestProgressEntity>)
}

@Dao
interface PdfReaderDao {
    @Query("SELECT * FROM pdf_reading_state WHERE pdfId = :pdfId LIMIT 1")
    suspend fun getReadingStateOnce(pdfId: String): PdfReadingStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingState(entity: PdfReadingStateEntity)

    @Query("SELECT * FROM pdf_bookmarks WHERE pdfId = :pdfId ORDER BY pageNumber ASC")
    fun observeBookmarks(pdfId: String): Flow<List<PdfBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(entity: PdfBookmarkEntity)

    @Query("DELETE FROM pdf_bookmarks WHERE pdfId = :pdfId AND pageNumber = :pageNumber")
    suspend fun removeBookmarkForPage(pdfId: String, pageNumber: Int)

    @Query("SELECT * FROM pdf_annotations WHERE pdfId = :pdfId ORDER BY pageNumber ASC, createdAtEpochMs DESC")
    fun observeAnnotations(pdfId: String): Flow<List<PdfAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnotation(entity: PdfAnnotationEntity)

    @Query("DELETE FROM pdf_annotations WHERE id = :id")
    suspend fun deleteAnnotation(id: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM pending_sync_queue ORDER BY createdAtEpochMs ASC")
    suspend fun getAllPending(): List<PendingSyncQueueEntity>

    @Query("SELECT COUNT(*) FROM pending_sync_queue")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingSyncQueueEntity)

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_sync_queue SET attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: Long)
}

@Dao
interface QuizAttemptDao {
    @Query("SELECT * FROM quiz_attempts ORDER BY submittedAtEpochMs DESC LIMIT 50")
    fun observeQuizHistory(): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts ORDER BY submittedAtEpochMs DESC")
    suspend fun getAllAttemptsOnce(): List<QuizAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)
}

@Dao
interface MistakeQuestionDao {
    @Query("SELECT * FROM mistake_questions WHERE mastered = 0 ORDER BY lastAttemptedAtEpochMs DESC")
    fun observeActiveMistakes(): Flow<List<MistakeQuestionEntity>>

    @Query("SELECT * FROM mistake_questions ORDER BY mastered ASC, lastAttemptedAtEpochMs DESC")
    fun observeAllMistakes(): Flow<List<MistakeQuestionEntity>>

    @Query("SELECT * FROM mistake_questions WHERE mcqId = :mcqId LIMIT 1")
    suspend fun getByIdOnce(mcqId: String): MistakeQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMistake(entity: MistakeQuestionEntity)

    @Query("UPDATE mistake_questions SET mastered = 1, lastAttemptedAtEpochMs = :epochMs WHERE mcqId = :mcqId")
    suspend fun markMastered(mcqId: String, epochMs: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM student_bookmarks ORDER BY createdAtEpochMs DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM student_bookmarks WHERE itemType = :itemType AND itemId = :itemId LIMIT 1")
    suspend fun findByTypeAndItemId(itemType: String, itemId: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(entity: BookmarkEntity)

    @Query("DELETE FROM student_bookmarks WHERE id = :id OR itemId = :id")
    suspend fun deleteByIdOrItemId(id: String)
}

@Dao
interface FlashcardReviewDao {
    @Query("SELECT * FROM flashcard_reviews ORDER BY nextReviewEpochMs ASC")
    fun observeAllFlashcards(): Flow<List<FlashcardReviewEntity>>

    @Query("SELECT * FROM flashcard_reviews WHERE nextReviewEpochMs <= :nowEpochMs ORDER BY nextReviewEpochMs ASC")
    fun observeDueFlashcards(nowEpochMs: Long): Flow<List<FlashcardReviewEntity>>

    @Query("SELECT * FROM flashcard_reviews WHERE flashcardId = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): FlashcardReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FlashcardReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOne(item: FlashcardReviewEntity)
}

@Dao
interface StudyPlannerDao {
    @Query("SELECT * FROM study_plan_tasks ORDER BY completed ASC, plannedDateIso ASC, createdAtEpochMs DESC")
    fun observeTasks(): Flow<List<StudyPlanTaskEntity>>

    @Query("SELECT * FROM study_plan_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskOnce(id: String): StudyPlanTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: StudyPlanTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<StudyPlanTaskEntity>)

    @Query("DELETE FROM study_plan_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}

@Dao
interface StudyActivityDao {
    @Query("SELECT * FROM study_activity_days ORDER BY dayYmd DESC LIMIT 14")
    fun observeRecentActivity(): Flow<List<StudyActivityDayEntity>>

    @Query("SELECT * FROM study_activity_days ORDER BY dayYmd ASC")
    suspend fun getAllActivityOnce(): List<StudyActivityDayEntity>

    @Query("SELECT * FROM study_activity_days WHERE dayYmd = :dayYmd LIMIT 1")
    suspend fun getDayOnce(dayYmd: String): StudyActivityDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(entity: StudyActivityDayEntity)
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM pending_feedback ORDER BY createdAtEpochMs DESC")
    fun observeFeedback(): Flow<List<PendingFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(entity: PendingFeedbackEntity)
}

@Database(
    entities = [
        CachedSubjectEntity::class,
        CachedChapterMetaEntity::class,
        CachedChapterEntity::class,
        CachedMcqEntity::class,
        CachedPastPaperEntity::class,
        GuestProgressEntity::class,
        DownloadedPdfEntity::class,
        PdfReadingStateEntity::class,
        PdfBookmarkEntity::class,
        PdfAnnotationEntity::class,
        PendingSyncQueueEntity::class,
        QuizAttemptEntity::class,
        MistakeQuestionEntity::class,
        BookmarkEntity::class,
        FlashcardReviewEntity::class,
        StudyPlanTaskEntity::class,
        StudyActivityDayEntity::class,
        PendingFeedbackEntity::class,
        DownloadTaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ImtihanDatabase : RoomDatabase() {
    abstract fun curriculumCacheDao(): CurriculumCacheDao
    abstract fun offlineContentDao(): OfflineContentDao
    abstract fun guestProgressDao(): GuestProgressDao
    abstract fun pdfReaderDao(): PdfReaderDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun mistakeQuestionDao(): MistakeQuestionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun flashcardReviewDao(): FlashcardReviewDao
    abstract fun studyPlannerDao(): StudyPlannerDao
    abstract fun studyActivityDao(): StudyActivityDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun downloadTaskDao(): DownloadTaskDao
}

object ImtihanDatabaseProvider {
    @Volatile
    private var INSTANCE: ImtihanDatabase? = null

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cached_subjects ADD COLUMN studyGroupsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE cached_chapter_meta ADD COLUMN topicsJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE cached_mcqs ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'medium'")
            db.execSQL("ALTER TABLE cached_mcqs ADD COLUMN topicTitle TEXT NOT NULL DEFAULT 'Core Concepts'")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS quiz_attempts (
                    id TEXT NOT NULL PRIMARY KEY,
                    sessionId TEXT NOT NULL,
                    subjectId TEXT NOT NULL,
                    subjectName TEXT NOT NULL,
                    chapterId TEXT,
                    chapterTitle TEXT,
                    examMode TEXT NOT NULL,
                    totalQuestions INTEGER NOT NULL,
                    correctCount INTEGER NOT NULL,
                    incorrectCount INTEGER NOT NULL,
                    unansweredCount INTEGER NOT NULL,
                    percentage INTEGER NOT NULL,
                    marksObtained REAL NOT NULL,
                    totalMarks REAL NOT NULL,
                    passed INTEGER NOT NULL,
                    elapsedSeconds INTEGER NOT NULL,
                    submittedAtIso TEXT NOT NULL,
                    submittedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mistake_questions (
                    mcqId TEXT NOT NULL PRIMARY KEY,
                    subjectId TEXT NOT NULL,
                    chapterId TEXT NOT NULL,
                    topicTitle TEXT NOT NULL,
                    question TEXT NOT NULL,
                    optionsJson TEXT NOT NULL,
                    lastWrongOptionId TEXT,
                    correctOptionId TEXT NOT NULL,
                    explanation TEXT NOT NULL,
                    wrongCount INTEGER NOT NULL,
                    mastered INTEGER NOT NULL,
                    lastAttemptedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS student_bookmarks (
                    id TEXT NOT NULL PRIMARY KEY,
                    itemType TEXT NOT NULL,
                    itemId TEXT NOT NULL,
                    chapterId TEXT,
                    subjectId TEXT,
                    title TEXT NOT NULL,
                    subtitle TEXT NOT NULL,
                    createdAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS flashcard_reviews (
                    flashcardId TEXT NOT NULL PRIMARY KEY,
                    subjectId TEXT NOT NULL,
                    chapterId TEXT NOT NULL,
                    front TEXT NOT NULL,
                    back TEXT NOT NULL,
                    formulaHint TEXT,
                    topicTitle TEXT NOT NULL,
                    intervalDays INTEGER NOT NULL,
                    easeFactor REAL NOT NULL,
                    repetitions INTEGER NOT NULL,
                    nextReviewEpochMs INTEGER NOT NULL,
                    lastReviewedEpochMs INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS study_plan_tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    subjectId TEXT,
                    chapterId TEXT,
                    plannedDateIso TEXT NOT NULL,
                    estimatedMinutes INTEGER NOT NULL,
                    taskType TEXT NOT NULL,
                    completed INTEGER NOT NULL,
                    completedAtIso TEXT,
                    createdAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS study_activity_days (
                    dayYmd TEXT NOT NULL PRIMARY KEY,
                    minutesLogged INTEGER NOT NULL,
                    quizzesCompleted INTEGER NOT NULL,
                    chaptersStudied INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pending_feedback (
                    id TEXT NOT NULL PRIMARY KEY,
                    category TEXT NOT NULL,
                    subjectId TEXT,
                    chapterId TEXT,
                    mcqId TEXT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    synced INTEGER NOT NULL,
                    createdAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cached_subjects ADD COLUMN pairingSchemeSummary TEXT")
            db.execSQL("ALTER TABLE cached_subjects ADD COLUMN offlinePackSizeMb REAL NOT NULL DEFAULT 0.6")
            db.execSQL("ALTER TABLE cached_chapter_meta ADD COLUMN titleUrdu TEXT")
            db.execSQL("ALTER TABLE cached_chapter_meta ADD COLUMN subtitleUrdu TEXT")
            db.execSQL("ALTER TABLE cached_chapter_meta ADD COLUMN boardWeightageMarks INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE cached_chapter_meta ADD COLUMN pairingHint TEXT")
            db.execSQL("ALTER TABLE cached_chapter_meta ADD COLUMN nightBeforeSummary TEXT")
            db.execSQL("ALTER TABLE cached_mcqs ADD COLUMN questionUrdu TEXT")
            db.execSQL("ALTER TABLE cached_mcqs ADD COLUMN optionsUrduJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE cached_mcqs ADD COLUMN explanationUrdu TEXT")
            db.execSQL("ALTER TABLE flashcard_reviews ADD COLUMN frontUrdu TEXT")
            db.execSQL("ALTER TABLE flashcard_reviews ADD COLUMN backUrdu TEXT")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS download_tasks (
                    pdfId TEXT NOT NULL PRIMARY KEY,
                    paperId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    categoryLabel TEXT NOT NULL,
                    remoteUrl TEXT NOT NULL,
                    state TEXT NOT NULL,
                    pausedReason TEXT NOT NULL,
                    bytesDownloaded INTEGER NOT NULL,
                    totalBytes INTEGER NOT NULL,
                    speedBytesPerSec INTEGER NOT NULL,
                    etag TEXT,
                    expectedSha256 TEXT,
                    retryCount INTEGER NOT NULL,
                    nextRetryAtMs INTEGER NOT NULL,
                    errorCode TEXT,
                    errorMessage TEXT,
                    localPath TEXT,
                    tmpPath TEXT NOT NULL,
                    allowMobileData INTEGER NOT NULL,
                    createdAtEpochMs INTEGER NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_download_tasks_state ON download_tasks(state, updatedAtEpochMs)")
        }
    }

    fun getInstance(context: Context): ImtihanDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                ImtihanDatabase::class.java,
                "imtihan_pro_local.db"
            )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}

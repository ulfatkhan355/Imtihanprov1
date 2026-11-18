import { KPK_SUB_BOARDS, subjectMatchesStudyGroup } from '../../../shared-contracts/rules';
import type {
  ApiAcademicClass,
  ApiAdPlacementConfig,
  ApiAdminAuditItem,
  ApiAiAgentSubmission,
  ApiBoard,
  ApiChapter,
  ApiChapterProgressItem,
  ApiEducationalContent,
  ApiExamPattern,
  ApiFeedbackReport,
  ApiMcqItem,
  ApiPastPaper,
  ApiPdfMetadata,
  ApiSearchResults,
  ApiStudentProgress,
  ApiSubject,
  ApiSubjectivePrompt,
  ApiTopic,
  BoardCode,
  ClassLevel,
  ContentCoverageStatus,
  EducationalContentBlock,
  KpkSubBoardCode,
  LanguageCode,
  McqOption,
  PublishStatus,
  StudyGroupCode,
  SubjectIconKey,
} from '../../../shared-contracts/types';
import type { D1Database } from '../env';

export async function listBoards(db: D1Database): Promise<ApiBoard[]> {
  const res = await db
    .prepare(
      'SELECT id, code, name, region, is_primary_initial_focus, is_active, supported_classes_json FROM boards ORDER BY is_primary_initial_focus DESC, code ASC'
    )
    .all<{
      id: string;
      code: BoardCode;
      name: string;
      region: string;
      is_primary_initial_focus: number;
      is_active: number;
      supported_classes_json: string;
    }>();

  return res.results.map((r) => ({
    id: r.id,
    code: r.code,
    name: r.name,
    region: r.region,
    isPrimaryInitialFocus: r.is_primary_initial_focus === 1,
    isActive: r.is_active === 1,
    supportedClasses: safeJsonParse<ClassLevel[]>(r.supported_classes_json, [9, 10, 11, 12]),
    subBoards: r.code === 'KPK' ? KPK_SUB_BOARDS : undefined,
  }));
}

export async function listAcademicClasses(
  db: D1Database,
  boardCode?: string
): Promise<ApiAcademicClass[]> {
  const query = boardCode
    ? `SELECT ac.id, ac.level, ac.label, ac.board_id, ac.board_code, ac.academic_year, ac.coverage_status, ac.notes,
       (SELECT COUNT(*) FROM subjects s WHERE s.class_id = ac.id) AS subject_count,
       (SELECT COALESCE(SUM(s.published_chapters), 0) FROM subjects s WHERE s.class_id = ac.id) AS published_chapters_count
       FROM academic_classes ac WHERE ac.board_code = ? ORDER BY ac.level ASC`
    : `SELECT ac.id, ac.level, ac.label, ac.board_id, ac.board_code, ac.academic_year, ac.coverage_status, ac.notes,
       (SELECT COUNT(*) FROM subjects s WHERE s.class_id = ac.id) AS subject_count,
       (SELECT COALESCE(SUM(s.published_chapters), 0) FROM subjects s WHERE s.class_id = ac.id) AS published_chapters_count
       FROM academic_classes ac ORDER BY ac.board_code ASC, ac.level ASC`;

  const stmt = boardCode ? db.prepare(query).bind(boardCode) : db.prepare(query);
  const res = await stmt.all<{
    id: string;
    level: ClassLevel;
    label: string;
    board_id: string;
    board_code: BoardCode;
    academic_year: string;
    coverage_status: ContentCoverageStatus;
    notes: string;
    subject_count: number;
    published_chapters_count: number;
  }>();

  return res.results.map((r) => ({
    id: r.id,
    level: r.level,
    label: r.label,
    boardId: r.board_id,
    boardCode: r.board_code,
    academicYear: r.academic_year,
    coverageStatus: r.coverage_status,
    subjectCount: Number(r.subject_count || 0),
    publishedChaptersCount: Number(r.published_chapters_count || 0),
    notes: r.notes,
  }));
}

export async function listSubjects(
  db: D1Database,
  filters?: { boardCode?: string; classLevel?: number; studyGroup?: StudyGroupCode }
): Promise<ApiSubject[]> {
  const clauses: string[] = [];
  const params: unknown[] = [];
  if (filters?.boardCode) {
    clauses.push('board_code = ?');
    params.push(filters.boardCode);
  }
  if (filters?.classLevel) {
    clauses.push('class_level = ?');
    params.push(filters.classLevel);
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const sql = `SELECT id, class_id, class_level, board_code, code, name, icon_key, accent_hex, total_chapters, published_chapters, is_offline_pack_available, coverage_status, updated_at_iso, study_groups_json, description, textbook_pdf_id, pairing_scheme_summary, offline_pack_size_mb FROM subjects ${where} ORDER BY class_level ASC, name ASC`;
  const stmt = params.length ? db.prepare(sql).bind(...params) : db.prepare(sql);
  const res = await stmt.all<{
    id: string;
    class_id: string;
    class_level: ClassLevel;
    board_code: BoardCode;
    code: string;
    name: string;
    icon_key: SubjectIconKey;
    accent_hex: string;
    total_chapters: number;
    published_chapters: number;
    is_offline_pack_available: number;
    coverage_status: ContentCoverageStatus;
    updated_at_iso: string;
    study_groups_json: string;
    description: string;
    textbook_pdf_id: string | null;
    pairing_scheme_summary?: string;
    offline_pack_size_mb?: number;
  }>();

  const mapped = res.results.map((r) => ({
    id: r.id,
    classId: r.class_id,
    classLevel: r.class_level,
    boardCode: r.board_code,
    code: r.code,
    name: r.name,
    iconKey: r.icon_key,
    accentHex: r.accent_hex,
    totalChapters: Number(r.total_chapters),
    publishedChapters: Number(r.published_chapters),
    isOfflinePackAvailable: r.is_offline_pack_available === 1,
    coverageStatus: r.coverage_status,
    updatedAtIso: r.updated_at_iso,
    studyGroups: safeJsonParse<StudyGroupCode[]>(r.study_groups_json, []),
    description: r.description || '',
    textbookPdfId: r.textbook_pdf_id ?? null,
    pairingSchemeSummary: r.pairing_scheme_summary || '',
    offlinePackSizeMb: Number(r.offline_pack_size_mb ?? 0.6),
  }));

  if (filters?.studyGroup) {
    return mapped.filter((s) => subjectMatchesStudyGroup(s, filters.studyGroup));
  }
  return mapped;
}

export async function listChapters(
  db: D1Database,
  subjectId?: string
): Promise<ApiChapter[]> {
  const sql = subjectId
    ? `SELECT c.*, s.board_code, s.class_level FROM chapters c JOIN subjects s ON s.id = c.subject_id WHERE c.subject_id = ? ORDER BY c.chapter_number ASC`
    : `SELECT c.*, s.board_code, s.class_level FROM chapters c JOIN subjects s ON s.id = c.subject_id ORDER BY c.subject_id ASC, c.chapter_number ASC`;
  const stmt = subjectId ? db.prepare(sql).bind(subjectId) : db.prepare(sql);
  const res = await stmt.all<{
    id: string;
    subject_id: string;
    chapter_number: number;
    title: string;
    subtitle: string;
    topics_json: string;
    current_version: string;
    publish_status: PublishStatus;
    has_detailed_notes: number;
    has_revision_notes: number;
    mcq_count: number;
    solved_questions_count: number;
    is_locked_by_default: number;
    estimated_size_kb: number;
    updated_at_iso: string;
    board_code: BoardCode;
    class_level: ClassLevel;
    curriculum_version?: string;
    difficulty_tag?: 'easy' | 'medium' | 'hard';
    source_reference?: string;
    reviewed_by_name?: string | null;
    title_urdu?: string;
    subtitle_urdu?: string;
    board_weightage_marks?: number;
    pairing_hint?: string;
    night_before_summary?: string;
  }>();

  return res.results.map((r) => ({
    id: r.id,
    subjectId: r.subject_id,
    chapterNumber: Number(r.chapter_number),
    title: r.title,
    subtitle: r.subtitle,
    topics: safeJsonParse<ApiTopic[]>(r.topics_json, []),
    currentVersion: r.current_version,
    publishStatus: r.publish_status,
    hasDetailedNotes: r.has_detailed_notes === 1,
    hasRevisionNotes: r.has_revision_notes === 1,
    mcqCount: Number(r.mcq_count),
    solvedQuestionsCount: Number(r.solved_questions_count),
    isLockedByDefault: r.is_locked_by_default === 1,
    estimatedSizeKb: Number(r.estimated_size_kb),
    updatedAtIso: r.updated_at_iso,
    boardCode: r.board_code,
    classLevel: r.class_level,
    curriculumVersion: r.curriculum_version || 'KPK-2026',
    difficultyTag: r.difficulty_tag || 'medium',
    sourceReference: r.source_reference || 'KPK Textbook Board Peshawar',
    reviewedByName: r.reviewed_by_name ?? null,
    titleUrdu: r.title_urdu || undefined,
    subtitleUrdu: r.subtitle_urdu || undefined,
    boardWeightageMarks: Number(r.board_weightage_marks ?? 0),
    pairingHint: r.pairing_hint || undefined,
    nightBeforeSummary: r.night_before_summary || undefined,
  }));
}

export async function getChapterContent(
  db: D1Database,
  chapterId: string,
  mode: ApiEducationalContent['mode'],
  publishedOnly = true
): Promise<ApiEducationalContent | null> {
  const statusClause = publishedOnly ? "AND publish_status = 'published'" : '';
  const row = await db
    .prepare(
      `SELECT id, chapter_id, mode, version, publish_status, language, blocks_json, author_or_source, updated_at_iso
       FROM chapter_content_versions
       WHERE chapter_id = ? AND mode = ? ${statusClause}
       ORDER BY version_num DESC, updated_at_iso DESC LIMIT 1`
    )
    .bind(chapterId, mode)
    .first<{
      id: string;
      chapter_id: string;
      mode: ApiEducationalContent['mode'];
      version: string;
      publish_status: PublishStatus;
      language: LanguageCode;
      blocks_json: string;
      author_or_source: string;
      updated_at_iso: string;
    }>();

  if (!row) return null;
  return {
    id: row.id,
    chapterId: row.chapter_id,
    mode: row.mode,
    version: row.version,
    publishStatus: row.publish_status,
    language: row.language,
    blocks: safeJsonParse<EducationalContentBlock[]>(row.blocks_json, []),
    authorOrSource: row.author_or_source,
    updatedAtIso: row.updated_at_iso,
  };
}

export async function listLatestContents(db: D1Database, limit = 200): Promise<ApiEducationalContent[]> {
  const safeLimit = Math.max(1, Math.min(500, limit));
  const res = await db
    .prepare(
      `SELECT id, chapter_id, mode, version, publish_status, language, blocks_json, author_or_source, updated_at_iso
       FROM (
         SELECT id, chapter_id, mode, version, publish_status, language, blocks_json, author_or_source, updated_at_iso,
                ROW_NUMBER() OVER (PARTITION BY chapter_id, mode ORDER BY version_num DESC, updated_at_iso DESC) AS rn
         FROM chapter_content_versions
       )
       WHERE rn = 1
       ORDER BY updated_at_iso DESC
       LIMIT ?`
    )
    .bind(safeLimit)
    .all<{
      id: string;
      chapter_id: string;
      mode: ApiEducationalContent['mode'];
      version: string;
      publish_status: PublishStatus;
      language: LanguageCode;
      blocks_json: string;
      author_or_source: string;
      updated_at_iso: string;
    }>();

  return res.results.map((row) => ({
    id: row.id,
    chapterId: row.chapter_id,
    mode: row.mode,
    version: row.version,
    publishStatus: row.publish_status,
    language: row.language,
    blocks: safeJsonParse<EducationalContentBlock[]>(row.blocks_json, []),
    authorOrSource: row.author_or_source,
    updatedAtIso: row.updated_at_iso,
  }));
}

export async function listChapterMcqs(
  db: D1Database,
  chapterId: string,
  includeUnpublished = false,
  limit = 100,
  offset = 0
): Promise<ApiMcqItem[]> {
  const statusClause = includeUnpublished ? '' : "AND publish_status = 'published'";
  const safeLimit = Math.max(1, Math.min(200, limit));
  const safeOffset = Math.max(0, offset);
  const res = await db
    .prepare(
      `SELECT * FROM mcqs WHERE chapter_id = ? ${statusClause} ORDER BY created_at_iso ASC LIMIT ? OFFSET ?`
    )
    .bind(chapterId, safeLimit, safeOffset)
    .all<McqDbRow>();

  return res.results.map(mapMcqRow);
}

export async function listMcqsFiltered(
  db: D1Database,
  filters: {
    subjectId?: string | null;
    chapterId?: string | null;
    classLevel?: number;
    boardCode?: string;
    topicTitle?: string | null;
    difficulty?: string | null;
    ids?: string[];
    includeUnpublished?: boolean;
    limit?: number;
    offset?: number;
  }
): Promise<ApiMcqItem[]> {
  if (filters.ids && filters.ids.length > 0) {
    const cappedIds = filters.ids.slice(0, 100);
    const placeholders = cappedIds.map(() => '?').join(',');
    const res = await db
      .prepare(`SELECT * FROM mcqs WHERE id IN (${placeholders})`)
      .bind(...cappedIds)
      .all<McqDbRow>();
    return res.results.map(mapMcqRow);
  }

  const clauses: string[] = [];
  const params: unknown[] = [];

  if (!filters.includeUnpublished) {
    clauses.push("m.publish_status = 'published'");
  }
  if (filters.chapterId) {
    clauses.push('m.chapter_id = ?');
    params.push(filters.chapterId);
  }
  if (filters.subjectId) {
    clauses.push('m.subject_id = ?');
    params.push(filters.subjectId);
  }
  if (filters.classLevel) {
    clauses.push('m.class_level = ?');
    params.push(filters.classLevel);
  }
  if (filters.boardCode) {
    clauses.push('s.board_code = ?');
    params.push(filters.boardCode);
  }
  if (filters.topicTitle) {
    clauses.push('LOWER(m.topic_title) LIKE ?');
    params.push(`%${filters.topicTitle.trim().toLowerCase()}%`);
  }
  if (filters.difficulty && filters.difficulty !== 'mixed') {
    clauses.push('m.difficulty = ?');
    params.push(filters.difficulty);
  }

  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const limit = Math.max(1, Math.min(200, filters.limit ?? 100));
  const offset = Math.max(0, filters.offset ?? 0);
  const sql = `SELECT m.* FROM mcqs m LEFT JOIN subjects s ON s.id = m.subject_id ${where} ORDER BY m.created_at_iso ASC LIMIT ? OFFSET ?`;
  params.push(limit, offset);

  const res = await db.prepare(sql).bind(...params).all<McqDbRow>();
  return res.results.map(mapMcqRow);
}

interface McqDbRow {
  id: string;
  chapter_id: string;
  subject_id: string;
  class_level: ClassLevel;
  question: string;
  options_json: string;
  correct_option_id: 'A' | 'B' | 'C' | 'D';
  explanation: string;
  difficulty: 'easy' | 'medium' | 'hard';
  topic_title: string;
  past_paper_year: number | null;
  publish_status?: PublishStatus;
  source_reference?: string;
  question_urdu?: string;
  options_urdu_json?: string;
  explanation_urdu?: string;
}

function mapMcqRow(r: McqDbRow): ApiMcqItem {
  const parsedUrduOptions = safeJsonParse<McqOption[]>(r.options_urdu_json, []);
  return {
    id: r.id,
    chapterId: r.chapter_id,
    subjectId: r.subject_id,
    classLevel: r.class_level,
    question: r.question,
    options: safeJsonParse<McqOption[]>(r.options_json, []),
    correctOptionId: r.correct_option_id,
    explanation: r.explanation,
    difficulty: r.difficulty,
    topicTitle: r.topic_title,
    pastPaperYear: r.past_paper_year ?? undefined,
    publishStatus: r.publish_status || 'published',
    sourceReference: r.source_reference || 'KPK Textbook & Board Papers',
    questionUrdu: r.question_urdu || undefined,
    optionsUrdu: parsedUrduOptions.length === 4 ? parsedUrduOptions : undefined,
    explanationUrdu: r.explanation_urdu || undefined,
  };
}

export async function listSubjectivePromptsForExam(
  db: D1Database,
  filters: { subjectId?: string | null; chapterId?: string | null; classLevel?: number },
  maxPrompts = 4
): Promise<ApiSubjectivePrompt[]> {
  if (maxPrompts <= 0) return [];
  let sql = `SELECT ccv.blocks_json
    FROM chapter_content_versions ccv
    JOIN chapters ch ON ch.id = ccv.chapter_id
    JOIN subjects s ON s.id = ch.subject_id
    WHERE ccv.publish_status = 'published' AND ccv.mode IN ('detailed_notes', 'solved_questions')`;
  const params: unknown[] = [];
  if (filters.chapterId) {
    sql += ' AND ch.id = ?';
    params.push(filters.chapterId);
  }
  if (filters.subjectId) {
    sql += ' AND ch.subject_id = ?';
    params.push(filters.subjectId);
  }
  if (filters.classLevel) {
    sql += ' AND s.class_level = ?';
    params.push(filters.classLevel);
  }
  sql += ' ORDER BY ch.chapter_number ASC LIMIT 20';

  const res = await db.prepare(sql).bind(...params).all<{ blocks_json: string }>();
  const prompts: ApiSubjectivePrompt[] = [];
  for (const row of res.results) {
    const blocks = safeJsonParse<EducationalContentBlock[]>(row.blocks_json, []);
    for (const b of blocks) {
      if (b.type === 'solved_question') {
        prompts.push({
          id: b.id,
          questionType: b.questionType,
          marks: b.marks,
          question: b.question,
          answerSteps: b.answerSteps,
          yearReference: b.yearReference,
        });
        if (prompts.length >= maxPrompts) return prompts;
      }
    }
  }
  return prompts;
}

export async function listExamPatterns(
  db: D1Database,
  filters?: { boardCode?: string; classLevel?: number; subjectId?: string }
): Promise<ApiExamPattern[]> {
  const clauses: string[] = [];
  const params: unknown[] = [];
  if (filters?.boardCode) {
    clauses.push('board_code = ?');
    params.push(filters.boardCode);
  }
  if (filters?.classLevel) {
    clauses.push('class_level = ?');
    params.push(filters.classLevel);
  }
  if (filters?.subjectId) {
    clauses.push('(subject_id IS NULL OR subject_id = ?)');
    params.push(filters.subjectId);
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const sql = `SELECT * FROM exam_patterns ${where} ORDER BY class_level ASC, mode ASC`;
  const stmt = params.length ? db.prepare(sql).bind(...params) : db.prepare(sql);
  const res = await stmt.all<{
    id: string;
    title: string;
    mode: ApiExamPattern['mode'];
    class_level: ClassLevel;
    board_code: BoardCode;
    subject_id: string | null;
    question_count: number;
    duration_minutes: number;
    marks_per_mcq: number;
    negative_marking_penalty: number;
    pass_percentage: number;
    includes_short_questions_count: number;
    includes_long_questions_count: number;
    section_summary: string;
  }>();

  return res.results.map((r) => ({
    id: r.id,
    title: r.title,
    mode: r.mode,
    classLevel: r.class_level,
    boardCode: r.board_code,
    subjectId: r.subject_id,
    questionCount: Number(r.question_count),
    durationMinutes: Number(r.duration_minutes),
    marksPerMcq: Number(r.marks_per_mcq),
    negativeMarkingPenalty: Number(r.negative_marking_penalty),
    passPercentage: Number(r.pass_percentage),
    includesShortQuestionsCount: Number(r.includes_short_questions_count),
    includesLongQuestionsCount: Number(r.includes_long_questions_count),
    sectionSummary: r.section_summary,
  }));
}

export async function searchCatalog(
  db: D1Database,
  queryRaw: string,
  filters?: { boardCode?: string; classLevel?: number }
): Promise<ApiSearchResults> {
  const query = queryRaw.trim();
  if (!query) {
    return { query: '', subjects: [], chapters: [], topics: [], mcqs: [], pdfs: [] };
  }
  const qLower = query.toLowerCase();
  const likePattern = `%${qLower}%`;

  const subClauses = ['(LOWER(name) LIKE ? OR LOWER(code) LIKE ? OR LOWER(description) LIKE ?)'];
  const subParams: unknown[] = [likePattern, likePattern, likePattern];
  if (filters?.boardCode) {
    subClauses.push('board_code = ?');
    subParams.push(filters.boardCode);
  }
  if (filters?.classLevel) {
    subClauses.push('class_level = ?');
    subParams.push(filters.classLevel);
  }

  const chClauses = ['(LOWER(c.title) LIKE ? OR LOWER(c.subtitle) LIKE ? OR LOWER(c.topics_json) LIKE ?)'];
  const chParams: unknown[] = [likePattern, likePattern, likePattern];
  if (filters?.boardCode) {
    chClauses.push('s.board_code = ?');
    chParams.push(filters.boardCode);
  }
  if (filters?.classLevel) {
    chClauses.push('s.class_level = ?');
    chParams.push(filters.classLevel);
  }

  const mcqClauses = ["m.publish_status = 'published'", '(LOWER(m.question) LIKE ? OR LOWER(m.topic_title) LIKE ?)'];
  const mcqParams: unknown[] = [likePattern, likePattern];
  if (filters?.boardCode) {
    mcqClauses.push('s.board_code = ?');
    mcqParams.push(filters.boardCode);
  }
  if (filters?.classLevel) {
    mcqClauses.push('m.class_level = ?');
    mcqParams.push(filters.classLevel);
  }

  const pdfClauses = ['LOWER(title) LIKE ?'];
  const pdfParams: unknown[] = [likePattern];
  if (filters?.boardCode) {
    pdfClauses.push('board_code = ?');
    pdfParams.push(filters.boardCode);
  }
  if (filters?.classLevel) {
    pdfClauses.push('class_level = ?');
    pdfParams.push(filters.classLevel);
  }

  const [subRes, chRes, mcqRes, pdfRes] = await Promise.all([
    db
      .prepare(
        `SELECT id, class_id, class_level, board_code, code, name, icon_key, accent_hex, total_chapters, published_chapters, is_offline_pack_available, coverage_status, updated_at_iso, study_groups_json, description, textbook_pdf_id
         FROM subjects WHERE ${subClauses.join(' AND ')} ORDER BY class_level ASC, name ASC LIMIT 15`
      )
      .bind(...subParams)
      .all<{
        id: string;
        class_id: string;
        class_level: ClassLevel;
        board_code: BoardCode;
        code: string;
        name: string;
        icon_key: SubjectIconKey;
        accent_hex: string;
        total_chapters: number;
        published_chapters: number;
        is_offline_pack_available: number;
        coverage_status: ContentCoverageStatus;
        updated_at_iso: string;
        study_groups_json: string;
        description: string;
        textbook_pdf_id: string | null;
      }>(),
    db
      .prepare(
        `SELECT c.*, s.board_code, s.class_level FROM chapters c JOIN subjects s ON s.id = c.subject_id
         WHERE ${chClauses.join(' AND ')} ORDER BY c.chapter_number ASC LIMIT 25`
      )
      .bind(...chParams)
      .all<{
        id: string;
        subject_id: string;
        chapter_number: number;
        title: string;
        subtitle: string;
        topics_json: string;
        current_version: string;
        publish_status: PublishStatus;
        has_detailed_notes: number;
        has_revision_notes: number;
        mcq_count: number;
        solved_questions_count: number;
        is_locked_by_default: number;
        estimated_size_kb: number;
        updated_at_iso: string;
        board_code: BoardCode;
        class_level: ClassLevel;
      }>(),
    db
      .prepare(
        `SELECT m.* FROM mcqs m LEFT JOIN subjects s ON s.id = m.subject_id
         WHERE ${mcqClauses.join(' AND ')} ORDER BY m.created_at_iso ASC LIMIT 15`
      )
      .bind(...mcqParams)
      .all<McqDbRow>(),
    db
      .prepare(
        `SELECT * FROM pdf_assets WHERE ${pdfClauses.join(' AND ')} ORDER BY created_at_iso DESC LIMIT 15`
      )
      .bind(...pdfParams)
      .all<{
        id: string;
        title: string;
        category: ApiPdfMetadata['category'];
        class_level: ClassLevel;
        board_code: BoardCode;
        subject_id: string;
        object_key: string;
        file_size_bytes: number;
        page_count: number;
        checksum_sha256: string;
        offline_cacheable: number;
        version: string;
      }>(),
  ]);

  const subjects: ApiSubject[] = subRes.results.map((r) => ({
    id: r.id,
    classId: r.class_id,
    classLevel: r.class_level,
    boardCode: r.board_code,
    code: r.code,
    name: r.name,
    iconKey: r.icon_key,
    accentHex: r.accent_hex,
    totalChapters: Number(r.total_chapters),
    publishedChapters: Number(r.published_chapters),
    isOfflinePackAvailable: r.is_offline_pack_available === 1,
    coverageStatus: r.coverage_status,
    updatedAtIso: r.updated_at_iso,
    studyGroups: safeJsonParse<StudyGroupCode[]>(r.study_groups_json, []),
    description: r.description || '',
    textbookPdfId: r.textbook_pdf_id ?? null,
  }));

  const chapters: ApiChapter[] = [];
  const topics: Array<ApiTopic & { subjectId: string; chapterTitle: string }> = [];

  for (const r of chRes.results) {
    const parsedTopics = safeJsonParse<ApiTopic[]>(r.topics_json, []);
    if (r.title.toLowerCase().includes(qLower) || r.subtitle.toLowerCase().includes(qLower)) {
      chapters.push({
        id: r.id,
        subjectId: r.subject_id,
        chapterNumber: Number(r.chapter_number),
        title: r.title,
        subtitle: r.subtitle,
        topics: parsedTopics,
        currentVersion: r.current_version,
        publishStatus: r.publish_status,
        hasDetailedNotes: r.has_detailed_notes === 1,
        hasRevisionNotes: r.has_revision_notes === 1,
        mcqCount: Number(r.mcq_count),
        solvedQuestionsCount: Number(r.solved_questions_count),
        isLockedByDefault: r.is_locked_by_default === 1,
        estimatedSizeKb: Number(r.estimated_size_kb),
        updatedAtIso: r.updated_at_iso,
        boardCode: r.board_code,
        classLevel: r.class_level,
      });
    }
    for (const t of parsedTopics) {
      if (t.title.toLowerCase().includes(qLower) && topics.length < 20) {
        topics.push({ ...t, subjectId: r.subject_id, chapterTitle: r.title });
      }
    }
  }

  const mcqs = mcqRes.results.map(mapMcqRow);
  const pdfs: ApiPdfMetadata[] = pdfRes.results.map((r) => ({
    id: r.id,
    title: r.title,
    category: r.category,
    classLevel: r.class_level,
    boardCode: r.board_code,
    subjectId: r.subject_id,
    objectKey: r.object_key,
    fileSizeBytes: Number(r.file_size_bytes),
    pageCount: Number(r.page_count),
    checksumSha256: r.checksum_sha256,
    offlineCacheable: r.offline_cacheable === 1,
    version: r.version,
  }));

  return { query, subjects, chapters: chapters.slice(0, 15), topics, mcqs, pdfs };
}

export async function listPastPapers(
  db: D1Database,
  filters?: { classLevel?: number; boardCode?: string; isSolved?: boolean | null; limit?: number; offset?: number }
): Promise<ApiPastPaper[]> {
  const clauses: string[] = [];
  const params: unknown[] = [];
  if (filters?.classLevel) {
    clauses.push('class_level = ?');
    params.push(filters.classLevel);
  }
  if (filters?.boardCode) {
    clauses.push('board_code = ?');
    params.push(filters.boardCode);
  }
  if (typeof filters?.isSolved === 'boolean') {
    clauses.push('is_solved = ?');
    params.push(filters.isSolved ? 1 : 0);
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const limit = Math.max(1, Math.min(200, filters?.limit ?? 100));
  const offset = Math.max(0, filters?.offset ?? 0);
  const sql = `SELECT * FROM past_papers ${where} ORDER BY year DESC, subject_name ASC LIMIT ? OFFSET ?`;
  params.push(limit, offset);
  const res = await db.prepare(sql).bind(...params).all<{
    id: string;
    board_code: BoardCode;
    class_level: ClassLevel;
    subject_id: string;
    subject_name: string;
    year: number;
    session: 'Annual' | 'Supplementary';
    is_solved: number;
    pdf_metadata_id: string;
    file_size_mb: number;
    page_count: number;
    download_count: number;
  }>();

  return res.results.map((r) => ({
    id: r.id,
    boardCode: r.board_code,
    classLevel: r.class_level,
    subjectId: r.subject_id,
    subjectName: r.subject_name,
    year: Number(r.year),
    session: r.session,
    isSolved: r.is_solved === 1,
    pdfMetadataId: r.pdf_metadata_id,
    fileSizeMb: Number(r.file_size_mb),
    pageCount: Number(r.page_count),
    downloadCount: Number(r.download_count),
  }));
}

export async function listPdfMetadata(
  db: D1Database,
  filters?: {
    category?: ApiPdfMetadata['category'];
    classLevel?: number;
    boardCode?: string;
    subjectId?: string;
    limit?: number;
    offset?: number;
  }
): Promise<ApiPdfMetadata[]> {
  const clauses: string[] = [];
  const params: unknown[] = [];
  if (filters?.category) {
    clauses.push('category = ?');
    params.push(filters.category);
  }
  if (filters?.classLevel) {
    clauses.push('class_level = ?');
    params.push(filters.classLevel);
  }
  if (filters?.boardCode) {
    clauses.push('board_code = ?');
    params.push(filters.boardCode);
  }
  if (filters?.subjectId) {
    clauses.push('subject_id = ?');
    params.push(filters.subjectId);
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const limit = Math.max(1, Math.min(200, filters?.limit ?? 100));
  const offset = Math.max(0, filters?.offset ?? 0);
  const sql = `SELECT * FROM pdf_assets ${where} ORDER BY created_at_iso DESC LIMIT ? OFFSET ?`;
  params.push(limit, offset);

  const res = await db.prepare(sql).bind(...params).all<{
    id: string;
    title: string;
    category: ApiPdfMetadata['category'];
    class_level: ClassLevel;
    board_code: BoardCode;
    subject_id: string;
    object_key: string;
    file_size_bytes: number;
    page_count: number;
    checksum_sha256: string;
    offline_cacheable: number;
    version: string;
  }>();

  return res.results.map((r) => ({
    id: r.id,
    title: r.title,
    category: r.category,
    classLevel: r.class_level,
    boardCode: r.board_code,
    subjectId: r.subject_id,
    objectKey: r.object_key,
    fileSizeBytes: Number(r.file_size_bytes),
    pageCount: Number(r.page_count),
    checksumSha256: r.checksum_sha256,
    offlineCacheable: r.offline_cacheable === 1,
    version: r.version,
  }));
}

export async function getPdfById(db: D1Database, pdfId: string): Promise<ApiPdfMetadata | null> {
  const r = await db
    .prepare('SELECT * FROM pdf_assets WHERE id = ? LIMIT 1')
    .bind(pdfId)
    .first<{
      id: string;
      title: string;
      category: ApiPdfMetadata['category'];
      class_level: ClassLevel;
      board_code: BoardCode;
      subject_id: string;
      object_key: string;
      file_size_bytes: number;
      page_count: number;
      checksum_sha256: string;
      offline_cacheable: number;
      version: string;
    }>();
  if (!r) return null;
  return {
    id: r.id,
    title: r.title,
    category: r.category,
    classLevel: r.class_level,
    boardCode: r.board_code,
    subjectId: r.subject_id,
    objectKey: r.object_key,
    fileSizeBytes: Number(r.file_size_bytes),
    pageCount: Number(r.page_count),
    checksumSha256: r.checksum_sha256,
    offlineCacheable: r.offline_cacheable === 1,
    version: r.version,
  };
}

export async function getAdPlacementPolicy(db: D1Database): Promise<ApiAdPlacementConfig> {
  const row = await db
    .prepare("SELECT setting_json FROM app_settings WHERE setting_key = 'ad_placement' LIMIT 1")
    .first<{ setting_json: string }>();
  return safeJsonParse<ApiAdPlacementConfig>(row?.setting_json, {
    enabled: false,
    bannerOnHome: true,
    bannerOnPapers: true,
    suppressDuringQuizAndExam: true,
    suppressDuringNotesReading: true,
    dailyBannerCap: 4,
    providerNote: 'Disabled until configured in Admin Console.',
  });
}

export async function listFeedbackReports(
  db: D1Database,
  limit = 100,
  offset = 0
): Promise<ApiFeedbackReport[]> {
  const safeLimit = Math.max(1, Math.min(200, limit));
  const safeOffset = Math.max(0, offset);
  const res = await db
    .prepare('SELECT * FROM feedback_reports ORDER BY created_at_iso DESC LIMIT ? OFFSET ?')
    .bind(safeLimit, safeOffset)
    .all<{
      id: string;
      student_id: string | null;
      guest_device_id: string | null;
      category: ApiFeedbackReport['category'];
      subject_id: string | null;
      chapter_id: string | null;
      mcq_id: string | null;
      title: string;
      description: string;
      status: ApiFeedbackReport['status'];
      admin_resolution_note: string;
      created_at_iso: string;
      updated_at_iso: string;
    }>();
  return res.results.map((r) => ({
    id: r.id,
    studentId: r.student_id,
    guestDeviceId: r.guest_device_id ?? undefined,
    category: r.category,
    subjectId: r.subject_id,
    chapterId: r.chapter_id,
    mcqId: r.mcq_id,
    title: r.title,
    description: r.description,
    status: r.status,
    adminResolutionNote: r.admin_resolution_note,
    createdAtIso: r.created_at_iso,
    updatedAtIso: r.updated_at_iso,
  }));
}

export async function listAiSubmissions(db: D1Database): Promise<ApiAiAgentSubmission[]> {
  const res = await db
    .prepare('SELECT * FROM ai_agent_submissions ORDER BY submitted_at_iso DESC')
    .all<{
      id: string;
      agent_name: string;
      model_identifier: string;
      target_class: ClassLevel;
      board_code: BoardCode;
      subject_name: string;
      chapter_id: string | null;
      chapter_title: string;
      submission_type: ApiAiAgentSubmission['submissionType'];
      payload_json: string;
      blocks_count: number;
      schema_validation_passed: number;
      validation_warnings_json: string;
      status: ApiAiAgentSubmission['status'];
      submitted_at_iso: string;
      reviewer_notes: string;
    }>();

  return res.results.map((r) => {
    const parsedPayload = safeJsonParse<{
      blocks?: EducationalContentBlock[];
      mcqs?: ApiAiAgentSubmission['mcqs'];
    }>(r.payload_json, {});
    return {
      id: r.id,
      agentName: r.agent_name,
      modelIdentifier: r.model_identifier,
      targetClass: r.target_class,
      boardCode: r.board_code,
      subjectName: r.subject_name,
      chapterId: r.chapter_id,
      chapterTitle: r.chapter_title,
      submissionType: r.submission_type,
      blocksCount: Number(r.blocks_count),
      schemaValidationPassed: r.schema_validation_passed === 1,
      validationWarnings: safeJsonParse<string[]>(r.validation_warnings_json, []),
      status: r.status,
      submittedAtIso: r.submitted_at_iso,
      reviewerNotes: r.reviewer_notes,
      blocks: parsedPayload.blocks,
      mcqs: parsedPayload.mcqs,
    };
  });
}

export async function listAuditLogs(db: D1Database, limit = 40): Promise<ApiAdminAuditItem[]> {
  const res = await db
    .prepare(
      'SELECT id, actor_name, action, target_entity, environment_mode, timestamp_iso FROM admin_audit_log ORDER BY timestamp_iso DESC LIMIT ?'
    )
    .bind(limit)
    .all<{
      id: string;
      actor_name: string;
      action: string;
      target_entity: string;
      environment_mode: 'stage_1_mock' | 'cloudflare_stage_2';
      timestamp_iso: string;
    }>();

  return res.results.map((r) => ({
    id: r.id,
    actorName: r.actor_name,
    action: r.action,
    targetEntity: r.target_entity,
    environmentMode: r.environment_mode,
    timestampIso: r.timestamp_iso,
  }));
}

export async function appendAuditLog(
  db: D1Database,
  params: {
    actorAdminId?: string | null;
    actorName: string;
    action: string;
    targetEntity: string;
    details?: Record<string, unknown>;
  }
): Promise<void> {
  const id = `aud_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
  await db
    .prepare(
      `INSERT INTO admin_audit_log (id, actor_admin_id, actor_name, action, target_entity, environment_mode, details_json, timestamp_iso)
       VALUES (?, ?, ?, ?, ?, 'cloudflare_stage_2', ?, ?)`
    )
    .bind(
      id,
      params.actorAdminId ?? null,
      params.actorName,
      params.action,
      params.targetEntity,
      JSON.stringify(params.details ?? {}),
      new Date().toISOString()
    )
    .run();
}

export async function getStudentProgressRecord(
  db: D1Database,
  studentId: string
): Promise<ApiStudentProgress | null> {
  const st = await db
    .prepare('SELECT * FROM students WHERE id = ? LIMIT 1')
    .bind(studentId)
    .first<{
      id: string;
      guest_device_id: string;
      google_uid: string | null;
      selected_class: ClassLevel;
      selected_board: BoardCode;
      selected_sub_board: KpkSubBoardCode | null;
      study_group: StudyGroupCode | null;
      language: LanguageCode;
      study_hours_total: number;
      daily_goal_percent: number;
      daily_target_minutes: number;
      exam_countdown_date_iso: string | null;
      diagnostic_completed: number;
      xp_total: number;
      current_streak_days: number;
      updated_at_iso: string;
    }>();
  if (!st) return null;

  const progressRows = await db
    .prepare('SELECT * FROM student_chapter_progress WHERE student_id = ?')
    .bind(studentId)
    .all<{
      chapter_id: string;
      subject_id: string;
      completion_percent: number;
      last_block_index: number;
      is_completed: number;
      last_accessed_at_iso: string;
    }>();

  const chapterProgress: ApiChapterProgressItem[] = progressRows.results.map((r) => ({
    chapterId: r.chapter_id,
    subjectId: r.subject_id,
    completionPercent: Number(r.completion_percent),
    lastBlockIndex: Number(r.last_block_index),
    isCompleted: r.is_completed === 1,
    lastAccessedAtIso: r.last_accessed_at_iso,
  }));

  const completedCount = chapterProgress.filter((c) => c.isCompleted || c.completionPercent >= 100).length;
  const avgPct =
    chapterProgress.length === 0
      ? 0
      : Math.round(
          chapterProgress.reduce((acc, item) => acc + item.completionPercent, 0) /
            chapterProgress.length
        );

  return {
    localProfileId: st.id,
    googleUid: st.google_uid,
    isGuestMode: !st.google_uid,
    selectedClass: st.selected_class,
    selectedBoard: st.selected_board,
    selectedSubBoard: st.selected_sub_board ?? 'PESHAWAR',
    studyGroup: st.study_group ?? 'SCIENCE_BIOLOGY',
    language: st.language ?? 'en',
    studyHoursTotal: Number(st.study_hours_total),
    chaptersCompletedCount: completedCount,
    overallProgressPercent: avgPct,
    dailyGoalPercent: Number(st.daily_goal_percent),
    dailyTargetMinutes: Number(st.daily_target_minutes ?? 60),
    examCountdownDateIso: st.exam_countdown_date_iso ?? null,
    diagnosticCompleted: st.diagnostic_completed === 1,
    xpTotal: Number(st.xp_total ?? 0),
    currentStreakDays: Number(st.current_streak_days ?? 0),
    chapterProgress,
    lastSyncedAtIso: st.updated_at_iso,
  };
}

export function safeJsonParse<T>(raw: string | null | undefined, fallback: T): T {
  if (!raw) return fallback;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

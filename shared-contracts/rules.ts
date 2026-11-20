import type {
  AgentIngestSubmissionRequest,
  ApiAcademicClass,
  ApiAdPlacementConfig,
  ApiAiAgentSubmission,
  ApiChapter,
  ApiChapterProgressItem,
  ApiKpkSubBoard,
  ApiMcqItem,
  ApiQuizResultResponse,
  ApiQuizSubmissionRequest,
  ApiStudyGroupOption,
  ApiSubject,
  ApiWeaknessItem,
  BoardCode,
  ClassLevel,
  EducationalContentBlock,
  PublishStatus,
  StudyGroupCode,
} from './types';

export const KPK_SUB_BOARDS: ApiKpkSubBoard[] = [
  { code: 'PESHAWAR', name: 'BISE Peshawar', headquarters: 'Peshawar' },
  { code: 'MARDAN', name: 'BISE Mardan', headquarters: 'Mardan' },
  { code: 'SWAT', name: 'BISE Swat', headquarters: 'Saidu Sharif, Swat' },
  { code: 'ABBOTTABAD', name: 'BISE Abbottabad', headquarters: 'Abbottabad' },
  { code: 'KOHAT', name: 'BISE Kohat', headquarters: 'Kohat' },
  { code: 'BANNU', name: 'BISE Bannu', headquarters: 'Bannu' },
  { code: 'DI_KHAN', name: 'BISE D.I. Khan', headquarters: 'Dera Ismail Khan' },
  { code: 'MALAKAND', name: 'BISE Malakand', headquarters: 'Chakdara, Malakand' },
];

export const STUDY_GROUPS_CATALOG: ApiStudyGroupOption[] = [
  {
    code: 'SCIENCE_BIOLOGY',
    label: 'Science (Biology Group)',
    applicableClasses: [9, 10],
    description: 'Physics, Chemistry, Biology, Mathematics, English, Urdu, Islamiat & Pak Studies',
  },
  {
    code: 'SCIENCE_CS',
    label: 'Science (Computer Group)',
    applicableClasses: [9, 10],
    description: 'Physics, Chemistry, Computer Science, Mathematics, English, Urdu',
  },
  {
    code: 'ARTS_HUMANITIES',
    label: 'Arts & Humanities',
    applicableClasses: [9, 10],
    description: 'General Science, General Mathematics, English, Urdu, Islamiat & Electives',
  },
  {
    code: 'PRE_MEDICAL',
    label: 'FSc Pre-Medical',
    applicableClasses: [11, 12],
    description: 'Biology, Physics, Chemistry, English, Urdu, Islamiat / Pak Studies',
  },
  {
    code: 'PRE_ENGINEERING',
    label: 'FSc Pre-Engineering',
    applicableClasses: [11, 12],
    description: 'Mathematics, Physics, Chemistry, English, Urdu, Islamiat / Pak Studies',
  },
  {
    code: 'ICS',
    label: 'ICS (Computer Science)',
    applicableClasses: [11, 12],
    description: 'Computer Science, Mathematics, Physics / Statistics, English, Urdu',
  },
  {
    code: 'FA',
    label: 'FA (Humanities)',
    applicableClasses: [11, 12],
    description: 'Civics, Economics, Islamic Studies, English, Urdu & Humanities',
  },
  {
    code: 'ICOM',
    label: 'I.Com (Commerce)',
    applicableClasses: [11, 12],
    description: 'Accounting, Commerce, Economics, Business Math, English & Urdu',
  },
];

export function getStudyGroupsForClass(classLevel: ClassLevel): ApiStudyGroupOption[] {
  return STUDY_GROUPS_CATALOG.filter((g) => g.applicableClasses.includes(classLevel));
}

export function subjectMatchesStudyGroup(
  subject: ApiSubject,
  studyGroup?: StudyGroupCode | null
): boolean {
  if (!studyGroup || !subject.studyGroups || subject.studyGroups.length === 0) {
    return true;
  }
  return subject.studyGroups.includes(studyGroup);
}

export function gradeQuizSubmission(
  mcqs: ApiMcqItem[],
  submission: ApiQuizSubmissionRequest
): ApiQuizResultResponse {
  const mcqMap = new Map(mcqs.map((m) => [m.id, m]));
  let correctCount = 0;
  let incorrectCount = 0;
  let unansweredCount = 0;

  const topicBuckets = new Map<string, { correct: number; total: number }>();

  const perQuestionFeedback = submission.answers.map((ans) => {
    const item = mcqMap.get(ans.mcqId);
    const topicName = item?.topicTitle?.trim() || 'General';
    const bucket = topicBuckets.get(topicName) ?? { correct: 0, total: 0 };
    bucket.total += 1;

    if (!item || !item.correctOptionId) {
      unansweredCount += 1;
      topicBuckets.set(topicName, bucket);
      return {
        mcqId: ans.mcqId,
        isCorrect: false,
        correctOptionId: 'A' as const,
        explanation: item?.explanation || 'Question metadata unavailable.',
      };
    }

    if (!ans.selectedOptionId) {
      unansweredCount += 1;
      topicBuckets.set(topicName, bucket);
      return {
        mcqId: ans.mcqId,
        isCorrect: false,
        correctOptionId: item.correctOptionId,
        explanation: item.explanation || '',
      };
    }

    const isCorrect = ans.selectedOptionId === item.correctOptionId;
    if (isCorrect) {
      correctCount += 1;
      bucket.correct += 1;
    } else {
      incorrectCount += 1;
    }
    topicBuckets.set(topicName, bucket);

    return {
      mcqId: ans.mcqId,
      isCorrect,
      correctOptionId: item.correctOptionId,
      explanation: item.explanation || '',
    };
  });

  const totalQuestions = submission.answers.length;
  const marksPerMcq = Math.max(0.5, Number(submission.marksPerMcq ?? 1));
  const penalty = Math.max(0, Number(submission.negativeMarkingPenalty ?? 0));
  const passPercentage = Math.max(1, Math.min(100, Number(submission.passPercentage ?? 40)));

  const totalMarks = Number((totalQuestions * marksPerMcq).toFixed(2));
  const rawMarks = correctCount * marksPerMcq - incorrectCount * penalty;
  const marksObtained = Number(Math.max(0, rawMarks).toFixed(2));

  const percentage =
    totalQuestions === 0
      ? 0
      : penalty > 0 && totalMarks > 0
        ? Math.round((marksObtained / totalMarks) * 100)
        : Math.round((correctCount / totalQuestions) * 100);

  const passed = totalQuestions > 0 && percentage >= passPercentage;
  const xpEarned = correctCount * 10 + (passed ? 20 : 5);

  const topicBreakdown = Array.from(topicBuckets.entries()).map(([topicTitle, stats]) => ({
    topicTitle,
    correct: stats.correct,
    total: stats.total,
    accuracyPercent: stats.total === 0 ? 0 : Math.round((stats.correct / stats.total) * 100),
  }));

  return {
    submissionId: `sub_${submission.sessionId}`,
    totalQuestions,
    correctCount,
    incorrectCount,
    unansweredCount,
    percentage,
    elapsedSeconds: Math.max(0, submission.elapsedSeconds),
    perQuestionFeedback,
    marksObtained,
    totalMarks,
    passed,
    passPercentage,
    xpEarned,
    topicBreakdown,
  };
}

export function validateContentBlock(block: EducationalContentBlock): string[] {
  const errors: string[] = [];

  if (!block.id || block.id.trim().length === 0) {
    errors.push('Block id is required.');
  }

  switch (block.type) {
    case 'heading':
    case 'subheading':
    case 'paragraph':
      if (!block.text || !block.text.trim()) {
        errors.push(`Block ${block.id} (${block.type}) cannot have empty text.`);
      }
      break;
    case 'math_expression':
      if ((!block.latex || !block.latex.trim()) && (!block.plainNotation || !block.plainNotation.trim())) {
        errors.push(`Math block ${block.id} requires LaTeX or plain notation.`);
      }
      break;
    case 'table':
      if (!Array.isArray(block.headers) || block.headers.length === 0) {
        errors.push(`Table block ${block.id} must define at least one column header.`);
      } else if (!Array.isArray(block.rows) || block.rows.some((r) => r.length !== block.headers.length)) {
        errors.push(`Table block ${block.id} has row column mismatch.`);
      }
      break;
    case 'list':
      if (!Array.isArray(block.items) || block.items.length === 0) {
        errors.push(`List block ${block.id} must contain at least one item.`);
      }
      break;
    case 'definition':
      if (!block.term?.trim() || !block.definition?.trim()) {
        errors.push(`Definition block ${block.id} requires term and definition.`);
      }
      break;
    case 'example':
      if (!block.problemStatement?.trim() || !Array.isArray(block.steps) || block.steps.length === 0) {
        errors.push(`Example block ${block.id} requires a problem statement and steps.`);
      }
      break;
    case 'diagram':
      if (!block.assetKey?.trim() || !block.altText?.trim()) {
        errors.push(`Diagram block ${block.id} requires an asset key and accessibility altText.`);
      }
      break;
    case 'exam_point':
      if (!Array.isArray(block.points) || block.points.length === 0) {
        errors.push(`Exam point block ${block.id} must contain at least one bullet point.`);
      }
      break;
    case 'mcq':
      if (!block.question?.trim() || !Array.isArray(block.options) || block.options.length !== 4) {
        errors.push(`MCQ block ${block.id} must have a question and 4 options.`);
      }
      break;
    case 'solved_question':
      if (!block.question?.trim() || !Array.isArray(block.answerSteps) || block.answerSteps.length === 0 || block.marks <= 0) {
        errors.push(`Solved question block ${block.id} requires question, answerSteps, and positive marks.`);
      }
      break;
    default:
      errors.push(`Unsupported block type: ${String((block as { type?: unknown }).type)}`);
  }

  return errors;
}

export function validateAgentIngestPayload(payload: AgentIngestSubmissionRequest): {
  valid: boolean;
  errors: string[];
  warnings: string[];
  blocksCount: number;
} {
  const errors: string[] = [];
  const warnings: string[] = [];

  if (!payload.agentName?.trim()) errors.push('agentName is required.');
  if (!payload.modelIdentifier?.trim()) errors.push('modelIdentifier is required.');
  if (![9, 10, 11, 12].includes(payload.targetClass)) {
    errors.push('targetClass must be 9, 10, 11, or 12.');
  }
  if (!['KPK', 'FEDERAL', 'PUNJAB', 'SINDH', 'BALOCHISTAN'].includes(payload.boardCode)) {
    errors.push('Invalid boardCode.');
  }
  if (!payload.subjectId?.trim()) errors.push('subjectId is required.');
  if (!payload.chapterTitle?.trim()) errors.push('chapterTitle is required.');

  const blocks = Array.isArray(payload.blocks) ? payload.blocks : [];
  const mcqs = Array.isArray(payload.mcqs) ? payload.mcqs : [];

  if (blocks.length === 0 && mcqs.length === 0 && !payload.pdfMetadata) {
    errors.push('Submission must include at least one block, MCQ item, or pdfMetadata.');
  }

  for (const block of blocks) {
    const blockErrs = validateContentBlock(block);
    errors.push(...blockErrs);
    if (block.type === 'diagram' && block.assetKey.includes('placeholder')) {
      warnings.push(`Diagram block ${block.id} references placeholder assetKey (${block.assetKey}).`);
    }
  }

  for (let i = 0; i < mcqs.length; i++) {
    const m = mcqs[i];
    if (!m.question?.trim()) errors.push(`MCQ[${i}] missing question.`);
    if (!Array.isArray(m.options) || m.options.length !== 4) {
      errors.push(`MCQ[${i}] must include exactly 4 options (A-D).`);
    }
    if (!['A', 'B', 'C', 'D'].includes(m.correctOptionId)) {
      errors.push(`MCQ[${i}] invalid correctOptionId.`);
    }
    if (!m.explanation?.trim()) {
      warnings.push(`MCQ[${i}] has empty explanation.`);
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    warnings,
    blocksCount: blocks.length + mcqs.length,
  };
}

function isLocalDevHost(hostname: string): boolean {
  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '10.0.2.2') {
    return true;
  }
  if (/^192\.168\.\d{1,3}\.\d{1,3}$/.test(hostname)) return true;
  if (/^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(hostname)) return true;
  return false;
}

export function validateApiBaseUrl(url: string): { valid: boolean; reason?: string } {
  const trimmed = url.trim();
  if (!trimmed) {
    return { valid: false, reason: 'API base URL cannot be empty.' };
  }
  try {
    const parsed = new URL(trimmed);
    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
      return { valid: false, reason: 'URL must use https:// or http://.' };
    }
    if (parsed.protocol === 'https:' || isLocalDevHost(parsed.hostname)) {
      return { valid: true };
    }
    return { valid: false, reason: 'Non-localhost endpoints must use HTTPS.' };
  } catch {
    return { valid: false, reason: 'Enter a valid URL (e.g., https://imtihan-pro-api.workers.dev/v1).' };
  }
}

export function canTransitionSubmissionStatus(
  from: ApiAiAgentSubmission['status'],
  to: ApiAiAgentSubmission['status']
): boolean {
  const allowed: Record<ApiAiAgentSubmission['status'], ApiAiAgentSubmission['status'][]> = {
    queued: ['in_review', 'approved', 'revision_requested', 'rejected'],
    in_review: ['approved', 'revision_requested', 'rejected'],
    revision_requested: ['in_review', 'approved', 'rejected'],
    approved: ['in_review'],
    rejected: ['in_review'],
  };
  return allowed[from].includes(to);
}

export function canTransitionPublishStatus(from: PublishStatus, to: PublishStatus): boolean {
  const allowed: Record<PublishStatus, PublishStatus[]> = {
    draft: ['in_review'],
    in_review: ['approved', 'rejected', 'draft'],
    approved: ['published', 'draft'],
    published: ['draft'],
    rejected: ['draft', 'in_review'],
  };
  return allowed[from].includes(to);
}

export function bumpSemanticVersion(currentVersion: string, bump: 'patch' | 'minor' = 'minor'): string {
  const clean = currentVersion.replace(/^v/i, '').replace(/-draft$/i, '');
  const parts = clean.split('.').map((n) => Number.parseInt(n, 10));
  const major = Number.isFinite(parts[0]) ? parts[0] : 1;
  const minor = Number.isFinite(parts[1]) ? parts[1] : 0;
  const patch = Number.isFinite(parts[2]) ? parts[2] : 0;

  if (bump === 'minor') {
    return `v${major}.${minor + 1}.0`;
  }
  return `v${major}.${minor}.${patch + 1}`;
}

export function mergeChapterProgress(
  existing: ApiChapterProgressItem[],
  incoming: ApiChapterProgressItem[]
): ApiChapterProgressItem[] {
  const byChapter = new Map<string, ApiChapterProgressItem>();

  for (const item of existing) {
    byChapter.set(item.chapterId, { ...item });
  }

  for (const inc of incoming) {
    const prev = byChapter.get(inc.chapterId);
    if (!prev) {
      byChapter.set(inc.chapterId, {
        ...inc,
        completionPercent: Math.max(0, Math.min(100, inc.completionPercent)),
        lastBlockIndex: Math.max(0, inc.lastBlockIndex),
      });
      continue;
    }

    const prevTime = Date.parse(prev.lastAccessedAtIso || '1970-01-01T00:00:00Z');
    const incTime = Date.parse(inc.lastAccessedAtIso || '1970-01-01T00:00:00Z');
    const incIsNewer = Number.isFinite(incTime) && (!Number.isFinite(prevTime) || incTime >= prevTime);

    const bestCompletion = Math.max(
      prev.completionPercent,
      Math.max(0, Math.min(100, inc.completionPercent))
    );

    byChapter.set(inc.chapterId, {
      chapterId: inc.chapterId,
      subjectId: inc.subjectId ?? prev.subjectId,
      completionPercent: bestCompletion,
      lastBlockIndex: incIsNewer ? Math.max(0, inc.lastBlockIndex) : prev.lastBlockIndex,
      isCompleted: Boolean(prev.isCompleted || inc.isCompleted || bestCompletion >= 100),
      lastAccessedAtIso: incIsNewer ? inc.lastAccessedAtIso : prev.lastAccessedAtIso,
    });
  }

  return Array.from(byChapter.values());
}

export function summarizeClassCoverage(
  classes: ApiAcademicClass[],
  board: BoardCode
): Record<ClassLevel, { label: string; coverageStatus: string; publishedChapters: number }> {
  const result = {} as Record<
    ClassLevel,
    { label: string; coverageStatus: string; publishedChapters: number }
  >;
  const levels: ClassLevel[] = [9, 10, 11, 12];
  for (const level of levels) {
    const match = classes.find((c) => c.boardCode === board && c.level === level);
    result[level] = match
      ? {
          label: match.label,
          coverageStatus: match.coverageStatus,
          publishedChapters: match.publishedChaptersCount,
        }
      : {
          label: `Class ${level}`,
          coverageStatus: 'planned',
          publishedChapters: 0,
        };
  }
  return result;
}

export function calculateSm2NextReview(
  current: { intervalDays: number; easeFactor: number; repetitions: number },
  quality: 0 | 1 | 2 | 3 | 4 | 5,
  nowIso: string = new Date().toISOString()
): {
  intervalDays: number;
  easeFactor: number;
  repetitions: number;
  nextReviewAtIso: string;
} {
  let repetitions = Math.max(0, current.repetitions);
  let easeFactor = Math.max(1.3, current.easeFactor || 2.5);
  let intervalDays = Math.max(1, current.intervalDays || 1);

  if (quality < 3) {
    repetitions = 0;
    intervalDays = 1;
  } else {
    if (repetitions === 0) {
      intervalDays = 1;
    } else if (repetitions === 1) {
      intervalDays = 6;
    } else {
      intervalDays = Math.max(1, Math.round(intervalDays * easeFactor));
    }
    repetitions += 1;
  }

  easeFactor = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
  if (easeFactor < 1.3) easeFactor = 1.3;
  easeFactor = Number(easeFactor.toFixed(2));

  const baseMs = Date.parse(nowIso) || Date.now();
  const nextReviewAtIso = new Date(baseMs + intervalDays * 86_400_000).toISOString();

  return {
    intervalDays,
    easeFactor,
    repetitions,
    nextReviewAtIso,
  };
}

export function computeHonestExamReadiness(params: {
  quizAttemptsCount: number;
  totalQuestionsAnswered: number;
  overallAccuracyPercent: number | null;
  completedChaptersCount: number;
  totalPublishedChaptersCount: number;
  activeMistakesCount: number;
}): {
  readinessPercent: number | null;
  readinessStatus: 'insufficient_data' | 'building' | 'on_track' | 'exam_ready';
  readinessMessage: string;
} {
  if (params.quizAttemptsCount < 2 || params.totalQuestionsAnswered < 5 || params.overallAccuracyPercent === null) {
    return {
      readinessPercent: null,
      readinessStatus: 'insufficient_data',
      readinessMessage:
        'Complete at least 2 practice tests (5+ questions) so Imtihan Pro can calculate an honest readiness estimate.',
    };
  }

  const chapterRatio =
    params.totalPublishedChaptersCount > 0
      ? Math.min(1, params.completedChaptersCount / params.totalPublishedChaptersCount)
      : 0;
  const accuracyScore = Math.max(0, Math.min(100, params.overallAccuracyPercent));
  const mistakePenalty = Math.min(15, params.activeMistakesCount * 2);

  const raw = Math.round(accuracyScore * 0.7 + chapterRatio * 100 * 0.3 - mistakePenalty);
  const readinessPercent = Math.max(0, Math.min(100, raw));

  if (readinessPercent >= 80) {
    return {
      readinessPercent,
      readinessStatus: 'exam_ready',
      readinessMessage: 'Strong syllabus coverage and high recent test accuracy across attempted chapters.',
    };
  }
  if (readinessPercent >= 55) {
    return {
      readinessPercent,
      readinessStatus: 'on_track',
      readinessMessage: 'Steady progress. Clear remaining Mistake Master items and revise weak chapters to reach 80%+.',
    };
  }
  return {
    readinessPercent,
    readinessStatus: 'building',
    readinessMessage: 'Foundation in progress. Focus on detailed chapter notes and spaced-repetition flashcards.',
  };
}

export function detectWeakChapters(params: {
  chapters: ApiChapter[];
  subjectsById: Map<string, ApiSubject>;
  progressByChapterId: Map<string, ApiChapterProgressItem>;
  quizStatsByChapterId: Map<string, { attemptsCount: number; averagePercent: number }>;
  activeMistakesByChapterId: Map<string, number>;
}): ApiWeaknessItem[] {
  const items: ApiWeaknessItem[] = [];

  for (const ch of params.chapters) {
    if (ch.publishStatus !== 'published') continue;
    const subj = params.subjectsById.get(ch.subjectId);
    const prog = params.progressByChapterId.get(ch.id);
    const quizStat = params.quizStatsByChapterId.get(ch.id);
    const mistakes = params.activeMistakesByChapterId.get(ch.id) ?? 0;
    const completion = prog?.completionPercent ?? 0;

    if (quizStat && quizStat.attemptsCount > 0 && quizStat.averagePercent < 65) {
      items.push({
        chapterId: ch.id,
        chapterTitle: ch.title,
        subjectId: ch.subjectId,
        subjectName: subj?.name ?? 'Subject',
        accuracyPercent: Math.round(quizStat.averagePercent),
        attemptsCount: quizStat.attemptsCount,
        completionPercent: completion,
        reason: `Quiz accuracy is ${Math.round(quizStat.averagePercent)}%${mistakes > 0 ? ` with ${mistakes} unresolved mistake(s)` : ''}.`,
        priority: quizStat.averagePercent < 50 ? 'high' : 'medium',
      });
    } else if (mistakes >= 2) {
      items.push({
        chapterId: ch.id,
        chapterTitle: ch.title,
        subjectId: ch.subjectId,
        subjectName: subj?.name ?? 'Subject',
        accuracyPercent: quizStat ? Math.round(quizStat.averagePercent) : null,
        attemptsCount: quizStat?.attemptsCount ?? 0,
        completionPercent: completion,
        reason: `${mistakes} missed MCQs waiting in Mistake Master.`,
        priority: 'medium',
      });
    } else if ((!quizStat || quizStat.attemptsCount === 0) && completion < 50) {
      items.push({
        chapterId: ch.id,
        chapterTitle: ch.title,
        subjectId: ch.subjectId,
        subjectName: subj?.name ?? 'Subject',
        accuracyPercent: null,
        attemptsCount: 0,
        completionPercent: completion,
        reason: completion === 0 ? 'Not started yet.' : `Notes ${completion}% read; no test attempted yet.`,
        priority: 'watch',
      });
    }
  }

  const order = { high: 0, medium: 1, watch: 2 };
  return items.sort((a, b) => order[a.priority] - order[b.priority]).slice(0, 8);
}

export function computeStudyStreak(activityDatesIso: string[], todayIso: string): {
  currentStreakDays: number;
  longestStreakDays: number;
} {
  const dayKeys = Array.from(
    new Set(
      activityDatesIso
        .map((d) => d.slice(0, 10))
        .filter((d) => /^\d{4}-\d{2}-\d{2}$/.test(d))
    )
  ).sort();

  if (dayKeys.length === 0) {
    return { currentStreakDays: 0, longestStreakDays: 0 };
  }

  const toDayNum = (ymd: string) => Math.floor(Date.parse(`${ymd}T00:00:00Z`) / 86_400_000);
  const nums = dayKeys.map(toDayNum);
  let longest = 1;
  let run = 1;
  for (let i = 1; i < nums.length; i++) {
    if (nums[i] === nums[i - 1] + 1) {
      run += 1;
      if (run > longest) longest = run;
    } else {
      run = 1;
    }
  }

  const todayNum = toDayNum(todayIso.slice(0, 10));
  const lastNum = nums[nums.length - 1];
  if (todayNum - lastNum > 1) {
    return { currentStreakDays: 0, longestStreakDays: longest };
  }

  let current = 1;
  for (let i = nums.length - 1; i > 0; i--) {
    if (nums[i] === nums[i - 1] + 1) {
      current += 1;
    } else {
      break;
    }
  }

  return { currentStreakDays: current, longestStreakDays: longest };
}

export function anonymizeLeaderboardHandle(
  displayName: string | null | undefined,
  guestDeviceId: string
): string {
  const cleanFirst = (displayName || '')
    .replace(/@.*/, '')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .trim()
    .split(/\s+/)[0];
  const prefix = cleanFirst && cleanFirst.toLowerCase() !== 'guest'
    ? cleanFirst.slice(0, 10)
    : 'Scholar';
  let hash = 0;
  for (let i = 0; i < guestDeviceId.length; i++) {
    hash = (hash * 31 + guestDeviceId.charCodeAt(i)) >>> 0;
  }
  const tag = hash.toString(16).toUpperCase().padStart(4, '0').slice(-4);
  return `${prefix}-${tag}`;
}

export function shouldDisplayAd(
  config: ApiAdPlacementConfig,
  surface: 'home' | 'papers' | 'quiz' | 'exam' | 'notes_reader' | 'pdf_reader',
  bannersShownToday: number
): boolean {
  if (!config.enabled) return false;
  if (bannersShownToday >= config.dailyBannerCap) return false;
  if (surface === 'quiz' || surface === 'exam') {
    return !config.suppressDuringQuizAndExam ? false : false; // Never show during active testing
  }
  if (surface === 'notes_reader' || surface === 'pdf_reader') {
    return !config.suppressDuringNotesReading ? false : false; // Never interrupt active reading
  }
  if (surface === 'home') return config.bannerOnHome;
  if (surface === 'papers') return config.bannerOnPapers;
  return false;
}

export function computeDaysUntilExam(
  examDateIso: string | null | undefined,
  todayIso: string = new Date().toISOString()
): number | null {
  if (!examDateIso) return null;
  const targetDay = examDateIso.trim().slice(0, 10);
  const currentDay = todayIso.trim().slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(targetDay) || !/^\d{4}-\d{2}-\d{2}$/.test(currentDay)) {
    return null;
  }
  const targetMs = Date.parse(`${targetDay}T00:00:00Z`);
  const currentMs = Date.parse(`${currentDay}T00:00:00Z`);
  if (!Number.isFinite(targetMs) || !Number.isFinite(currentMs)) return null;
  return Math.max(0, Math.round((targetMs - currentMs) / 86_400_000));
}

export function computeFocusSessionXp(durationMinutes: number, completed: boolean): number {
  const safeMins = Math.max(1, Math.min(180, Math.round(durationMinutes)));
  if (!completed) return Math.min(10, Math.floor(safeMins / 5));
  if (safeMins >= 50) return 40;
  if (safeMins >= 25) return 20;
  if (safeMins >= 15) return 12;
  return Math.max(5, Math.floor(safeMins / 2));
}

export function calculateBiseWeightageSummary(chapters: ApiChapter[]): {
  totalMarksMapped: number;
  chaptersWithWeightage: number;
} {
  let totalMarksMapped = 0;
  let chaptersWithWeightage = 0;
  for (const ch of chapters) {
    if (typeof ch.boardWeightageMarks === 'number' && ch.boardWeightageMarks > 0) {
      totalMarksMapped += ch.boardWeightageMarks;
      chaptersWithWeightage += 1;
    }
  }
  return { totalMarksMapped, chaptersWithWeightage };
}



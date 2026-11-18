import test from 'node:test';
import assert from 'node:assert/strict';
import {
  KPK_SUB_BOARDS,
  anonymizeLeaderboardHandle,
  calculateBiseWeightageSummary,
  calculateSm2NextReview,
  canTransitionPublishStatus,
  computeDaysUntilExam,
  computeFocusSessionXp,
  computeHonestExamReadiness,
  computeStudyStreak,
  detectWeakChapters,
  getStudyGroupsForClass,
  gradeQuizSubmission,
  shouldDisplayAd,
  subjectMatchesStudyGroup,
} from '../shared-contracts/rules';
import type { ApiAdPlacementConfig, ApiChapter, ApiMcqItem, ApiSubject } from '../shared-contracts/types';

test('KPK BISE sub-boards and study groups catalog cover Classes 9-12', () => {
  assert.equal(KPK_SUB_BOARDS.length, 8);
  assert.ok(KPK_SUB_BOARDS.some((b) => b.code === 'PESHAWAR'));
  assert.ok(KPK_SUB_BOARDS.some((b) => b.code === 'MALAKAND'));

  const g9 = getStudyGroupsForClass(9).map((g) => g.code);
  assert.deepEqual(g9, ['SCIENCE_BIOLOGY', 'SCIENCE_CS', 'ARTS_HUMANITIES']);

  const g11 = getStudyGroupsForClass(11).map((g) => g.code);
  assert.deepEqual(g11, ['PRE_MEDICAL', 'PRE_ENGINEERING', 'ICS', 'FA', 'ICOM']);
});

test('subjectMatchesStudyGroup filters subjects cleanly', () => {
  const bio: ApiSubject = {
    id: 'sub_bio',
    classId: 'cls_9',
    classLevel: 9,
    boardCode: 'KPK',
    code: 'BIO-9',
    name: 'Biology 9',
    iconKey: 'biology',
    accentHex: '#22C55E',
    totalChapters: 9,
    publishedChapters: 1,
    isOfflinePackAvailable: true,
    coverageStatus: 'active_seed',
    updatedAtIso: '2026-01-01T00:00:00Z',
    studyGroups: ['SCIENCE_BIOLOGY'],
  };
  assert.equal(subjectMatchesStudyGroup(bio, 'SCIENCE_BIOLOGY'), true);
  assert.equal(subjectMatchesStudyGroup(bio, 'SCIENCE_CS'), false);
  assert.equal(subjectMatchesStudyGroup(bio, null), true);
});

test('gradeQuizSubmission computes negative marking, pass threshold, XP, and topic breakdown', () => {
  const mcqs: ApiMcqItem[] = [
    {
      id: 'm1',
      chapterId: 'ch1',
      subjectId: 's1',
      classLevel: 9,
      question: 'Q1',
      options: [
        { id: 'A', text: '1' },
        { id: 'B', text: '2' },
        { id: 'C', text: '3' },
        { id: 'D', text: '4' },
      ],
      correctOptionId: 'A',
      explanation: 'E1',
      difficulty: 'easy',
      topicTitle: 'Kinematics',
    },
    {
      id: 'm2',
      chapterId: 'ch1',
      subjectId: 's1',
      classLevel: 9,
      question: 'Q2',
      options: [
        { id: 'A', text: '1' },
        { id: 'B', text: '2' },
        { id: 'C', text: '3' },
        { id: 'D', text: '4' },
      ],
      correctOptionId: 'B',
      explanation: 'E2',
      difficulty: 'medium',
      topicTitle: 'Kinematics',
    },
    {
      id: 'm3',
      chapterId: 'ch1',
      subjectId: 's1',
      classLevel: 9,
      question: 'Q3',
      options: [
        { id: 'A', text: '1' },
        { id: 'B', text: '2' },
        { id: 'C', text: '3' },
        { id: 'D', text: '4' },
      ],
      correctOptionId: 'C',
      explanation: 'E3',
      difficulty: 'hard',
      topicTitle: 'Vectors',
    },
  ];

  const res = gradeQuizSubmission(mcqs, {
    sessionId: 'sess_1',
    studentId: null,
    guestDeviceId: 'dev_1',
    subjectId: 's1',
    chapterId: 'ch1',
    marksPerMcq: 2,
    negativeMarkingPenalty: 0.5,
    passPercentage: 60,
    elapsedSeconds: 95,
    submittedAtIso: '2026-04-01T10:00:00Z',
    answers: [
      { mcqId: 'm1', selectedOptionId: 'A', markedForReview: false },
      { mcqId: 'm2', selectedOptionId: 'A', markedForReview: true },
      { mcqId: 'm3', selectedOptionId: 'C', markedForReview: false },
    ],
  });

  // 2 correct * 2 = 4.0, minus 1 wrong * 0.5 = 3.5 out of 6.0 -> 58% -> failed 60% threshold
  assert.equal(res.correctCount, 2);
  assert.equal(res.incorrectCount, 1);
  assert.equal(res.marksObtained, 3.5);
  assert.equal(res.totalMarks, 6);
  assert.equal(res.percentage, 58);
  assert.equal(res.passed, false);
  assert.equal(res.xpEarned, 25);
  assert.equal(res.topicBreakdown?.length, 2);
});

test('calculateSm2NextReview spaces intervals accurately and resets on recall failure', () => {
  const firstPass = calculateSm2NextReview({ intervalDays: 1, easeFactor: 2.5, repetitions: 0 }, 5, '2026-04-01T00:00:00Z');
  assert.equal(firstPass.repetitions, 1);
  assert.equal(firstPass.intervalDays, 1);

  const secondPass = calculateSm2NextReview(firstPass, 4, '2026-04-02T00:00:00Z');
  assert.equal(secondPass.repetitions, 2);
  assert.equal(secondPass.intervalDays, 6);

  const thirdPass = calculateSm2NextReview(secondPass, 5, '2026-04-08T00:00:00Z');
  assert.equal(thirdPass.repetitions, 3);
  assert.ok(thirdPass.intervalDays >= 15);

  const lapsed = calculateSm2NextReview(thirdPass, 1, '2026-04-24T00:00:00Z');
  assert.equal(lapsed.repetitions, 0);
  assert.equal(lapsed.intervalDays, 1);
});

test('computeHonestExamReadiness returns null until sufficient real activity exists', () => {
  const insufficient = computeHonestExamReadiness({
    quizAttemptsCount: 1,
    totalQuestionsAnswered: 4,
    overallAccuracyPercent: 100,
    completedChaptersCount: 2,
    totalPublishedChaptersCount: 10,
    activeMistakesCount: 0,
  });
  assert.equal(insufficient.readinessPercent, null);
  assert.equal(insufficient.readinessStatus, 'insufficient_data');

  const ready = computeHonestExamReadiness({
    quizAttemptsCount: 5,
    totalQuestionsAnswered: 40,
    overallAccuracyPercent: 88,
    completedChaptersCount: 7,
    totalPublishedChaptersCount: 10,
    activeMistakesCount: 1,
  });
  assert.equal(ready.readinessStatus, 'exam_ready');
  assert.ok(typeof ready.readinessPercent === 'number' && ready.readinessPercent >= 80);
});

test('detectWeakChapters identifies low accuracy and unresolved mistakes honestly', () => {
  const ch: ApiChapter = {
    id: 'ch1',
    subjectId: 's1',
    chapterNumber: 1,
    title: 'Measurements',
    subtitle: 'Units',
    topics: [],
    currentVersion: 'v1.0.0',
    publishStatus: 'published',
    hasDetailedNotes: true,
    hasRevisionNotes: true,
    mcqCount: 5,
    solvedQuestionsCount: 2,
    isLockedByDefault: false,
    estimatedSizeKb: 40,
    updatedAtIso: '2026-04-01T00:00:00Z',
  };
  const weak = detectWeakChapters({
    chapters: [ch],
    subjectsById: new Map([
      [
        's1',
        {
          id: 's1',
          classId: 'c9',
          classLevel: 9,
          boardCode: 'KPK',
          code: 'PHY-9',
          name: 'Physics 9',
          iconKey: 'physics',
          accentHex: '#3B82F6',
          totalChapters: 9,
          publishedChapters: 1,
          isOfflinePackAvailable: true,
          coverageStatus: 'active_seed',
          updatedAtIso: '2026-04-01T00:00:00Z',
        },
      ],
    ]),
    progressByChapterId: new Map(),
    quizStatsByChapterId: new Map([['ch1', { attemptsCount: 2, averagePercent: 42 }]]),
    activeMistakesByChapterId: new Map([['ch1', 3]]),
  });
  assert.equal(weak.length, 1);
  assert.equal(weak[0].priority, 'high');
});

test('computeStudyStreak handles consecutive days, gaps, and grace period', () => {
  const active = computeStudyStreak(['2026-04-01', '2026-04-02', '2026-04-03'], '2026-04-04');
  assert.equal(active.currentStreakDays, 3);
  assert.equal(active.longestStreakDays, 3);

  const broken = computeStudyStreak(['2026-04-01', '2026-04-02', '2026-04-03'], '2026-04-06');
  assert.equal(broken.currentStreakDays, 0);
  assert.equal(broken.longestStreakDays, 3);
});

test('anonymizeLeaderboardHandle hides email/PII and shouldDisplayAd suppresses during study/testing', () => {
  const handle = anonymizeLeaderboardHandle('ahmad.khan@gmail.com', 'device-abc-123');
  assert.ok(!handle.includes('@'));
  assert.ok(handle.startsWith('ahmadkhan-'));

  const adCfg: ApiAdPlacementConfig = {
    enabled: true,
    bannerOnHome: true,
    bannerOnPapers: true,
    suppressDuringQuizAndExam: true,
    suppressDuringNotesReading: true,
    dailyBannerCap: 3,
    providerNote: 'AdMob non-personalized',
  };
  assert.equal(shouldDisplayAd(adCfg, 'home', 0), true);
  assert.equal(shouldDisplayAd(adCfg, 'home', 3), false);
  assert.equal(shouldDisplayAd(adCfg, 'quiz', 0), false);
  assert.equal(shouldDisplayAd(adCfg, 'exam', 0), false);
  assert.equal(shouldDisplayAd(adCfg, 'notes_reader', 0), false);
  assert.equal(shouldDisplayAd(adCfg, 'pdf_reader', 0), false);
});

test('canTransitionPublishStatus supports 4-stage draft -> in_review -> approved -> published workflow', () => {
  assert.equal(canTransitionPublishStatus('draft', 'in_review'), true);
  assert.equal(canTransitionPublishStatus('in_review', 'approved'), true);
  assert.equal(canTransitionPublishStatus('approved', 'published'), true);
  assert.equal(canTransitionPublishStatus('draft', 'published'), false);
});

test('2050 Flagship helpers compute exam countdown, Pomodoro XP, and BISE weightage totals', () => {
  assert.equal(computeDaysUntilExam('2026-05-20', '2026-05-01T12:00:00Z'), 19);
  assert.equal(computeDaysUntilExam('2026-04-20', '2026-05-01'), 0);
  assert.equal(computeDaysUntilExam(null, '2026-05-01'), null);
  assert.equal(computeDaysUntilExam('invalid-date', '2026-05-01'), null);

  assert.equal(computeFocusSessionXp(50, true), 40);
  assert.equal(computeFocusSessionXp(25, true), 20);
  assert.equal(computeFocusSessionXp(15, true), 12);
  assert.equal(computeFocusSessionXp(20, false), 4);

  const summary = calculateBiseWeightageSummary([
    {
      id: 'ch1',
      subjectId: 's1',
      chapterNumber: 1,
      title: 'Measurements',
      subtitle: 'Units',
      boardWeightageMarks: 10,
      topics: [],
      currentVersion: 'v1.0.0',
      publishStatus: 'published',
      hasDetailedNotes: true,
      hasRevisionNotes: true,
      mcqCount: 5,
      solvedQuestionsCount: 2,
      isLockedByDefault: false,
      estimatedSizeKb: 40,
      updatedAtIso: '2026-04-01T00:00:00Z',
    },
    {
      id: 'ch2',
      subjectId: 's1',
      chapterNumber: 2,
      title: 'Kinematics',
      subtitle: 'Motion',
      boardWeightageMarks: 12,
      topics: [],
      currentVersion: 'v1.0.0',
      publishStatus: 'published',
      hasDetailedNotes: true,
      hasRevisionNotes: true,
      mcqCount: 5,
      solvedQuestionsCount: 2,
      isLockedByDefault: false,
      estimatedSizeKb: 40,
      updatedAtIso: '2026-04-01T00:00:00Z',
    },
  ]);
  assert.equal(summary.totalMarksMapped, 22);
  assert.equal(summary.chaptersWithWeightage, 2);
});

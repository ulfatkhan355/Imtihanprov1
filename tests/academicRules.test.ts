import test from 'node:test';
import assert from 'node:assert/strict';
import {
  canTransitionPublishStatus,
  canTransitionSubmissionStatus,
  gradeQuizSubmission,
  summarizeClassCoverage,
  validateApiBaseUrl,
  validateContentBlock,
} from '../shared-contracts/rules';
import type { ApiMcqItem } from '../shared-contracts/types';
import { Stage1MockAdminRepository } from '../admin/services/adminApiService';
import { INITIAL_ADMIN_DATASET } from '../admin/data/mockSeedData';

test('gradeQuizSubmission computes accurate score, incorrect, unanswered, and percentage', () => {
  const mcqs: ApiMcqItem[] = [
    {
      id: 'm1',
      chapterId: 'c1',
      subjectId: 's1',
      classLevel: 10,
      question: 'g on Earth?',
      options: [
        { id: 'A', text: '9.8 m/s²' },
        { id: 'B', text: '10 km/s' },
        { id: 'C', text: '5 m/s²' },
        { id: 'D', text: '0' },
      ],
      correctOptionId: 'A',
      explanation: 'Standard Earth gravity is 9.8 m/s².',
      difficulty: 'easy',
      topicTitle: 'SHM',
    },
    {
      id: 'm2',
      chapterId: 'c1',
      subjectId: 's1',
      classLevel: 10,
      question: 'Wave speed formula?',
      options: [
        { id: 'A', text: 'v = f/λ' },
        { id: 'B', text: 'v = fλ' },
        { id: 'C', text: 'v = λ/f' },
        { id: 'D', text: 'v = f+λ' },
      ],
      correctOptionId: 'B',
      explanation: 'Wave speed equals frequency times wavelength.',
      difficulty: 'easy',
      topicTitle: 'Waves',
    },
    {
      id: 'm3',
      chapterId: 'c1',
      subjectId: 's1',
      classLevel: 10,
      question: 'SI unit of frequency?',
      options: [
        { id: 'A', text: 'Newton' },
        { id: 'B', text: 'Joule' },
        { id: 'C', text: 'Hertz' },
        { id: 'D', text: 'Watt' },
      ],
      correctOptionId: 'C',
      explanation: 'Frequency is measured in Hertz (Hz).',
      difficulty: 'easy',
      topicTitle: 'Waves',
    },
  ];

  const result = gradeQuizSubmission(mcqs, {
    sessionId: 'sess_1',
    studentId: null,
    guestDeviceId: 'guest_abc',
    subjectId: 's1',
    chapterId: 'c1',
    answers: [
      { mcqId: 'm1', selectedOptionId: 'A', markedForReview: false },
      { mcqId: 'm2', selectedOptionId: 'A', markedForReview: true },
      { mcqId: 'm3', selectedOptionId: null, markedForReview: false },
    ],
    elapsedSeconds: 95,
    submittedAtIso: '2026-11-14T22:00:00Z',
  });

  assert.equal(result.totalQuestions, 3);
  assert.equal(result.correctCount, 1);
  assert.equal(result.incorrectCount, 1);
  assert.equal(result.unansweredCount, 1);
  assert.equal(result.percentage, 33);
});

test('validateContentBlock rejects invalid blocks and accepts valid structured blocks', () => {
  const invalidErrors = validateContentBlock({
    id: 'blk_bad',
    orderIndex: 1,
    type: 'table',
    headers: ['Col 1', 'Col 2'],
    rows: [['Only 1 cell']],
  });
  assert.equal(invalidErrors.length, 1);

  const validErrors = validateContentBlock({
    id: 'blk_good',
    orderIndex: 2,
    type: 'math_expression',
    latex: 'F = -kx',
    plainNotation: 'F = -kx',
    caption: 'Hooke law',
  });
  assert.equal(validErrors.length, 0);
});

test('validateApiBaseUrl enforces valid https or localhost URLs', () => {
  assert.equal(validateApiBaseUrl('https://api.imtihanpro.pk/v1').valid, true);
  assert.equal(validateApiBaseUrl('http://localhost:8787/v1').valid, true);
  assert.equal(validateApiBaseUrl('http://insecure.example.com/v1').valid, false);
  assert.equal(validateApiBaseUrl('').valid, false);
});

test('submission and publish state transitions enforce editorial rules', () => {
  assert.equal(canTransitionSubmissionStatus('queued', 'approved'), true);
  assert.equal(canTransitionSubmissionStatus('approved', 'rejected'), false);
  assert.equal(canTransitionPublishStatus('draft', 'in_review'), true);
  assert.equal(canTransitionPublishStatus('draft', 'published'), false);
});

test('summarizeClassCoverage represents Classes 9-12 honestly for KPK', () => {
  const summary = summarizeClassCoverage(INITIAL_ADMIN_DATASET.classes, 'KPK');
  assert.equal(summary[10].coverageStatus, 'active_seed');
  assert.equal(summary[11].coverageStatus, 'structure_ready');
  assert.equal(summary[12].publishedChapters, 0);
});

test('Stage1MockAdminRepository creates subjects, saves blocks, and approves submissions', () => {
  const repo = new Stage1MockAdminRepository();
  const initialCount = repo.getDataset().subjects.length;
  repo.createSubject({
    classId: 'cls_kpk_10',
    classLevel: 10,
    boardCode: 'KPK',
    code: 'CS-10',
    name: 'Computer Science',
    iconKey: 'cs',
    accentHex: '#3B82F6',
    totalChapters: 7,
    publishedChapters: 0,
    isOfflinePackAvailable: false,
    coverageStatus: 'structure_ready',
  });
  assert.equal(repo.getDataset().subjects.length, initialCount + 1);

  const saveRes = repo.saveChapterBlocks('ch_phy10_01', [
    { id: 'b_test', orderIndex: 1, type: 'paragraph', text: 'Updated paragraph block.' },
  ]);
  assert.equal(saveRes.ok, true);

  const transitionRes = repo.transitionAiSubmission('aisub_002', 'approved', 'Verified clean.');
  assert.equal(transitionRes.ok, true);
  const updated = repo.getDataset().aiSubmissions.find((s) => s.id === 'aisub_002');
  assert.equal(updated?.status, 'approved');

  const pdfRes = repo.addPdfPaperRecord(
    {
      boardCode: 'KPK',
      classLevel: 10,
      subjectId: 'sub_kpk10_phy',
      subjectName: 'Physics',
      year: 2024,
      session: 'Annual',
      isSolved: true,
      fileSizeMb: 2.2,
      pageCount: 10,
    },
    'KPK Class 10 Physics — 2024 Solved Annual Paper'
  );
  assert.equal(pdfRes.pdf.objectKey, 'kpk/class-10/physics/2024-annual.pdf');
});

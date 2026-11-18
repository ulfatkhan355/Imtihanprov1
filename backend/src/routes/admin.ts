import {
  bumpSemanticVersion,
  canTransitionPublishStatus,
  canTransitionSubmissionStatus,
  validateContentBlock,
} from '../../../shared-contracts/rules';
import type {
  AdminAnalyticsResponse,
  AdminAuthResponse,
  AdminBootstrapRequest,
  AdminBootstrapStatusResponse,
  AdminChapterWritePayload,
  AdminInfrastructureStatusResponse,
  AdminLoginRequest,
  AdminMcqWritePayload,
  AdminSubjectWritePayload,
  AdminUserProfile,
  ApiAdPlacementConfig,
  ApiAiAgentSubmission,
  ApiEducationalContent,
  BoardCode,
  CapacityPolicyConfig,
  ClassLevel,
  EducationalContentBlock,
  PublishStatus,
} from '../../../shared-contracts/types';
import {
  hashPassword,
  sha256Hex,
  signSessionToken,
  timingSafeEqualStrings,
  verifyPassword,
  verifySessionToken,
} from '../crypto/auth';
import {
  appendAuditLog,
  getAdPlacementPolicy,
  listAcademicClasses,
  listAiSubmissions,
  listAuditLogs,
  listBoards,
  listChapters,
  listFeedbackReports,
  listLatestContents,
  listMcqsFiltered,
  listPastPapers,
  listPdfMetadata,
  listSubjects,
  safeJsonParse,
} from '../db/repository';
import { HttpError, jsonEnvelope, type Env } from '../env';
import { PRIVATE_NO_STORE_CACHE_CONTROL } from '../middleware/cache';
import { enforceRateLimit, getClientIp } from '../middleware/security';
import {
  buildAdminCapacityDashboard,
  recordUsageDelta,
  saveCapacityPolicy,
} from '../services/usageTelemetry';
import {
  BackblazeB2Client,
  buildB2ObjectKey,
  isB2Configured,
  resolveB2Config,
  validatePdfUploadBytes,
} from '../storage/backblazeB2';

interface AuthenticatedAdmin {
  id: string;
  email: string;
  displayName: string;
  role: 'reviewer' | 'admin' | 'super_admin';
}

async function requireAdmin(request: Request, env: Env): Promise<AuthenticatedAdmin> {
  const secret = (env.ADMIN_SESSION_SECRET || '').trim();
  if (!secret || secret.length < 16) {
    throw new HttpError(
      503,
      'ADMIN_SESSION_SECRET_MISSING',
      'ADMIN_SESSION_SECRET must be configured on the Worker.'
    );
  }
  const authHeader = request.headers.get('Authorization') || '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
  if (!token) {
    throw new HttpError(401, 'ADMIN_AUTH_REQUIRED', 'Admin Bearer token is required.');
  }

  const payload = await verifySessionToken(token, secret);
  if (!payload) {
    throw new HttpError(401, 'ADMIN_TOKEN_INVALID', 'Admin session is expired or invalid.');
  }
  if (payload.typ !== 'admin') {
    throw new HttpError(403, 'ADMIN_ROLE_REQUIRED', 'Admin privileges are required for this endpoint.');
  }

  const row = await env.DB.prepare(
    'SELECT id, email, display_name, role, is_active FROM admin_users WHERE id = ? LIMIT 1'
  )
    .bind(payload.sub)
    .first<{
      id: string;
      email: string;
      display_name: string;
      role: 'reviewer' | 'admin' | 'super_admin';
      is_active: number;
    }>();

  if (!row || row.is_active !== 1) {
    throw new HttpError(403, 'ADMIN_ACCOUNT_DISABLED', 'Admin account is disabled or missing.');
  }

  return {
    id: row.id,
    email: row.email,
    displayName: row.display_name,
    role: row.role,
  };
}

async function computeRealAnalytics(env: Env): Promise<AdminAnalyticsResponse> {
  const subjectRows = await env.DB.prepare(
    `SELECT s.name AS subject_name, s.class_level AS class_level,
       COUNT(q.id) AS attempts_count,
       AVG(q.percentage) AS avg_pct
     FROM subjects s
     LEFT JOIN quiz_submissions q ON q.subject_id = s.id
     WHERE s.board_code = 'KPK'
     GROUP BY s.id
     ORDER BY attempts_count DESC, s.class_level ASC, s.name ASC`
  ).all<{
    subject_name: string;
    class_level: ClassLevel;
    attempts_count: number;
    avg_pct: number | null;
  }>();

  const dailyRows = await env.DB.prepare(
    `SELECT substr(submitted_at_iso, 1, 10) AS day_label,
       COUNT(DISTINCT guest_device_id) AS active_learners,
       COUNT(id) AS quiz_submissions
     FROM quiz_submissions
     GROUP BY substr(submitted_at_iso, 1, 10)
     ORDER BY day_label DESC
     LIMIT 7`
  ).all<{
    day_label: string;
    active_learners: number;
    quiz_submissions: number;
  }>();

  return {
    available: true,
    subjectAccuracy: subjectRows.results.map((r) => ({
      subjectName: r.subject_name,
      classLevel: r.class_level,
      averageAccuracyPercent:
        Number(r.attempts_count) > 0 && r.avg_pct !== null ? Math.round(Number(r.avg_pct)) : null,
      attemptsCount: Number(r.attempts_count || 0),
    })),
    weeklyStudyDistribution: dailyRows.results.reverse().map((r) => ({
      dayLabel: r.day_label.slice(5),
      activeLearners: Number(r.active_learners || 0),
      quizSubmissions: Number(r.quiz_submissions || 0),
    })),
  };
}

async function listAdminStudents(env: Env) {
  const rows = await env.DB.prepare(
    `SELECT s.id, s.display_name, s.google_uid, s.selected_class, s.selected_board, s.last_active_iso,
       (SELECT COUNT(*) FROM student_chapter_progress p WHERE p.student_id = s.id AND p.is_completed = 1) AS chapters_completed,
       (SELECT AVG(q.percentage) FROM quiz_submissions q WHERE q.student_id = s.id OR q.guest_device_id = s.guest_device_id) AS quiz_avg
     FROM students s
     ORDER BY s.last_active_iso DESC
     LIMIT 100`
  ).all<{
    id: string;
    display_name: string;
    google_uid: string | null;
    selected_class: ClassLevel;
    selected_board: BoardCode;
    last_active_iso: string;
    chapters_completed: number;
    quiz_avg: number | null;
  }>();

  return rows.results.map((r) => ({
    id: r.id,
    displayName: r.display_name,
    accountType: (r.google_uid ? 'google_synced' : 'guest') as 'guest' | 'google_synced',
    classLevel: r.selected_class,
    boardCode: r.selected_board,
    chaptersCompleted: Number(r.chapters_completed || 0),
    quizAveragePercent: r.quiz_avg !== null ? Math.round(Number(r.quiz_avg)) : 0,
    offlinePacksCount: 0,
    lastActiveIso: r.last_active_iso,
  }));
}

export async function handleAdminRoutes(
  request: Request,
  path: string,
  env: Env,
  headers: Record<string, string>,
  fetchFn: typeof fetch = fetch
): Promise<Response | null> {
  const method = request.method.toUpperCase();
  const privateHeaders = {
    ...headers,
    'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
  };

  // 1. Bootstrap status (public check whether an admin exists yet)
  if (method === 'GET' && path === '/v1/admin/bootstrap-status') {
    const countRow = await env.DB.prepare('SELECT COUNT(*) AS cnt FROM admin_users').first<{
      cnt: number;
    }>();
    const status: AdminBootstrapStatusResponse = {
      hasAdmin: Number(countRow?.cnt ?? 0) > 0,
      bootstrapConfigured: Boolean(env.ADMIN_BOOTSTRAP_SECRET && env.ADMIN_BOOTSTRAP_SECRET.trim().length >= 12),
    };
    return jsonEnvelope(status, env, 200, headers);
  }

  // 2. First-admin secure bootstrap (strictly allowed only when admin_users count == 0)
  if (method === 'POST' && path === '/v1/admin/bootstrap') {
    await enforceRateLimit(env, `admin-bootstrap:${getClientIp(request)}`, 10, 60_000);
    const expectedBootstrap = (env.ADMIN_BOOTSTRAP_SECRET || '').trim();
    if (!expectedBootstrap || expectedBootstrap.length < 12) {
      throw new HttpError(
        503,
        'BOOTSTRAP_SECRET_MISSING',
        'ADMIN_BOOTSTRAP_SECRET (>=12 chars) must be configured on the Worker.'
      );
    }

    const existingCount = await env.DB.prepare('SELECT COUNT(*) AS cnt FROM admin_users').first<{
      cnt: number;
    }>();
    if (Number(existingCount?.cnt ?? 0) > 0) {
      throw new HttpError(
        409,
        'ADMIN_ALREADY_INITIALIZED',
        'An admin account already exists. Use POST /v1/admin/login instead.'
      );
    }

    const body = (await request.json().catch(() => null)) as AdminBootstrapRequest | null;
    if (!body || !body.bootstrapSecret || !body.email || !body.password) {
      throw new HttpError(
        400,
        'INVALID_BOOTSTRAP_REQUEST',
        'bootstrapSecret, email, and password are required.'
      );
    }
    if (!timingSafeEqualStrings(body.bootstrapSecret.trim(), expectedBootstrap)) {
      throw new HttpError(403, 'INVALID_BOOTSTRAP_SECRET', 'Invalid bootstrap secret.');
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email.trim())) {
      throw new HttpError(400, 'INVALID_EMAIL', 'Enter a valid admin email address.');
    }
    if (body.password.length < 10) {
      throw new HttpError(
        400,
        'WEAK_PASSWORD',
        'Admin password must be at least 10 characters long.'
      );
    }

    const sessionSecret = (env.ADMIN_SESSION_SECRET || '').trim();
    if (sessionSecret.length < 16) {
      throw new HttpError(
        503,
        'ADMIN_SESSION_SECRET_MISSING',
        'ADMIN_SESSION_SECRET (>=16 chars) must be configured.'
      );
    }

    const adminId = `adm_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    const nowIso = new Date().toISOString();
    const { hashHex, saltHex } = await hashPassword(body.password);
    const emailNorm = body.email.trim().toLowerCase();
    const displayName = body.displayName?.trim() || 'Super Admin';

    await env.DB.prepare(
      `INSERT INTO admin_users (id, email, display_name, role, password_hash, password_salt, is_active, created_at_iso, last_login_at_iso)
       VALUES (?, ?, ?, 'super_admin', ?, ?, 1, ?, ?)`
    )
      .bind(adminId, emailNorm, displayName, hashHex, saltHex, nowIso, nowIso)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: adminId,
      actorName: displayName,
      action: 'Initialized first super_admin account',
      targetEntity: emailNorm,
    });

    const signed = await signSessionToken(
      { sub: adminId, typ: 'admin', role: 'super_admin', email: emailNorm },
      sessionSecret,
      60 * 60 * 12
    );

    const admin: AdminUserProfile = {
      id: adminId,
      email: emailNorm,
      displayName,
      role: 'super_admin',
      createdAtIso: nowIso,
      lastLoginAtIso: nowIso,
    };
    const resp: AdminAuthResponse = { token: signed.token, expiresAtIso: signed.expiresAtIso, admin };
    return jsonEnvelope(resp, env, 201, headers);
  }

  // 3. Admin Login
  if (method === 'POST' && path === '/v1/admin/login') {
    await enforceRateLimit(env, `admin-login:${getClientIp(request)}`, 20, 60_000);
    const body = (await request.json().catch(() => null)) as AdminLoginRequest | null;
    if (!body || !body.email?.trim() || !body.password) {
      throw new HttpError(400, 'INVALID_LOGIN_REQUEST', 'Email and password are required.');
    }

    const emailNorm = body.email.trim().toLowerCase();
    const row = await env.DB.prepare(
      'SELECT id, email, display_name, role, password_hash, password_salt, is_active, created_at_iso FROM admin_users WHERE email = ? LIMIT 1'
    )
      .bind(emailNorm)
      .first<{
        id: string;
        email: string;
        display_name: string;
        role: 'reviewer' | 'admin' | 'super_admin';
        password_hash: string;
        password_salt: string;
        is_active: number;
        created_at_iso: string;
      }>();

    if (!row || row.is_active !== 1) {
      throw new HttpError(401, 'INVALID_CREDENTIALS', 'Invalid admin email or password.');
    }

    const passwordOk = await verifyPassword(body.password, row.password_hash, row.password_salt);
    if (!passwordOk) {
      throw new HttpError(401, 'INVALID_CREDENTIALS', 'Invalid admin email or password.');
    }

    const sessionSecret = (env.ADMIN_SESSION_SECRET || '').trim();
    if (sessionSecret.length < 16) {
      throw new HttpError(503, 'ADMIN_SESSION_SECRET_MISSING', 'ADMIN_SESSION_SECRET is missing.');
    }

    const nowIso = new Date().toISOString();
    await env.DB.prepare('UPDATE admin_users SET last_login_at_iso = ? WHERE id = ?')
      .bind(nowIso, row.id)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: row.id,
      actorName: row.display_name,
      action: 'Signed in to Admin Console',
      targetEntity: row.email,
    });

    const signed = await signSessionToken(
      { sub: row.id, typ: 'admin', role: row.role, email: row.email },
      sessionSecret,
      60 * 60 * 12
    );

    const resp: AdminAuthResponse = {
      token: signed.token,
      expiresAtIso: signed.expiresAtIso,
      admin: {
        id: row.id,
        email: row.email,
        displayName: row.display_name,
        role: row.role,
        createdAtIso: row.created_at_iso,
        lastLoginAtIso: nowIso,
      },
    };
    return jsonEnvelope(resp, env, 200, headers);
  }

  if (method === 'POST' && path === '/v1/admin/logout') {
    return jsonEnvelope({ loggedOut: true }, env, 200, headers);
  }

  if (!path.startsWith('/v1/admin')) {
    return null;
  }

  // All subsequent /v1/admin/* endpoints strictly require verified Admin JWT
  const admin = await requireAdmin(request, env);

  // 4. Overview full snapshot
  if (method === 'GET' && path === '/v1/admin/overview') {
    const [
      boards,
      classes,
      subjects,
      chapters,
      contents,
      mcqs,
      aiSubmissions,
      pastPapers,
      pdfs,
      students,
      auditLog,
      analytics,
      feedbackReports,
    ] = await Promise.all([
      listBoards(env.DB),
      listAcademicClasses(env.DB),
      listSubjects(env.DB),
      listChapters(env.DB),
      listLatestContents(env.DB),
      listMcqsFiltered(env.DB, { includeUnpublished: true, limit: 200 }),
      listAiSubmissions(env.DB),
      listPastPapers(env.DB),
      listPdfMetadata(env.DB),
      listAdminStudents(env),
      listAuditLogs(env.DB, 40),
      computeRealAnalytics(env),
      listFeedbackReports(env.DB),
    ]);

    return jsonEnvelope(
      {
        boards,
        classes,
        subjects,
        chapters,
        contents,
        mcqs,
        aiSubmissions,
        pastPapers,
        pdfs,
        students,
        auditLog,
        analytics,
        feedbackReports,
      },
      env,
      200,
      headers
    );
  }

  // 5. Subjects CRUD
  if (method === 'POST' && path === '/v1/admin/subjects') {
    const body = (await request.json().catch(() => null)) as Partial<AdminSubjectWritePayload> | null;
    if (!body || !body.name?.trim() || !body.code?.trim() || !body.boardCode || !body.classLevel) {
      throw new HttpError(400, 'INVALID_SUBJECT', 'name, code, boardCode, and classLevel are required.');
    }
    let resolvedClassId = body.classId?.trim() || '';
    if (!resolvedClassId) {
      const clsRow = await env.DB.prepare(
        'SELECT id FROM academic_classes WHERE board_code = ? AND level = ? LIMIT 1'
      )
        .bind(body.boardCode, body.classLevel)
        .first<{ id: string }>();
      if (!clsRow) {
        throw new HttpError(404, 'CLASS_NOT_FOUND', `Academic class ${body.boardCode} Class ${body.classLevel} not found.`);
      }
      resolvedClassId = clsRow.id;
    }
    const id = `sub_${body.boardCode.toLowerCase()}${body.classLevel}_${crypto.randomUUID().slice(0, 6)}`;
    const nowIso = new Date().toISOString();
    await env.DB.prepare(
      `INSERT INTO subjects (id, class_id, class_level, board_code, code, name, icon_key, accent_hex, total_chapters, published_chapters, is_offline_pack_available, coverage_status, updated_at_iso, study_groups_json, description)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        id,
        resolvedClassId,
        body.classLevel,
        body.boardCode,
        body.code.trim(),
        body.name.trim(),
        body.iconKey || 'physics',
        body.accentHex || '#2563EB',
        body.totalChapters ?? 1,
        body.publishedChapters ?? 0,
        body.isOfflinePackAvailable ? 1 : 0,
        body.coverageStatus || 'structure_ready',
        nowIso,
        JSON.stringify(body.studyGroups ?? []),
        body.description?.trim() ?? ''
      )
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Created subject',
      targetEntity: `${body.name.trim()} (Class ${body.classLevel})`,
    });

    return jsonEnvelope({ ...body, id, updatedAtIso: nowIso }, env, 201, headers);
  }

  const subjectIdMatch = path.match(/^\/v1\/admin\/subjects\/([^/]+)$/);
  if (subjectIdMatch && method === 'PUT') {
    const subjectId = decodeURIComponent(subjectIdMatch[1]);
    const body = (await request.json().catch(() => null)) as Partial<AdminSubjectWritePayload> | null;
    if (!body) throw new HttpError(400, 'INVALID_JSON', 'Request body is required.');
    const nowIso = new Date().toISOString();
    const res = await env.DB.prepare(
      `UPDATE subjects SET
        name = COALESCE(?, name),
        code = COALESCE(?, code),
        total_chapters = COALESCE(?, total_chapters),
        published_chapters = COALESCE(?, published_chapters),
        coverage_status = COALESCE(?, coverage_status),
        study_groups_json = COALESCE(?, study_groups_json),
        description = COALESCE(?, description),
        updated_at_iso = ?
       WHERE id = ?`
    )
      .bind(
        body.name?.trim() ?? null,
        body.code?.trim() ?? null,
        body.totalChapters ?? null,
        body.publishedChapters ?? null,
        body.coverageStatus ?? null,
        Array.isArray(body.studyGroups) ? JSON.stringify(body.studyGroups) : null,
        body.description?.trim() ?? null,
        nowIso,
        subjectId
      )
      .run();

    if ((res.meta.changes ?? 0) === 0) {
      throw new HttpError(404, 'SUBJECT_NOT_FOUND', `Subject ${subjectId} not found.`);
    }
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Updated subject',
      targetEntity: subjectId,
    });
    return jsonEnvelope({ id: subjectId, updatedAtIso: nowIso }, env, 200, headers);
  }

  if (subjectIdMatch && method === 'DELETE') {
    const subjectId = decodeURIComponent(subjectIdMatch[1]);
    const res = await env.DB.prepare('DELETE FROM subjects WHERE id = ?').bind(subjectId).run();
    if ((res.meta.changes ?? 0) === 0) {
      throw new HttpError(404, 'SUBJECT_NOT_FOUND', `Subject ${subjectId} not found.`);
    }
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Deleted subject',
      targetEntity: subjectId,
    });
    return jsonEnvelope({ deleted: true, id: subjectId }, env, 200, headers);
  }

  // 6. Chapters CRUD
  if (method === 'POST' && path === '/v1/admin/chapters') {
    const body = (await request.json().catch(() => null)) as AdminChapterWritePayload | null;
    if (!body || !body.subjectId || !body.title?.trim()) {
      throw new HttpError(400, 'INVALID_CHAPTER', 'subjectId and title are required.');
    }
    const id = `ch_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
    const nowIso = new Date().toISOString();
    await env.DB.prepare(
      `INSERT INTO chapters (
        id, subject_id, chapter_number, title, subtitle, topics_json, current_version,
        publish_status, has_detailed_notes, has_revision_notes, mcq_count,
        solved_questions_count, is_locked_by_default, estimated_size_kb, updated_at_iso,
        curriculum_version, difficulty_tag, source_reference,
        title_urdu, subtitle_urdu, board_weightage_marks, pairing_hint, night_before_summary
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        id,
        body.subjectId,
        body.chapterNumber,
        body.title.trim(),
        body.subtitle?.trim() || '',
        JSON.stringify(body.topics ?? []),
        body.currentVersion || 'v0.1-draft',
        body.publishStatus || 'draft',
        body.hasDetailedNotes ? 1 : 0,
        body.hasRevisionNotes ? 1 : 0,
        body.mcqCount ?? 0,
        body.solvedQuestionsCount ?? 0,
        body.isLockedByDefault ? 1 : 0,
        body.estimatedSizeKb ?? 180,
        nowIso,
        body.curriculumVersion || 'KPK-2026',
        body.difficultyTag || 'medium',
        body.sourceReference || 'KPK Textbook Board Peshawar',
        body.titleUrdu?.trim() || '',
        body.subtitleUrdu?.trim() || '',
        body.boardWeightageMarks ?? 0,
        body.pairingHint?.trim() || '',
        body.nightBeforeSummary?.trim() || ''
      )
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Created chapter',
      targetEntity: `Ch ${body.chapterNumber}: ${body.title.trim()}`,
    });

    return jsonEnvelope({ ...body, id, updatedAtIso: nowIso }, env, 201, headers);
  }

  const chapterIdMatch = path.match(/^\/v1\/admin\/chapters\/([^/]+)$/);
  if (chapterIdMatch && method === 'PUT') {
    const chapterId = decodeURIComponent(chapterIdMatch[1]);
    const body = (await request.json().catch(() => null)) as Partial<AdminChapterWritePayload> | null;
    if (!body) throw new HttpError(400, 'INVALID_JSON', 'Request body required.');
    const nowIso = new Date().toISOString();
    const res = await env.DB.prepare(
      `UPDATE chapters SET
        title = COALESCE(?, title),
        subtitle = COALESCE(?, subtitle),
        chapter_number = COALESCE(?, chapter_number),
        topics_json = COALESCE(?, topics_json),
        is_locked_by_default = COALESCE(?, is_locked_by_default),
        curriculum_version = COALESCE(?, curriculum_version),
        difficulty_tag = COALESCE(?, difficulty_tag),
        source_reference = COALESCE(?, source_reference),
        title_urdu = COALESCE(?, title_urdu),
        subtitle_urdu = COALESCE(?, subtitle_urdu),
        board_weightage_marks = COALESCE(?, board_weightage_marks),
        pairing_hint = COALESCE(?, pairing_hint),
        night_before_summary = COALESCE(?, night_before_summary),
        updated_at_iso = ?
       WHERE id = ?`
    )
      .bind(
        body.title?.trim() ?? null,
        body.subtitle?.trim() ?? null,
        body.chapterNumber ?? null,
        Array.isArray(body.topics) ? JSON.stringify(body.topics) : null,
        typeof body.isLockedByDefault === 'boolean' ? (body.isLockedByDefault ? 1 : 0) : null,
        body.curriculumVersion ?? null,
        body.difficultyTag ?? null,
        body.sourceReference ?? null,
        body.titleUrdu?.trim() ?? null,
        body.subtitleUrdu?.trim() ?? null,
        body.boardWeightageMarks ?? null,
        body.pairingHint?.trim() ?? null,
        body.nightBeforeSummary?.trim() ?? null,
        nowIso,
        chapterId
      )
      .run();
    if ((res.meta.changes ?? 0) === 0) {
      throw new HttpError(404, 'CHAPTER_NOT_FOUND', `Chapter ${chapterId} not found.`);
    }
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Updated chapter',
      targetEntity: chapterId,
    });
    return jsonEnvelope({ id: chapterId, updatedAtIso: nowIso }, env, 200, headers);
  }

  if (chapterIdMatch && method === 'DELETE') {
    const chapterId = decodeURIComponent(chapterIdMatch[1]);
    const res = await env.DB.prepare('DELETE FROM chapters WHERE id = ?').bind(chapterId).run();
    if ((res.meta.changes ?? 0) === 0) {
      throw new HttpError(404, 'CHAPTER_NOT_FOUND', `Chapter ${chapterId} not found.`);
    }
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Deleted chapter',
      targetEntity: chapterId,
    });
    return jsonEnvelope({ deleted: true, id: chapterId }, env, 200, headers);
  }

  // 7. Content Drafts & Versioned Publishing
  if (method === 'POST' && path === '/v1/admin/content/drafts') {
    const body = (await request.json().catch(() => null)) as {
      chapterId: string;
      mode?: ApiEducationalContent['mode'];
      language?: 'en' | 'ur';
      blocks: EducationalContentBlock[];
    } | null;

    if (!body || !body.chapterId || !Array.isArray(body.blocks)) {
      throw new HttpError(400, 'INVALID_DRAFT', 'chapterId and blocks array are required.');
    }

    const errors = body.blocks.flatMap((b) => validateContentBlock(b));
    if (errors.length > 0) {
      throw new HttpError(422, 'CONTENT_VALIDATION_FAILED', errors.join(' '));
    }

    const chapter = await env.DB.prepare('SELECT id, current_version, title FROM chapters WHERE id = ?')
      .bind(body.chapterId)
      .first<{ id: string; current_version: string; title: string }>();
    if (!chapter) {
      throw new HttpError(404, 'CHAPTER_NOT_FOUND', `Chapter ${body.chapterId} not found.`);
    }

    const mode: ApiEducationalContent['mode'] = body.mode || 'detailed_notes';
    const maxRow = await env.DB.prepare(
      'SELECT COALESCE(MAX(version_num), 0) AS max_ver FROM chapter_content_versions WHERE chapter_id = ? AND mode = ?'
    )
      .bind(body.chapterId, mode)
      .first<{ max_ver: number }>();

    const nextVerNum = Number(maxRow?.max_ver ?? 0) + 1;
    const normalizedBlocks = body.blocks.map((b, idx) => ({ ...b, orderIndex: idx + 1 }));
    const blocksJson = JSON.stringify(normalizedBlocks);
    const contentHash = await sha256Hex(blocksJson);
    const contentId = `cnt_${body.chapterId}_${nextVerNum}_${crypto.randomUUID().slice(0, 6)}`;
    const nowIso = new Date().toISOString();
    const draftVersion = `${chapter.current_version.replace(/-draft$/, '')}-draft`;

    await env.DB.prepare(
      `INSERT INTO chapter_content_versions (
        id, chapter_id, mode, version, version_num, publish_status, language,
        blocks_json, author_or_source, content_sha256, created_by_admin_id, updated_at_iso
      ) VALUES (?, ?, ?, ?, ?, 'draft', ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        contentId,
        body.chapterId,
        mode,
        draftVersion,
        nextVerNum,
        body.language || 'en',
        blocksJson,
        admin.displayName,
        contentHash,
        admin.id,
        nowIso
      )
      .run();

    const sizeKb = Math.max(24, Math.round(blocksJson.length / 1024) + 16);
    await env.DB.prepare(
      `UPDATE chapters SET
        has_detailed_notes = CASE WHEN ? = 'detailed_notes' THEN 1 ELSE has_detailed_notes END,
        has_revision_notes = CASE WHEN ? = 'revision_notes' THEN 1 ELSE has_revision_notes END,
        estimated_size_kb = ?,
        updated_at_iso = ?
       WHERE id = ?`
    )
      .bind(mode, mode, sizeKb, nowIso, body.chapterId)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: `Saved ${mode} draft (${normalizedBlocks.length} blocks)`,
      targetEntity: chapter.title,
      details: { contentId, version: draftVersion },
    });

    const saved: ApiEducationalContent = {
      id: contentId,
      chapterId: body.chapterId,
      mode,
      version: draftVersion,
      publishStatus: 'draft',
      language: body.language || 'en',
      blocks: normalizedBlocks,
      authorOrSource: admin.displayName,
      updatedAtIso: nowIso,
    };
    return jsonEnvelope(saved, env, 201, headers);
  }

  // 6b. Standalone MCQ Bank CRUD (Stages 4 & 8)
  if (method === 'GET' && path === '/v1/admin/mcqs') {
    const urlObj = new URL(request.url);
    const chapterId = urlObj.searchParams.get('chapterId') || undefined;
    const subjectId = urlObj.searchParams.get('subjectId') || undefined;
    const mcqs = await listMcqsFiltered(env.DB, {
      chapterId,
      subjectId,
      includeUnpublished: true,
      limit: 200,
    });
    return jsonEnvelope(mcqs, env, 200, headers);
  }

  if (method === 'POST' && path === '/v1/admin/mcqs') {
    const body = (await request.json().catch(() => null)) as Partial<AdminMcqWritePayload> | null;
    if (
      !body ||
      !body.chapterId ||
      !body.subjectId ||
      !body.question?.trim() ||
      !Array.isArray(body.options) ||
      body.options.length !== 4 ||
      !body.correctOptionId
    ) {
      throw new HttpError(400, 'INVALID_MCQ', 'chapterId, subjectId, question, 4 options, and correctOptionId are required.');
    }

    const id = `mcq_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
    const nowIso = new Date().toISOString();
    const publishStatus: PublishStatus = body.publishStatus || 'published';
    await env.DB.prepare(
      `INSERT INTO mcqs (
        id, chapter_id, subject_id, class_level, question, options_json,
        correct_option_id, explanation, difficulty, topic_title, past_paper_year,
        publish_status, source_reference, created_at_iso
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        id,
        body.chapterId,
        body.subjectId,
        body.classLevel ?? 9,
        body.question.trim(),
        JSON.stringify(body.options),
        body.correctOptionId,
        body.explanation?.trim() || '',
        body.difficulty || 'medium',
        body.topicTitle?.trim() || 'General',
        body.pastPaperYear ?? null,
        publishStatus,
        body.sourceReference || 'KPK Board Editorial Bank',
        nowIso
      )
      .run();

    await env.DB.prepare(
      `UPDATE chapters SET mcq_count = (SELECT COUNT(*) FROM mcqs WHERE chapter_id = ?), updated_at_iso = ? WHERE id = ?`
    )
      .bind(body.chapterId, nowIso, body.chapterId)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Created MCQ item',
      targetEntity: body.question.trim().slice(0, 60),
      details: { id, chapterId: body.chapterId },
    });

    return jsonEnvelope({ ...body, id, publishStatus }, env, 201, headers);
  }

  const mcqIdMatch = path.match(/^\/v1\/admin\/mcqs\/([^/]+)$/);
  if (mcqIdMatch && method === 'PUT') {
    const mcqId = decodeURIComponent(mcqIdMatch[1]);
    const body = (await request.json().catch(() => null)) as Partial<AdminMcqWritePayload> | null;
    if (!body) throw new HttpError(400, 'INVALID_JSON', 'Request body required.');
    const res = await env.DB.prepare(
      `UPDATE mcqs SET
        question = COALESCE(?, question),
        options_json = COALESCE(?, options_json),
        correct_option_id = COALESCE(?, correct_option_id),
        explanation = COALESCE(?, explanation),
        difficulty = COALESCE(?, difficulty),
        topic_title = COALESCE(?, topic_title),
        publish_status = COALESCE(?, publish_status),
        source_reference = COALESCE(?, source_reference)
       WHERE id = ?`
    )
      .bind(
        body.question?.trim() ?? null,
        Array.isArray(body.options) ? JSON.stringify(body.options) : null,
        body.correctOptionId ?? null,
        body.explanation?.trim() ?? null,
        body.difficulty ?? null,
        body.topicTitle?.trim() ?? null,
        body.publishStatus ?? null,
        body.sourceReference ?? null,
        mcqId
      )
      .run();
    if ((res.meta.changes ?? 0) === 0) {
      throw new HttpError(404, 'MCQ_NOT_FOUND', `MCQ ${mcqId} not found.`);
    }
    return jsonEnvelope({ id: mcqId, updated: true }, env, 200, headers);
  }

  if (mcqIdMatch && method === 'DELETE') {
    const mcqId = decodeURIComponent(mcqIdMatch[1]);
    const existing = await env.DB.prepare('SELECT chapter_id FROM mcqs WHERE id = ? LIMIT 1')
      .bind(mcqId)
      .first<{ chapter_id: string }>();
    if (!existing) throw new HttpError(404, 'MCQ_NOT_FOUND', `MCQ ${mcqId} not found.`);
    await env.DB.prepare('DELETE FROM mcqs WHERE id = ?').bind(mcqId).run();
    await env.DB.prepare(
      `UPDATE chapters SET mcq_count = (SELECT COUNT(*) FROM mcqs WHERE chapter_id = ?), updated_at_iso = ? WHERE id = ?`
    )
      .bind(existing.chapter_id, new Date().toISOString(), existing.chapter_id)
      .run();
    return jsonEnvelope({ deleted: true, id: mcqId }, env, 200, headers);
  }

  // 7. Content Drafts, 4-Step Workflow Status & Versioned Publishing
  const contentStatusMatch = path.match(/^\/v1\/admin\/content\/([^/]+)\/status$/);
  if (method === 'POST' && contentStatusMatch) {
    const contentId = decodeURIComponent(contentStatusMatch[1]);
    const body = (await request.json().catch(() => null)) as { status?: PublishStatus } | null;
    if (!body || !body.status) {
      throw new HttpError(400, 'INVALID_STATUS', 'status is required.');
    }
    const existing = await env.DB.prepare(
      'SELECT id, chapter_id, publish_status FROM chapter_content_versions WHERE id = ? LIMIT 1'
    )
      .bind(contentId)
      .first<{ id: string; chapter_id: string; publish_status: PublishStatus }>();
    if (!existing) {
      throw new HttpError(404, 'CONTENT_NOT_FOUND', `Content version ${contentId} not found.`);
    }
    if (!canTransitionPublishStatus(existing.publish_status, body.status)) {
      throw new HttpError(
        409,
        'INVALID_PUBLISH_TRANSITION',
        `Cannot transition content from ${existing.publish_status} to ${body.status}.`
      );
    }
    const nowIso = new Date().toISOString();
    await env.DB.prepare(
      `UPDATE chapter_content_versions SET publish_status = ?, reviewed_by_admin_id = ?, updated_at_iso = ? WHERE id = ?`
    )
      .bind(body.status, admin.id, nowIso, contentId)
      .run();

    await env.DB.prepare(
      `UPDATE chapters SET publish_status = ?, reviewed_by_name = ?, updated_at_iso = ? WHERE id = ?`
    )
      .bind(body.status, admin.displayName, nowIso, existing.chapter_id)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: `Transitioned content status to ${body.status}`,
      targetEntity: contentId,
    });
    return jsonEnvelope({ contentId, publishStatus: body.status, updatedAtIso: nowIso }, env, 200, headers);
  }

  const publishMatch = path.match(/^\/v1\/admin\/content\/([^/]+)\/publish$/);
  if (method === 'POST' && publishMatch) {
    const contentId = decodeURIComponent(publishMatch[1]);
    const contentRow = await env.DB.prepare(
      'SELECT id, chapter_id, mode, version FROM chapter_content_versions WHERE id = ? LIMIT 1'
    )
      .bind(contentId)
      .first<{ id: string; chapter_id: string; mode: string; version: string }>();

    if (!contentRow) {
      throw new HttpError(404, 'CONTENT_NOT_FOUND', `Content version ${contentId} not found.`);
    }

    const chapter = await env.DB.prepare(
      'SELECT id, subject_id, current_version, title FROM chapters WHERE id = ?'
    )
      .bind(contentRow.chapter_id)
      .first<{ id: string; subject_id: string; current_version: string; title: string }>();

    if (!chapter) {
      throw new HttpError(404, 'CHAPTER_NOT_FOUND', 'Parent chapter missing.');
    }

    const nextVersion = bumpSemanticVersion(chapter.current_version, 'minor');
    const nowIso = new Date().toISOString();

    await env.DB.prepare(
      `UPDATE chapter_content_versions SET publish_status = 'published', version = ?, reviewed_by_admin_id = ?, published_at_iso = ?, updated_at_iso = ? WHERE id = ?`
    )
      .bind(nextVersion, admin.id, nowIso, nowIso, contentId)
      .run();

    await env.DB.prepare(
      `UPDATE chapters SET current_version = ?, publish_status = 'published', reviewed_by_name = ?, is_locked_by_default = 0, updated_at_iso = ? WHERE id = ?`
    )
      .bind(nextVersion, admin.displayName, nowIso, chapter.id)
      .run();

    await env.DB.prepare(
      `UPDATE subjects SET
        published_chapters = (SELECT COUNT(*) FROM chapters WHERE subject_id = ? AND publish_status = 'published'),
        updated_at_iso = ?
       WHERE id = ?`
    )
      .bind(chapter.subject_id, nowIso, chapter.subject_id)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: `Published ${nextVersion}`,
      targetEntity: chapter.title,
      details: { contentId },
    });

    return jsonEnvelope(
      { contentId, chapterId: chapter.id, version: nextVersion, publishStatus: 'published', publishedAtIso: nowIso },
      env,
      200,
      headers
    );
  }

  // 8. AI Submissions Review & Promotion
  if (method === 'GET' && path === '/v1/admin/ai-submissions') {
    const items = await listAiSubmissions(env.DB);
    return jsonEnvelope(items, env, 200, headers);
  }

  const aiDecisionMatch = path.match(/^\/v1\/admin\/ai-submissions\/([^/]+)\/decision$/);
  if (method === 'POST' && aiDecisionMatch) {
    const submissionId = decodeURIComponent(aiDecisionMatch[1]);
    const body = (await request.json().catch(() => null)) as {
      status: ApiAiAgentSubmission['status'];
      reviewerNotes?: string;
    } | null;
    if (!body || !body.status) {
      throw new HttpError(400, 'INVALID_DECISION', 'status is required.');
    }

    const existing = await env.DB.prepare(
      'SELECT id, status, chapter_title FROM ai_agent_submissions WHERE id = ? LIMIT 1'
    )
      .bind(submissionId)
      .first<{ id: string; status: ApiAiAgentSubmission['status']; chapter_title: string }>();

    if (!existing) {
      throw new HttpError(404, 'SUBMISSION_NOT_FOUND', `Submission ${submissionId} not found.`);
    }
    if (!canTransitionSubmissionStatus(existing.status, body.status)) {
      throw new HttpError(
        409,
        'INVALID_STATE_TRANSITION',
        `Cannot transition submission from ${existing.status} to ${body.status}.`
      );
    }

    const nowIso = new Date().toISOString();
    await env.DB.prepare(
      'UPDATE ai_agent_submissions SET status = ?, reviewer_notes = ?, reviewed_by_admin_id = ?, updated_at_iso = ? WHERE id = ?'
    )
      .bind(body.status, (body.reviewerNotes || '').trim(), admin.id, nowIso, submissionId)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: `Marked AI submission ${body.status}`,
      targetEntity: existing.chapter_title,
      details: { submissionId },
    });

    return jsonEnvelope({ submissionId, status: body.status, updatedAtIso: nowIso }, env, 200, headers);
  }

  const aiPromoteMatch = path.match(/^\/v1\/admin\/ai-submissions\/([^/]+)\/promote$/);
  if (method === 'POST' && aiPromoteMatch) {
    const submissionId = decodeURIComponent(aiPromoteMatch[1]);
    const sub = await env.DB.prepare('SELECT * FROM ai_agent_submissions WHERE id = ? LIMIT 1')
      .bind(submissionId)
      .first<{
        id: string;
        status: string;
        subject_id: string;
        target_class: ClassLevel;
        chapter_id: string | null;
        chapter_title: string;
        submission_type: string;
        payload_json: string;
        agent_name: string;
      }>();

    if (!sub) {
      throw new HttpError(404, 'SUBMISSION_NOT_FOUND', `Submission ${submissionId} not found.`);
    }
    if (sub.status !== 'approved') {
      throw new HttpError(409, 'PROMOTE_REQUIRES_APPROVAL', 'Approve the AI submission before promoting it to draft content.');
    }

    const parsed = safeJsonParse<{
      language?: 'en' | 'ur';
      blocks?: EducationalContentBlock[];
      mcqs?: ApiAiAgentSubmission['mcqs'];
    }>(sub.payload_json, {});

    const nowIso = new Date().toISOString();
    let targetChapterId = sub.chapter_id;

    if (!targetChapterId) {
      const chCount = await env.DB.prepare('SELECT COUNT(*) AS cnt FROM chapters WHERE subject_id = ?')
        .bind(sub.subject_id)
        .first<{ cnt: number }>();
      targetChapterId = `ch_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
      await env.DB.prepare(
        `INSERT INTO chapters (id, subject_id, chapter_number, title, subtitle, topics_json, current_version, publish_status, has_detailed_notes, has_revision_notes, mcq_count, solved_questions_count, is_locked_by_default, estimated_size_kb, updated_at_iso)
         VALUES (?, ?, ?, ?, 'Promoted from approved AI submission', '[]', 'v0.1-draft', 'draft', 0, 0, 0, 0, 1, 120, ?)`
      )
        .bind(targetChapterId, sub.subject_id, Number(chCount?.cnt ?? 0) + 1, sub.chapter_title, nowIso)
        .run();
    }

    let promotedContentId: string | null = null;
    if (Array.isArray(parsed.blocks) && parsed.blocks.length > 0) {
      const mode = sub.submission_type === 'revision_notes' ? 'revision_notes' : 'detailed_notes';
      const blocksJson = JSON.stringify(parsed.blocks);
      const hash = await sha256Hex(blocksJson);
      promotedContentId = `cnt_${targetChapterId}_${crypto.randomUUID().slice(0, 6)}`;
      await env.DB.prepare(
        `INSERT INTO chapter_content_versions (id, chapter_id, mode, version, version_num, publish_status, language, blocks_json, author_or_source, content_sha256, created_by_admin_id, updated_at_iso)
         VALUES (?, ?, ?, 'v0.1-draft', 1, 'draft', ?, ?, ?, ?, ?, ?)`
      )
        .bind(
          promotedContentId,
          targetChapterId,
          mode,
          parsed.language || 'en',
          blocksJson,
          `AI Promoted (${sub.agent_name})`,
          hash,
          admin.id,
          nowIso
        )
        .run();
    }

    if (Array.isArray(parsed.mcqs) && parsed.mcqs.length > 0) {
      for (const m of parsed.mcqs) {
        const mcqId = `mcq_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
        await env.DB.prepare(
          `INSERT INTO mcqs (id, chapter_id, subject_id, class_level, question, options_json, correct_option_id, explanation, difficulty, topic_title, created_at_iso)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
        )
          .bind(
            mcqId,
            targetChapterId,
            sub.subject_id,
            sub.target_class,
            m.question,
            JSON.stringify(m.options),
            m.correctOptionId,
            m.explanation || '',
            m.difficulty || 'medium',
            m.topicTitle || sub.chapter_title,
            nowIso
          )
          .run();
      }
      await env.DB.prepare(
        `UPDATE chapters SET mcq_count = (SELECT COUNT(*) FROM mcqs WHERE chapter_id = ?), updated_at_iso = ? WHERE id = ?`
      )
        .bind(targetChapterId, nowIso, targetChapterId)
        .run();
    }

    await env.DB.prepare(
      'UPDATE ai_agent_submissions SET chapter_id = ?, promoted_content_id = ?, updated_at_iso = ? WHERE id = ?'
    )
      .bind(targetChapterId, promotedContentId, nowIso, submissionId)
      .run();

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Promoted approved AI submission to chapter draft',
      targetEntity: sub.chapter_title,
      details: { submissionId, targetChapterId, promotedContentId },
    });

    return jsonEnvelope({ submissionId, targetChapterId, promotedContentId }, env, 200, headers);
  }

  // 9. PDF Upload to Backblaze B2 + D1 Metadata
  if (method === 'POST' && path === '/v1/admin/pdfs') {
    const contentType = request.headers.get('Content-Type') || '';
    if (!contentType.includes('multipart/form-data')) {
      throw new HttpError(
        400,
        'MULTIPART_REQUIRED',
        'Upload PDFs using multipart/form-data with a binary "file" field.'
      );
    }

    const form = await request.formData();
    const fileEntry = form.get('file');
    if (!fileEntry || typeof fileEntry === 'string') {
      throw new HttpError(400, 'PDF_FILE_REQUIRED', 'Form field "file" (PDF binary) is required.');
    }

    const title = String(form.get('title') || '').trim();
    const subjectId = String(form.get('subjectId') || '').trim();
    const year = Number.parseInt(String(form.get('year') || '2025'), 10);
    const session = String(form.get('session') || 'Annual') === 'Supplementary' ? 'Supplementary' : 'Annual';
    const isSolved = String(form.get('isSolved') || 'false') === 'true';
    const pageCount = Math.max(1, Number.parseInt(String(form.get('pageCount') || '1'), 10) || 1);
    const categoryRaw = String(form.get('category') || 'past_paper');
    const category =
      categoryRaw === 'notes_pack' || categoryRaw === 'syllabus' || categoryRaw === 'textbook'
        ? categoryRaw
        : 'past_paper';

    if (!title || !subjectId) {
      throw new HttpError(400, 'MISSING_PDF_METADATA', 'title and subjectId fields are required.');
    }

    const subject = await env.DB.prepare(
      'SELECT id, name, board_code, class_level FROM subjects WHERE id = ? LIMIT 1'
    )
      .bind(subjectId)
      .first<{ id: string; name: string; board_code: BoardCode; class_level: ClassLevel }>();
    if (!subject) {
      throw new HttpError(404, 'SUBJECT_NOT_FOUND', `Subject ${subjectId} does not exist.`);
    }

    const fileBlob = fileEntry as File;
    const arrayBuffer = await fileBlob.arrayBuffer();
    const bytes = new Uint8Array(arrayBuffer);

    const validation = validatePdfUploadBytes(bytes, fileBlob.type, fileBlob.name || 'document.pdf');
    if (!validation.valid) {
      throw new HttpError(422, 'INVALID_PDF_FILE', validation.reason || 'Invalid PDF upload.');
    }

    const suffix = crypto.randomUUID().slice(0, 6);
    const objectKey = buildB2ObjectKey(
      subject.board_code,
      subject.class_level,
      subject.name,
      `${year}-${session.toLowerCase()}-${suffix}.pdf`
    );

    const b2Client = new BackblazeB2Client(env, fetchFn);
    const uploaded = await b2Client.putObject(objectKey, bytes, 'application/pdf');
    recordUsageDelta({ b2UploadBytes: uploaded.fileSizeBytes });

    const pdfId = `pdf_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
    const paperId = `pp_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
    const nowIso = new Date().toISOString();
    const fileSizeMb = Number((uploaded.fileSizeBytes / (1024 * 1024)).toFixed(2));

    await env.DB.prepare(
      `INSERT INTO pdf_assets (
        id, title, category, class_level, board_code, subject_id, object_key,
        mime_type, file_size_bytes, page_count, checksum_sha256, offline_cacheable,
        version, created_at_iso, updated_at_iso
      ) VALUES (?, ?, ?, ?, ?, ?, ?, 'application/pdf', ?, ?, ?, 1, '1.0', ?, ?)`
    )
      .bind(
        pdfId,
        title,
        category,
        subject.class_level,
        subject.board_code,
        subject.id,
        uploaded.objectKey,
        uploaded.fileSizeBytes,
        pageCount,
        uploaded.checksumSha256,
        nowIso,
        nowIso
      )
      .run();

    if (category === 'past_paper') {
      await env.DB.prepare(
        `INSERT INTO past_papers (
          id, board_code, class_level, subject_id, subject_name, year, session,
          is_solved, pdf_metadata_id, file_size_mb, page_count, download_count, created_at_iso
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)`
      )
        .bind(
          paperId,
          subject.board_code,
          subject.class_level,
          subject.id,
          subject.name,
          Number.isFinite(year) ? year : 2025,
          session,
          isSolved ? 1 : 0,
          pdfId,
          fileSizeMb,
          pageCount,
          nowIso
        )
        .run();
    } else if (category === 'textbook') {
      await env.DB.prepare('UPDATE subjects SET textbook_pdf_id = ?, updated_at_iso = ? WHERE id = ?')
        .bind(pdfId, nowIso, subject.id)
        .run();
    }

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Uploaded PDF to Backblaze B2',
      targetEntity: title,
      details: { pdfId, objectKey: uploaded.objectKey, bytes: uploaded.fileSizeBytes },
    });

    return jsonEnvelope(
      {
        pdf: {
          id: pdfId,
          title,
          category,
          classLevel: subject.class_level,
          boardCode: subject.board_code,
          subjectId: subject.id,
          objectKey: uploaded.objectKey,
          fileSizeBytes: uploaded.fileSizeBytes,
          pageCount,
          checksumSha256: uploaded.checksumSha256,
          offlineCacheable: true,
          version: '1.0',
        },
        paper: {
          id: paperId,
          boardCode: subject.board_code,
          classLevel: subject.class_level,
          subjectId: subject.id,
          subjectName: subject.name,
          year: Number.isFinite(year) ? year : 2025,
          session,
          isSolved,
          pdfMetadataId: pdfId,
          fileSizeMb,
          pageCount,
          downloadCount: 0,
        },
      },
      env,
      201,
      headers
    );
  }

  const pdfDeleteMatch = path.match(/^\/v1\/admin\/pdfs\/([^/]+)$/);
  if (method === 'DELETE' && pdfDeleteMatch) {
    const pdfId = decodeURIComponent(pdfDeleteMatch[1]);
    const pdf = await env.DB.prepare('SELECT id, title, object_key FROM pdf_assets WHERE id = ? LIMIT 1')
      .bind(pdfId)
      .first<{ id: string; title: string; object_key: string }>();

    if (!pdf) {
      throw new HttpError(404, 'PDF_NOT_FOUND', `PDF ${pdfId} not found.`);
    }

    if (isB2Configured(env)) {
      const b2Client = new BackblazeB2Client(env, fetchFn);
      await b2Client.deleteObject(pdf.object_key);
    }

    await env.DB.prepare('DELETE FROM pdf_assets WHERE id = ?').bind(pdfId).run();
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Deleted PDF asset',
      targetEntity: pdf.title,
      details: { pdfId, objectKey: pdf.object_key },
    });
    return jsonEnvelope({ deleted: true, id: pdfId }, env, 200, headers);
  }

  // 10. Feedback Triage, Students, Analytics, Infrastructure & Settings
  if (method === 'GET' && path === '/v1/admin/feedback') {
    const reports = await listFeedbackReports(env.DB);
    return jsonEnvelope(reports, env, 200, headers);
  }

  const feedbackIdMatch = path.match(/^\/v1\/admin\/feedback\/([^/]+)$/);
  if (method === 'PATCH' && feedbackIdMatch) {
    const feedbackId = decodeURIComponent(feedbackIdMatch[1]);
    const body = (await request.json().catch(() => null)) as {
      status?: 'open' | 'triaged' | 'resolved' | 'dismissed';
      adminResolutionNote?: string;
    } | null;
    if (!body || !body.status) {
      throw new HttpError(400, 'INVALID_FEEDBACK_UPDATE', 'status is required.');
    }
    const nowIso = new Date().toISOString();
    const res = await env.DB.prepare(
      `UPDATE feedback_reports SET
        status = ?,
        admin_resolution_note = COALESCE(?, admin_resolution_note),
        updated_at_iso = ?
       WHERE id = ?`
    )
      .bind(body.status, body.adminResolutionNote?.trim() ?? null, nowIso, feedbackId)
      .run();
    if ((res.meta.changes ?? 0) === 0) {
      throw new HttpError(404, 'FEEDBACK_NOT_FOUND', `Feedback report ${feedbackId} not found.`);
    }
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: `Marked feedback ${body.status}`,
      targetEntity: feedbackId,
    });
    return jsonEnvelope({ id: feedbackId, status: body.status, updatedAtIso: nowIso }, env, 200, headers);
  }

  if (method === 'GET' && path === '/v1/admin/students') {
    const students = await listAdminStudents(env);
    return jsonEnvelope(students, env, 200, headers);
  }

  if (method === 'GET' && path === '/v1/admin/analytics') {
    const analytics = await computeRealAnalytics(env);
    return jsonEnvelope(analytics, env, 200, headers);
  }

  if (method === 'GET' && path === '/v1/admin/infrastructure') {
    const start = Date.now();
    let d1Connected = false;
    let tableCount: number | null = null;
    try {
      const cnt = await env.DB.prepare(
        "SELECT COUNT(*) AS cnt FROM sqlite_master WHERE type = 'table'"
      ).first<{ cnt: number }>();
      d1Connected = true;
      tableCount = Number(cnt?.cnt ?? 0);
    } catch {
      d1Connected = false;
    }
    const latencyMs = Date.now() - start;

    const b2Cfg = resolveB2Config(env);
    const b2Client = new BackblazeB2Client(env, fetchFn);
    const b2Health = await b2Client.checkBucketHealth();
    const aiConfigured = Boolean((env.GEMINI_API_KEY || env.AI_PROVIDER_API_KEY || '').trim());

    const infra: AdminInfrastructureStatusResponse = {
      workerName: 'imtihan-pro-api',
      workerReachable: true,
      apiVersion: env.API_VERSION || '2.0.0',
      d1: {
        databaseName: 'imtihan-pro-db',
        databaseId: '079128c1-203b-477b-8c4b-d6b34daf8bbe',
        connected: d1Connected,
        latencyMs: d1Connected ? latencyMs : null,
        tableCount,
      },
      b2: {
        bucketName: b2Cfg.bucketName,
        bucketId: b2Cfg.bucketId,
        endpoint: b2Cfg.endpoint,
        configured: isB2Configured(env),
        reachable: isB2Configured(env) ? b2Health.reachable : null,
        message: b2Health.message,
      },
      googleAuth: {
        clientIdConfigured: Boolean(env.GOOGLE_OAUTH_CLIENT_ID && env.GOOGLE_OAUTH_CLIENT_ID.trim()),
      },
      agentIngest: {
        secretConfigured: Boolean(env.AGENT_INGEST_SECRET && env.AGENT_INGEST_SECRET.trim()),
      },
      aiTutor: {
        configured: aiConfigured,
        provider: aiConfigured ? 'gemini' : 'unconfigured',
      },
      checkedAtIso: new Date().toISOString(),
    };
    return jsonEnvelope(infra, env, 200, headers);
  }

  if (method === 'GET' && path === '/v1/admin/settings') {
    const row = await env.DB.prepare(
      "SELECT setting_json FROM app_settings WHERE setting_key = 'platform_policy' LIMIT 1"
    ).first<{ setting_json: string }>();
    const policy = safeJsonParse<Record<string, unknown>>(row?.setting_json, {
      guestFirstMandatory: true,
      allowOptionalGoogleSync: true,
      defaultBoardCode: 'KPK',
      offlineMaxCacheMb: 200,
    });
    const adPlacement = await getAdPlacementPolicy(env.DB);
    return jsonEnvelope({ ...policy, adPlacement }, env, 200, headers);
  }

  if (method === 'PUT' && path === '/v1/admin/settings') {
    const body = (await request.json().catch(() => null)) as
      | (Record<string, unknown> & { adPlacement?: ApiAdPlacementConfig })
      | null;
    if (!body) throw new HttpError(400, 'INVALID_SETTINGS', 'Settings JSON object is required.');
    const nowIso = new Date().toISOString();

    const { adPlacement, ...policyRest } = body;
    await env.DB.prepare(
      `INSERT INTO app_settings (setting_key, setting_json, updated_at_iso) VALUES ('platform_policy', ?, ?)
       ON CONFLICT(setting_key) DO UPDATE SET setting_json = excluded.setting_json, updated_at_iso = excluded.updated_at_iso`
    )
      .bind(JSON.stringify(policyRest), nowIso)
      .run();

    if (adPlacement && typeof adPlacement === 'object') {
      const normalizedAd: ApiAdPlacementConfig = {
        enabled: Boolean(adPlacement.enabled),
        bannerOnHome: Boolean(adPlacement.bannerOnHome),
        bannerOnPapers: Boolean(adPlacement.bannerOnPapers),
        suppressDuringQuizAndExam: true, // Always strictly enforced
        suppressDuringNotesReading: true, // Always strictly enforced
        dailyBannerCap: Math.max(1, Math.min(10, Number(adPlacement.dailyBannerCap ?? 4))),
        providerNote: String(adPlacement.providerNote || '').slice(0, 240),
      };
      await env.DB.prepare(
        `INSERT INTO app_settings (setting_key, setting_json, updated_at_iso) VALUES ('ad_placement', ?, ?)
         ON CONFLICT(setting_key) DO UPDATE SET setting_json = excluded.setting_json, updated_at_iso = excluded.updated_at_iso`
      )
        .bind(JSON.stringify(normalizedAd), nowIso)
        .run();
    }

    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Updated platform & ad placement policy settings',
      targetEntity: 'platform_policy',
    });

    return jsonEnvelope(body, env, 200, privateHeaders);
  }

  // 11. Free-Tier Capacity Telemetry, Projections & Cost-Protection Policy
  if (method === 'GET' && path === '/v1/admin/capacity') {
    const dashboard = await buildAdminCapacityDashboard(env);
    return jsonEnvelope(dashboard, env, 200, privateHeaders);
  }

  if (method === 'PUT' && path === '/v1/admin/capacity-policy') {
    const body = (await request.json().catch(() => null)) as Partial<CapacityPolicyConfig> | null;
    if (!body || typeof body !== 'object') {
      throw new HttpError(400, 'INVALID_CAPACITY_POLICY', 'Capacity policy JSON object is required.');
    }
    const updated = await saveCapacityPolicy(env.DB, body);
    await appendAuditLog(env.DB, {
      actorAdminId: admin.id,
      actorName: admin.displayName,
      action: 'Updated free-tier capacity & quota protection policy',
      targetEntity: 'capacity_policy',
      details: updated as unknown as Record<string, unknown>,
    });
    return jsonEnvelope(updated, env, 200, privateHeaders);
  }

  return null;
}

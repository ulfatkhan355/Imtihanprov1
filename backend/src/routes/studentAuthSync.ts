import { anonymizeLeaderboardHandle, computeStudyStreak, mergeChapterProgress } from '../../../shared-contracts/rules';
import type {
  ApiAuthSession,
  ApiStudentProgress,
  BoardCode,
  ClassLevel,
  KpkSubBoardCode,
  LanguageCode,
  StudentGoogleExchangeRequest,
  StudentGuestAuthRequest,
  StudentProgressSyncRequest,
  StudyGroupCode,
} from '../../../shared-contracts/types';
import {
  signSessionToken,
  verifyGoogleIdToken,
  verifySessionToken,
} from '../crypto/auth';
import { getStudentProgressRecord } from '../db/repository';
import { HttpError, jsonEnvelope, type D1PreparedStatement, type Env } from '../env';
import { PRIVATE_NO_STORE_CACHE_CONTROL } from '../middleware/cache';

export async function resolveStudentFromRequest(
  request: Request,
  env: Env
): Promise<{
  studentId: string;
  guestDeviceId: string;
  isGoogleLinked: boolean;
}> {
  const authHeader = request.headers.get('Authorization') || '';
  const bearer = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
  if (bearer && env.ADMIN_SESSION_SECRET) {
    const payload = await verifySessionToken(bearer, env.ADMIN_SESSION_SECRET);
    if (payload && payload.typ === 'student') {
      const row = await env.DB.prepare(
        'SELECT id, guest_device_id, google_uid FROM students WHERE id = ? LIMIT 1'
      )
        .bind(payload.sub)
        .first<{ id: string; guest_device_id: string; google_uid: string | null }>();
      if (row) {
        return {
          studentId: row.id,
          guestDeviceId: row.guest_device_id,
          isGoogleLinked: Boolean(row.google_uid),
        };
      }
    }
  }

  const deviceId = (request.headers.get('X-Guest-Device-Id') || '').trim();
  if (!deviceId) {
    throw new HttpError(
      401,
      'STUDENT_IDENTITY_REQUIRED',
      'Provide a student Bearer token or X-Guest-Device-Id header.'
    );
  }

  const existing = await env.DB.prepare(
    'SELECT id, guest_device_id, google_uid FROM students WHERE guest_device_id = ? LIMIT 1'
  )
    .bind(deviceId)
    .first<{ id: string; guest_device_id: string; google_uid: string | null }>();

  if (existing) {
    return {
      studentId: existing.id,
      guestDeviceId: existing.guest_device_id,
      isGoogleLinked: Boolean(existing.google_uid),
    };
  }

  const nowIso = new Date().toISOString();
  const newId = `stu_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
  const aliasHandle = anonymizeLeaderboardHandle('Guest Learner', deviceId);
  await env.DB.prepare(
    `INSERT INTO students (
      id, guest_device_id, google_uid, email, display_name, avatar_url,
      selected_class, selected_board, selected_sub_board, study_group, language,
      study_hours_total, daily_goal_percent, alias_handle,
      last_active_iso, created_at_iso, updated_at_iso
    ) VALUES (?, ?, NULL, NULL, 'Guest Learner', NULL, 11, 'KPK', 'PESHAWAR', 'PRE_MEDICAL', 'en', 0, 0, ?, ?, ?, ?)`
  )
    .bind(newId, deviceId, aliasHandle, nowIso, nowIso, nowIso)
    .run();

  return { studentId: newId, guestDeviceId: deviceId, isGoogleLinked: false };
}

export async function recordStudentActivityDay(
  env: Env,
  studentId: string,
  delta: { minutes?: number; quizzes?: number; chapters?: number },
  dayIso: string = new Date().toISOString()
): Promise<void> {
  const dayYmd = dayIso.slice(0, 10);
  await env.DB.prepare(
    `INSERT INTO student_activity_days (student_id, day_ymd, minutes_logged, quizzes_completed, chapters_studied)
     VALUES (?, ?, ?, ?, ?)
     ON CONFLICT(student_id, day_ymd) DO UPDATE SET
       minutes_logged = minutes_logged + excluded.minutes_logged,
       quizzes_completed = quizzes_completed + excluded.quizzes_completed,
       chapters_studied = chapters_studied + excluded.chapters_studied`
  )
    .bind(studentId, dayYmd, delta.minutes ?? 0, delta.quizzes ?? 0, delta.chapters ?? 0)
    .run();

  const days = await env.DB.prepare(
    'SELECT day_ymd FROM student_activity_days WHERE student_id = ? ORDER BY day_ymd ASC'
  )
    .bind(studentId)
    .all<{ day_ymd: string }>();

  const streak = computeStudyStreak(
    days.results.map((d) => d.day_ymd),
    dayIso
  );
  await env.DB.prepare(
    'UPDATE students SET current_streak_days = ?, longest_streak_days = MAX(longest_streak_days, ?) WHERE id = ?'
  )
    .bind(streak.currentStreakDays, streak.longestStreakDays, studentId)
    .run();
}

export async function handleStudentAuthAndSyncRoutes(
  request: Request,
  path: string,
  env: Env,
  headers: Record<string, string>,
  fetchFn: typeof fetch = fetch
): Promise<Response | null> {
  const method = request.method.toUpperCase();

  if (method === 'POST' && path === '/v1/auth/guest') {
    const body = (await request.json().catch(() => null)) as StudentGuestAuthRequest | null;
    if (!body || !body.guestDeviceId?.trim()) {
      throw new HttpError(400, 'INVALID_GUEST_REQUEST', 'guestDeviceId is required.');
    }
    const deviceId = body.guestDeviceId.trim();
    const nowIso = new Date().toISOString();
    const displayName = body.displayName?.trim() || 'Guest Learner';
    const selectedClass: ClassLevel = [9, 10, 11, 12].includes(Number(body.selectedClass))
      ? (Number(body.selectedClass) as ClassLevel)
      : 11;
    const selectedBoard: BoardCode = body.selectedBoard || 'KPK';
    const selectedSubBoard: KpkSubBoardCode = body.selectedSubBoard || 'PESHAWAR';
    const studyGroup: StudyGroupCode = body.studyGroup || (selectedClass >= 11 ? 'PRE_MEDICAL' : 'SCIENCE_BIOLOGY');
    const language: LanguageCode = body.language === 'ur' ? 'ur' : 'en';

    let row = await env.DB.prepare(
      'SELECT id, display_name, email, avatar_url, google_uid FROM students WHERE guest_device_id = ? LIMIT 1'
    )
      .bind(deviceId)
      .first<{
        id: string;
        display_name: string;
        email: string | null;
        avatar_url: string | null;
        google_uid: string | null;
      }>();

    if (!row) {
      const newId = `stu_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
      const aliasHandle = anonymizeLeaderboardHandle(displayName, deviceId);
      await env.DB.prepare(
        `INSERT INTO students (
          id, guest_device_id, google_uid, email, display_name, avatar_url,
          selected_class, selected_board, selected_sub_board, study_group, language,
          study_hours_total, daily_goal_percent, alias_handle,
          last_active_iso, created_at_iso, updated_at_iso
        ) VALUES (?, ?, NULL, NULL, ?, NULL, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?)`
      )
        .bind(
          newId,
          deviceId,
          displayName,
          selectedClass,
          selectedBoard,
          selectedSubBoard,
          studyGroup,
          language,
          aliasHandle,
          nowIso,
          nowIso,
          nowIso
        )
        .run();
      row = { id: newId, display_name: displayName, email: null, avatar_url: null, google_uid: null };
    } else {
      await env.DB.prepare(
        `UPDATE students SET
          selected_class = COALESCE(?, selected_class),
          selected_board = COALESCE(?, selected_board),
          selected_sub_board = COALESCE(?, selected_sub_board),
          study_group = COALESCE(?, study_group),
          language = COALESCE(?, language),
          last_active_iso = ?, updated_at_iso = ?
         WHERE id = ?`
      )
        .bind(
          body.selectedClass ?? null,
          body.selectedBoard ?? null,
          body.selectedSubBoard ?? null,
          body.studyGroup ?? null,
          body.language ?? null,
          nowIso,
          nowIso,
          row.id
        )
        .run();
    }

    let token: string | undefined;
    let expiresAtIso: string | null = null;
    if (env.ADMIN_SESSION_SECRET && env.ADMIN_SESSION_SECRET.trim().length >= 16) {
      const signed = await signSessionToken(
        {
          sub: row.id,
          typ: 'student',
          role: 'student',
          guestDeviceId: deviceId,
        },
        env.ADMIN_SESSION_SECRET,
        60 * 60 * 24 * 30
      );
      token = signed.token;
      expiresAtIso = signed.expiresAtIso;
    }

    const session: ApiAuthSession = {
      mode: row.google_uid ? 'google_authenticated' : 'guest',
      guestDeviceId: deviceId,
      token,
      user: {
        id: row.id,
        displayName: row.display_name,
        email: row.email,
        avatarUrl: row.avatar_url,
        role: 'student',
      },
      accessTokenExpiresAtIso: expiresAtIso,
    };

    return jsonEnvelope(session, env, 200, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  if (method === 'POST' && path === '/v1/auth/google/exchange') {
    const body = (await request.json().catch(() => null)) as StudentGoogleExchangeRequest | null;
    if (!body || !body.idToken || !body.guestDeviceId?.trim()) {
      throw new HttpError(400, 'INVALID_GOOGLE_EXCHANGE', 'idToken and guestDeviceId are required.');
    }

    let identity;
    try {
      identity = await verifyGoogleIdToken(body.idToken, env, fetchFn);
    } catch (err) {
      throw new HttpError(
        401,
        'GOOGLE_TOKEN_INVALID',
        err instanceof Error ? err.message : 'Google token verification failed.'
      );
    }

    const nowIso = new Date().toISOString();
    const deviceId = body.guestDeviceId.trim();

    const byGoogle = await env.DB.prepare('SELECT id FROM students WHERE google_uid = ? LIMIT 1')
      .bind(identity.googleUid)
      .first<{ id: string }>();
    const byGuest = await env.DB.prepare('SELECT id FROM students WHERE guest_device_id = ? LIMIT 1')
      .bind(deviceId)
      .first<{ id: string }>();

    let studentId: string;
    if (byGoogle) {
      studentId = byGoogle.id;
      // Merge all progress and learning records from temporary guest row into the Google-linked account
      if (byGuest && byGuest.id !== byGoogle.id) {
        const guestProg = await getStudentProgressRecord(env.DB, byGuest.id);
        const cloudProg = await getStudentProgressRecord(env.DB, byGoogle.id);
        const merged = mergeChapterProgress(
          cloudProg?.chapterProgress ?? [],
          guestProg?.chapterProgress ?? []
        );
        const mergeStatements: D1PreparedStatement[] = merged.map((item) =>
          env.DB.prepare(
            `INSERT INTO student_chapter_progress (
              student_id, chapter_id, subject_id, completion_percent,
              last_block_index, is_completed, last_accessed_at_iso, updated_at_iso
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(student_id, chapter_id) DO UPDATE SET
              completion_percent = excluded.completion_percent,
              last_block_index = excluded.last_block_index,
              is_completed = excluded.is_completed,
              last_accessed_at_iso = excluded.last_accessed_at_iso,
              updated_at_iso = excluded.updated_at_iso`
          ).bind(
            studentId,
            item.chapterId,
            item.subjectId ?? '',
            item.completionPercent,
            item.lastBlockIndex,
            item.isCompleted ? 1 : 0,
            item.lastAccessedAtIso,
            nowIso
          )
        );

        // Reassign quiz submissions, study tasks, and exam sessions
        mergeStatements.push(
          env.DB.prepare('UPDATE quiz_submissions SET student_id = ? WHERE student_id = ?').bind(
            studentId,
            byGuest.id
          ),
          env.DB.prepare('UPDATE student_study_tasks SET student_id = ? WHERE student_id = ?').bind(
            studentId,
            byGuest.id
          ),
          env.DB.prepare('UPDATE exam_sessions SET student_id = ? WHERE student_id = ?').bind(
            studentId,
            byGuest.id
          )
        );

        // Merge mistakes safely without duplicate unique constraint violations
        const guestMistakes = await env.DB.prepare(
          'SELECT * FROM student_mistakes WHERE student_id = ?'
        )
          .bind(byGuest.id)
          .all<{
            id: string;
            mcq_id: string;
            subject_id: string;
            chapter_id: string;
            topic_title: string;
            question: string;
            last_wrong_option_id: string | null;
            correct_option_id: string;
            wrong_count: number;
            mastered: number;
            last_attempted_at_iso: string;
          }>();

        for (const gm of guestMistakes.results) {
          mergeStatements.push(
            env.DB.prepare(
              `INSERT INTO student_mistakes (
                id, student_id, mcq_id, subject_id, chapter_id, topic_title,
                question, last_wrong_option_id, correct_option_id, wrong_count,
                mastered, last_attempted_at_iso
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              ON CONFLICT(student_id, mcq_id) DO UPDATE SET
                wrong_count = MAX(wrong_count, excluded.wrong_count),
                mastered = MAX(mastered, excluded.mastered),
                last_wrong_option_id = COALESCE(excluded.last_wrong_option_id, last_wrong_option_id),
                last_attempted_at_iso = MAX(last_attempted_at_iso, excluded.last_attempted_at_iso)`
            ).bind(
              `msk_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`,
              studentId,
              gm.mcq_id,
              gm.subject_id,
              gm.chapter_id,
              gm.topic_title,
              gm.question,
              gm.last_wrong_option_id,
              gm.correct_option_id,
              gm.wrong_count,
              gm.mastered,
              gm.last_attempted_at_iso
            )
          );
        }

        // Merge bookmarks cleanly
        const guestBookmarks = await env.DB.prepare(
          'SELECT * FROM student_bookmarks WHERE student_id = ?'
        )
          .bind(byGuest.id)
          .all<{
            item_type: string;
            item_id: string;
            chapter_id: string | null;
            subject_id: string | null;
            title: string;
            note: string;
            created_at_iso: string;
          }>();
        for (const gb of guestBookmarks.results) {
          mergeStatements.push(
            env.DB.prepare(
              `INSERT OR IGNORE INTO student_bookmarks (
                id, student_id, item_type, item_id, chapter_id, subject_id, title, note, created_at_iso
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
            ).bind(
              `bm_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`,
              studentId,
              gb.item_type,
              gb.item_id,
              gb.chapter_id,
              gb.subject_id,
              gb.title,
              gb.note,
              gb.created_at_iso
            )
          );
        }

        // Merge flashcard spaced-repetition state
        const guestReviews = await env.DB.prepare(
          'SELECT * FROM student_flashcard_reviews WHERE student_id = ?'
        )
          .bind(byGuest.id)
          .all<{
            flashcard_id: string;
            ease_factor: number;
            interval_days: number;
            repetitions: number;
            last_quality: number;
            next_review_at_iso: string;
            updated_at_iso: string;
          }>();
        for (const gr of guestReviews.results) {
          mergeStatements.push(
            env.DB.prepare(
              `INSERT INTO student_flashcard_reviews (
                student_id, flashcard_id, ease_factor, interval_days, repetitions, last_quality, next_review_at_iso, updated_at_iso
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
              ON CONFLICT(student_id, flashcard_id) DO UPDATE SET
                repetitions = MAX(repetitions, excluded.repetitions),
                interval_days = MAX(interval_days, excluded.interval_days),
                ease_factor = excluded.ease_factor,
                last_quality = excluded.last_quality,
                next_review_at_iso = excluded.next_review_at_iso,
                updated_at_iso = MAX(updated_at_iso, excluded.updated_at_iso)`
            ).bind(
              studentId,
              gr.flashcard_id,
              gr.ease_factor,
              gr.interval_days,
              gr.repetitions,
              gr.last_quality,
              gr.next_review_at_iso,
              gr.updated_at_iso
            )
          );
        }

        // Merge daily activity & achievements
        const guestDays = await env.DB.prepare(
          'SELECT day_ymd, minutes_logged, quizzes_completed, chapters_studied FROM student_activity_days WHERE student_id = ?'
        )
          .bind(byGuest.id)
          .all<{ day_ymd: string; minutes_logged: number; quizzes_completed: number; chapters_studied: number }>();
        for (const gd of guestDays.results) {
          mergeStatements.push(
            env.DB.prepare(
              `INSERT INTO student_activity_days (student_id, day_ymd, minutes_logged, quizzes_completed, chapters_studied)
               VALUES (?, ?, ?, ?, ?)
               ON CONFLICT(student_id, day_ymd) DO UPDATE SET
                 minutes_logged = minutes_logged + excluded.minutes_logged,
                 quizzes_completed = quizzes_completed + excluded.quizzes_completed,
                 chapters_studied = chapters_studied + excluded.chapters_studied`
            ).bind(studentId, gd.day_ymd, gd.minutes_logged, gd.quizzes_completed, gd.chapters_studied)
          );
        }

        const guestAchievements = await env.DB.prepare(
          'SELECT achievement_code, unlocked_at_iso FROM student_achievements WHERE student_id = ?'
        )
          .bind(byGuest.id)
          .all<{ achievement_code: string; unlocked_at_iso: string }>();
        for (const ga of guestAchievements.results) {
          mergeStatements.push(
            env.DB.prepare(
              'INSERT OR IGNORE INTO student_achievements (student_id, achievement_code, unlocked_at_iso) VALUES (?, ?, ?)'
            ).bind(studentId, ga.achievement_code, ga.unlocked_at_iso)
          );
        }

        const guestXp = guestProg?.xpTotal ?? 0;
        const guestHours = guestProg?.studyHoursTotal ?? 0;
        const guestDiag = guestProg?.diagnosticCompleted ? 1 : 0;
        if (guestXp > 0 || guestHours > 0 || guestDiag > 0) {
          mergeStatements.push(
            env.DB.prepare(
              `UPDATE students SET
                xp_total = xp_total + ?,
                study_hours_total = study_hours_total + ?,
                diagnostic_completed = MAX(diagnostic_completed, ?)
               WHERE id = ?`
            ).bind(guestXp, guestHours, guestDiag, studentId)
          );
        }

        if (mergeStatements.length > 0) {
          await env.DB.batch(mergeStatements);
        }
      }
      await env.DB.prepare(
        `UPDATE students SET email = ?, display_name = ?, avatar_url = ?, last_active_iso = ?, updated_at_iso = ? WHERE id = ?`
      )
        .bind(identity.email, identity.displayName, identity.avatarUrl, nowIso, nowIso, studentId)
        .run();
    } else if (byGuest) {
      // Link existing guest profile directly to Google UID
      studentId = byGuest.id;
      await env.DB.prepare(
        `UPDATE students SET google_uid = ?, email = ?, display_name = ?, avatar_url = ?, last_active_iso = ?, updated_at_iso = ? WHERE id = ?`
      )
        .bind(
          identity.googleUid,
          identity.email,
          identity.displayName,
          identity.avatarUrl,
          nowIso,
          nowIso,
          studentId
        )
        .run();
    } else {
      studentId = `stu_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
      const fallbackClass: ClassLevel = (body.selectedClass ?? 11) as ClassLevel;
      const fallbackGroup: StudyGroupCode =
        fallbackClass >= 11 ? 'PRE_MEDICAL' : 'SCIENCE_BIOLOGY';
      await env.DB.prepare(
        `INSERT INTO students (
          id, guest_device_id, google_uid, email, display_name, avatar_url,
          selected_class, selected_board, selected_sub_board, study_group,
          study_hours_total, daily_goal_percent, last_active_iso, created_at_iso, updated_at_iso
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PESHAWAR', ?, 0, 0, ?, ?, ?)`
      )
        .bind(
          studentId,
          deviceId,
          identity.googleUid,
          identity.email,
          identity.displayName,
          identity.avatarUrl,
          fallbackClass,
          body.selectedBoard ?? 'KPK',
          fallbackGroup,
          nowIso,
          nowIso,
          nowIso
        )
        .run();
    }

    const secret = env.ADMIN_SESSION_SECRET;
    if (!secret || secret.trim().length < 16) {
      throw new HttpError(
        503,
        'SESSION_SECRET_MISSING',
        'ADMIN_SESSION_SECRET must be configured on the Worker to mint signed sessions.'
      );
    }

    const signed = await signSessionToken(
      {
        sub: studentId,
        typ: 'student',
        role: 'student',
        email: identity.email,
        guestDeviceId: deviceId,
      },
      secret,
      60 * 60 * 24 * 30
    );

    const session: ApiAuthSession = {
      mode: 'google_authenticated',
      guestDeviceId: deviceId,
      token: signed.token,
      user: {
        id: studentId,
        displayName: identity.displayName,
        email: identity.email,
        avatarUrl: identity.avatarUrl,
        role: 'student',
      },
      accessTokenExpiresAtIso: signed.expiresAtIso,
    };
    return jsonEnvelope(session, env, 200, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  if (method === 'GET' && path === '/v1/auth/session') {
    const resolved = await resolveStudentFromRequest(request, env);
    const st = await env.DB.prepare(
      'SELECT id, display_name, email, avatar_url, google_uid, guest_device_id FROM students WHERE id = ?'
    )
      .bind(resolved.studentId)
      .first<{
        id: string;
        display_name: string;
        email: string | null;
        avatar_url: string | null;
        google_uid: string | null;
        guest_device_id: string;
      }>();

    const session: ApiAuthSession = {
      mode: st?.google_uid ? 'google_authenticated' : 'guest',
      guestDeviceId: st?.guest_device_id || resolved.guestDeviceId,
      user: st
        ? {
            id: st.id,
            displayName: st.display_name,
            email: st.email,
            avatarUrl: st.avatar_url,
            role: 'student',
          }
        : null,
      accessTokenExpiresAtIso: null,
    };
    return jsonEnvelope(session, env, 200, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  if (method === 'GET' && path === '/v1/students/me/progress') {
    const resolved = await resolveStudentFromRequest(request, env);
    const progress = await getStudentProgressRecord(env.DB, resolved.studentId);
    if (!progress) {
      throw new HttpError(404, 'STUDENT_NOT_FOUND', 'Student profile not found.');
    }
    return jsonEnvelope(progress, env, 200, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  if (method === 'POST' && path === '/v1/students/me/progress/sync') {
    const body = (await request.json().catch(() => null)) as StudentProgressSyncRequest | null;
    if (!body || !body.guestDeviceId?.trim() || !Array.isArray(body.chapterProgress)) {
      throw new HttpError(400, 'INVALID_SYNC_PAYLOAD', 'guestDeviceId and chapterProgress array are required.');
    }

    const reqWithDevice = new Request(request.url, {
      headers: new Headers([
        ...Array.from(request.headers.entries()),
        ...(request.headers.has('X-Guest-Device-Id')
          ? []
          : [['X-Guest-Device-Id', body.guestDeviceId.trim()] as [string, string]]),
      ]),
    });
    const resolved = await resolveStudentFromRequest(reqWithDevice, env);
    const current = await getStudentProgressRecord(env.DB, resolved.studentId);

    const mergedChapters = mergeChapterProgress(
      current?.chapterProgress ?? [],
      body.chapterProgress
    );
    const nowIso = new Date().toISOString();
    const mergedHours = Math.max(current?.studyHoursTotal ?? 0, Number(body.studyHoursTotal || 0));
    const mergedDailyGoal = Math.max(
      0,
      Math.min(100, Number(body.dailyGoalPercent ?? current?.dailyGoalPercent ?? 0))
    );

    const deltaHours = Math.max(0, mergedHours - (current?.studyHoursTotal ?? 0));
    const deltaMinutes = Math.round(deltaHours * 60);

    // Compare against existing chapter rows and only write changed chapters (eliminates redundant D1 writes)
    const existingByChapter = new Map(
      (current?.chapterProgress ?? []).map((item) => [item.chapterId, item])
    );
    const changedChapters = mergedChapters.filter((ch) => {
      const prev = existingByChapter.get(ch.chapterId);
      if (!prev) return true;
      return (
        prev.completionPercent !== ch.completionPercent ||
        prev.lastBlockIndex !== ch.lastBlockIndex ||
        prev.isCompleted !== ch.isCompleted ||
        (ch.subjectId && prev.subjectId !== ch.subjectId)
      );
    });

    const profileUnchanged =
      current &&
      current.selectedClass === (body.selectedClass || current.selectedClass) &&
      current.selectedBoard === (body.selectedBoard || current.selectedBoard) &&
      (body.selectedSubBoard === undefined || current.selectedSubBoard === body.selectedSubBoard) &&
      (body.studyGroup === undefined || current.studyGroup === body.studyGroup) &&
      (body.language === undefined || current.language === body.language) &&
      current.studyHoursTotal === mergedHours &&
      current.dailyGoalPercent === mergedDailyGoal &&
      (body.dailyTargetMinutes === undefined || current.dailyTargetMinutes === body.dailyTargetMinutes) &&
      (body.examCountdownDateIso === undefined || current.examCountdownDateIso === body.examCountdownDateIso) &&
      (!body.diagnosticCompleted || current.diagnosticCompleted) &&
      changedChapters.length === 0;

    if (!profileUnchanged) {
      await env.DB.prepare(
        `UPDATE students SET
          selected_class = ?,
          selected_board = ?,
          selected_sub_board = COALESCE(?, selected_sub_board),
          study_group = COALESCE(?, study_group),
          language = COALESCE(?, language),
          study_hours_total = ?,
          daily_goal_percent = ?,
          daily_target_minutes = COALESCE(?, daily_target_minutes),
          exam_countdown_date_iso = COALESCE(?, exam_countdown_date_iso),
          diagnostic_completed = MAX(diagnostic_completed, ?),
          last_active_iso = ?,
          updated_at_iso = ?
         WHERE id = ?`
      )
        .bind(
          body.selectedClass || current?.selectedClass || 11,
          body.selectedBoard || current?.selectedBoard || 'KPK',
          body.selectedSubBoard ?? null,
          body.studyGroup ?? null,
          body.language ?? null,
          mergedHours,
          mergedDailyGoal,
          body.dailyTargetMinutes ?? null,
          body.examCountdownDateIso ?? null,
          body.diagnosticCompleted ? 1 : 0,
          nowIso,
          nowIso,
          resolved.studentId
        )
        .run();
    }

    if (deltaMinutes > 0 || changedChapters.length > 0) {
      await recordStudentActivityDay(
        env,
        resolved.studentId,
        { minutes: deltaMinutes, chapters: changedChapters.length > 0 ? 1 : 0 },
        nowIso
      );
    }

    if (changedChapters.length > 0) {
      const statements: D1PreparedStatement[] = changedChapters.map((ch) =>
        env.DB.prepare(
          `INSERT INTO student_chapter_progress (
            student_id, chapter_id, subject_id, completion_percent,
            last_block_index, is_completed, last_accessed_at_iso, updated_at_iso
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT(student_id, chapter_id) DO UPDATE SET
            completion_percent = excluded.completion_percent,
            last_block_index = excluded.last_block_index,
            is_completed = excluded.is_completed,
            last_accessed_at_iso = excluded.last_accessed_at_iso,
            updated_at_iso = excluded.updated_at_iso`
        ).bind(
          resolved.studentId,
          ch.chapterId,
          ch.subjectId ?? '',
          ch.completionPercent,
          ch.lastBlockIndex,
          ch.isCompleted ? 1 : 0,
          ch.lastAccessedAtIso || nowIso,
          nowIso
        )
      );
      await env.DB.batch(statements);
    }

    const updated: ApiStudentProgress | null = await getStudentProgressRecord(
      env.DB,
      resolved.studentId
    );
    return jsonEnvelope(updated, env, 200, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  return null;
}

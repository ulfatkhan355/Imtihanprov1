import { validateAgentIngestPayload } from '../../../shared-contracts/rules';
import type {
  AgentIngestSubmissionRequest,
  AgentIngestSubmissionResponse,
} from '../../../shared-contracts/types';
import { sha256Hex, timingSafeEqualStrings } from '../crypto/auth';
import { appendAuditLog } from '../db/repository';
import { HttpError, jsonEnvelope, type Env } from '../env';

/**
 * Restricted AI Agent Content Ingestion Pipeline.
 * Security boundaries:
 * - Must authenticate via X-Agent-Secret matching env.AGENT_INGEST_SECRET.
 * - Cannot publish content directly (`status` is always `'queued'`).
 * - Cannot modify admin_users or settings.
 */
export async function handleAgentRoutes(
  request: Request,
  path: string,
  env: Env,
  headers: Record<string, string>
): Promise<Response | null> {
  const method = request.method.toUpperCase();
  if (method !== 'POST' || path !== '/v1/agent/submissions') {
    return null;
  }

  const expectedSecret = (env.AGENT_INGEST_SECRET || '').trim();
  if (!expectedSecret) {
    throw new HttpError(
      503,
      'AGENT_SECRET_NOT_CONFIGURED',
      'AGENT_INGEST_SECRET is not configured on the Worker.'
    );
  }

  const providedSecret = (request.headers.get('X-Agent-Secret') || '').trim();
  if (!providedSecret || !timingSafeEqualStrings(providedSecret, expectedSecret)) {
    throw new HttpError(403, 'INVALID_AGENT_SECRET', 'Invalid or missing X-Agent-Secret header.');
  }

  const body = (await request.json().catch(() => null)) as AgentIngestSubmissionRequest | null;
  if (!body) {
    throw new HttpError(400, 'INVALID_JSON', 'Request body must be valid JSON.');
  }

  const validation = validateAgentIngestPayload(body);
  if (!validation.valid) {
    throw new HttpError(
      422,
      'AGENT_SCHEMA_VALIDATION_FAILED',
      validation.errors.slice(0, 5).join(' ')
    );
  }

  const subject = await env.DB.prepare(
    'SELECT id, name, board_code, class_level FROM subjects WHERE id = ? LIMIT 1'
  )
    .bind(body.subjectId)
    .first<{ id: string; name: string; board_code: string; class_level: number }>();

  if (!subject) {
    throw new HttpError(404, 'SUBJECT_NOT_FOUND', `Referenced subjectId "${body.subjectId}" does not exist.`);
  }

  if (body.chapterId) {
    const ch = await env.DB.prepare('SELECT id FROM chapters WHERE id = ? LIMIT 1')
      .bind(body.chapterId)
      .first<{ id: string }>();
    if (!ch) {
      throw new HttpError(404, 'CHAPTER_NOT_FOUND', `Referenced chapterId "${body.chapterId}" does not exist.`);
    }
  }

  const payloadString = JSON.stringify({
    submissionType: body.submissionType,
    language: body.language || 'en',
    blocks: body.blocks ?? [],
    mcqs: body.mcqs ?? [],
    pdfMetadata: body.pdfMetadata ?? null,
  });
  const payloadSha256 = await sha256Hex(payloadString);

  const duplicate = await env.DB.prepare(
    'SELECT id FROM ai_agent_submissions WHERE payload_sha256 = ? LIMIT 1'
  )
    .bind(payloadSha256)
    .first<{ id: string }>();

  const warnings = [...validation.warnings];
  let duplicateDetected = false;
  if (duplicate) {
    duplicateDetected = true;
    warnings.push(`Identical content hash matches earlier submission ${duplicate.id}.`);
  }

  const submissionId = `aisub_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
  const nowIso = new Date().toISOString();

  await env.DB.prepare(
    `INSERT INTO ai_agent_submissions (
      id, agent_name, model_identifier, target_class, board_code,
      subject_id, subject_name, chapter_id, chapter_title, submission_type,
      payload_json, payload_sha256, blocks_count, schema_validation_passed,
      validation_warnings_json, status, reviewer_notes, submitted_at_iso, updated_at_iso
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'queued', '', ?, ?)`
  )
    .bind(
      submissionId,
      body.agentName.trim(),
      body.modelIdentifier.trim(),
      body.targetClass,
      body.boardCode,
      subject.id,
      subject.name,
      body.chapterId ?? null,
      body.chapterTitle.trim(),
      body.submissionType,
      payloadString,
      payloadSha256,
      validation.blocksCount,
      1,
      JSON.stringify(warnings),
      nowIso,
      nowIso
    )
    .run();

  await appendAuditLog(env.DB, {
    actorName: `AI Agent (${body.agentName.trim()})`,
    action: 'Submitted content draft to review queue',
    targetEntity: `${subject.name} — ${body.chapterTitle.trim()}`,
    details: { submissionId, blocksCount: validation.blocksCount, duplicateDetected },
  });

  const resp: AgentIngestSubmissionResponse = {
    submissionId,
    status: 'queued',
    blocksCount: validation.blocksCount,
    schemaValidationPassed: true,
    validationWarnings: warnings,
    duplicateDetected,
  };

  return jsonEnvelope(resp, env, 201, headers);
}

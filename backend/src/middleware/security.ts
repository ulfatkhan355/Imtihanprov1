import { HttpError, type Env } from '../env';

export function buildCorsHeaders(request: Request, env: Env): Record<string, string> {
  const origin = request.headers.get('Origin') || '';
  const allowedList = (env.ALLOWED_CORS_ORIGINS || 'http://localhost:3000,http://127.0.0.1:3000')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

  const isLocalOrigin =
    origin.startsWith('http://localhost:') ||
    origin.startsWith('http://127.0.0.1:') ||
    /^https:\/\/[a-z0-9-]+\.pages\.dev$/i.test(origin);

  const allowOrigin =
    origin && (allowedList.includes(origin) || isLocalOrigin)
      ? origin
      : allowedList[0] || 'http://localhost:3000';

  return {
    'Access-Control-Allow-Origin': allowOrigin,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Agent-Secret, X-Guest-Device-Id',
    'Access-Control-Expose-Headers': 'Content-Disposition, Content-Length, ETag',
    'Access-Control-Max-Age': '86400',
    Vary: 'Origin',
  };
}

export const SECURITY_HEADERS: Record<string, string> = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
};

export function getClientIp(request: Request): string {
  return (
    request.headers.get('CF-Connecting-IP') ||
    request.headers.get('X-Forwarded-For')?.split(',')[0]?.trim() ||
    'unknown-client'
  );
}

// Fast per-isolate rate limiter for public GET endpoints (0 D1 reads/writes on free tier).
const inMemoryRateBuckets = new Map<string, { count: number; windowStartMs: number }>();

export function enforceInMemoryRateLimit(
  bucketKey: string,
  maxRequests: number,
  windowMs = 60_000
): void {
  const now = Date.now();
  if (inMemoryRateBuckets.size > 2000) {
    for (const [key, entry] of inMemoryRateBuckets.entries()) {
      if (now - entry.windowStartMs >= windowMs) {
        inMemoryRateBuckets.delete(key);
      }
    }
  }

  const current = inMemoryRateBuckets.get(bucketKey);
  if (!current || now - current.windowStartMs >= windowMs) {
    inMemoryRateBuckets.set(bucketKey, { count: 1, windowStartMs: now });
    return;
  }

  if (current.count >= maxRequests) {
    throw new HttpError(429, 'RATE_LIMITED', 'Too many requests. Please wait before retrying.');
  }

  current.count += 1;
}

export function resetInMemoryRateLimits(): void {
  inMemoryRateBuckets.clear();
}

/**
 * Atomic single-query fixed-window rate limiter persisted in D1 `rate_limits` table
 * for mutating/auth/AI routes (replaces previous multi-query SELECT + INSERT/UPDATE).
 */
export async function enforceRateLimit(
  env: Env,
  bucketKey: string,
  maxRequests: number,
  windowMs = 60_000
): Promise<void> {
  const now = Date.now();
  const row = await env.DB.prepare(
    `INSERT INTO rate_limits (rate_key, count, window_start_ms)
     VALUES (?, 1, ?)
     ON CONFLICT(rate_key) DO UPDATE SET
       count = CASE
         WHEN excluded.window_start_ms - rate_limits.window_start_ms >= ? THEN 1
         ELSE rate_limits.count + 1
       END,
       window_start_ms = CASE
         WHEN excluded.window_start_ms - rate_limits.window_start_ms >= ? THEN excluded.window_start_ms
         ELSE rate_limits.window_start_ms
       END
     RETURNING count`
  )
    .bind(bucketKey, now, windowMs, windowMs)
    .first<{ count: number }>();

  if ((row?.count ?? 1) > maxRequests) {
    throw new HttpError(429, 'RATE_LIMITED', 'Too many requests. Please wait before retrying.');
  }

  // Opportunistic low-frequency cleanup of expired buckets (> 10 minutes stale) to bound table size.
  if (Math.random() < 0.02) {
    await env.DB.prepare('DELETE FROM rate_limits WHERE window_start_ms < ?')
      .bind(now - Math.max(windowMs * 5, 600_000))
      .run()
      .catch(() => undefined);
  }
}

export function sanitizeLogMeta(meta: Record<string, unknown>): string {
  const redacted: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(meta)) {
    const lower = k.toLowerCase();
    if (
      lower.includes('secret') ||
      lower.includes('password') ||
      lower.includes('token') ||
      lower.includes('key')
    ) {
      redacted[k] = '[REDACTED]';
    } else {
      redacted[k] = v;
    }
  }
  return JSON.stringify(redacted);
}

import type { ApiEnvelope } from '../../../shared-contracts/types';
import type { Env } from '../env';

/**
 * Edge & HTTP Cache Governance for Imtihan Pro
 * Guarantees:
 * - Public curriculum GETs emit ETag + public Cache-Control + 304 Not Modified support.
 * - Private/student/admin routes ALWAYS emit `Cache-Control: private, no-store` so caching
 *   never leaks private student or admin records across users.
 */

export const PUBLIC_CURRICULUM_CACHE_CONTROL =
  'public, max-age=300, s-maxage=3600, stale-while-revalidate=86400';

export const PRIVATE_NO_STORE_CACHE_CONTROL = 'private, no-store, max-age=0';

export async function computeWeakEtag(payloadText: string): Promise<string> {
  const bytes = new TextEncoder().encode(payloadText);
  const digest = await crypto.subtle.digest('SHA-256', bytes);
  const hex = Array.from(new Uint8Array(digest))
    .slice(0, 8)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
  return `W/"${hex}"`;
}

export function ifNoneMatchMatches(request: Request, etag: string): boolean {
  const header = request.headers.get('If-None-Match');
  if (!header) return false;
  return header
    .split(',')
    .map((s) => s.trim())
    .some((tag) => tag === '*' || tag === etag || tag.replace(/^W\//, '') === etag.replace(/^W\//, ''));
}

export async function tryMatchWorkerEdgeCache(request: Request): Promise<Response | null> {
  try {
    const cacheStorage = (globalThis as unknown as { caches?: { default?: Cache } }).caches;
    if (!cacheStorage?.default) return null;
    const hit = await cacheStorage.default.match(request);
    return hit ?? null;
  } catch {
    return null;
  }
}

export async function tryPutWorkerEdgeCache(request: Request, response: Response): Promise<void> {
  try {
    const cacheStorage = (globalThis as unknown as { caches?: { default?: Cache } }).caches;
    if (!cacheStorage?.default) return;
    if (request.method.toUpperCase() === 'GET' && response.status === 200) {
      await cacheStorage.default.put(request, response.clone());
    }
  } catch {
    // Edge Cache API unavailable in Node test environment; safe no-op.
  }
}

export async function buildCacheableJsonResponse<T>(
  request: Request,
  bodyObj: T,
  baseHeaders: Record<string, string>,
  ttlSeconds = 3600
): Promise<{ response: Response; notModified: boolean }> {
  const jsonText = JSON.stringify(bodyObj);
  const etag = await computeWeakEtag(jsonText);
  const cacheHeaders: Record<string, string> = {
    ...baseHeaders,
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': `public, max-age=300, s-maxage=${ttlSeconds}, stale-while-revalidate=86400`,
    ETag: etag,
  };

  if (ifNoneMatchMatches(request, etag)) {
    return {
      notModified: true,
      response: new Response(null, { status: 304, headers: cacheHeaders }),
    };
  }

  return {
    notModified: false,
    response: new Response(jsonText, { status: 200, headers: cacheHeaders }),
  };
}

export async function buildCacheableEnvelopeResponse<T>(
  request: Request,
  data: T,
  env: Env,
  baseHeaders: Record<string, string>,
  ttlSeconds = 3600
): Promise<{ response: Response; notModified: boolean }> {
  const dataJson = JSON.stringify(data);
  const etag = await computeWeakEtag(dataJson);
  const cacheHeaders: Record<string, string> = {
    ...baseHeaders,
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': `public, max-age=300, s-maxage=${ttlSeconds}, stale-while-revalidate=86400`,
    ETag: etag,
  };

  if (ifNoneMatchMatches(request, etag)) {
    return {
      notModified: true,
      response: new Response(null, { status: 304, headers: cacheHeaders }),
    };
  }

  const envelope: ApiEnvelope<T> = {
    ok: true,
    data,
    meta: {
      apiVersion: env.API_VERSION || '2.0.0',
      source: 'cloudflare_stage_2',
      timestampIso: new Date().toISOString(),
    },
  };

  return {
    notModified: false,
    response: new Response(JSON.stringify(envelope), { status: 200, headers: cacheHeaders }),
  };
}

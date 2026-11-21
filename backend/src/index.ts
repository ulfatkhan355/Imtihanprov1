import { HttpError, jsonError, type Env, type ExecutionContext } from './env';
import { PRIVATE_NO_STORE_CACHE_CONTROL } from './middleware/cache';
import {
  buildCorsHeaders,
  enforceInMemoryRateLimit,
  enforceRateLimit,
  getClientIp,
  SECURITY_HEADERS,
} from './middleware/security';
import { handleAdminRoutes } from './routes/admin';
import { handleAgentRoutes } from './routes/agent';
import { handleLearningRoutes } from './routes/learning';
import { handlePublicStudentRoutes } from './routes/publicStudent';
import { handleStudentAuthAndSyncRoutes } from './routes/studentAuthSync';
import { handleTutorRoutes } from './routes/tutor';
import {
  flushUsageTelemetryNow,
  recordUsageDelta,
  shouldFlushUsageBuffer,
} from './services/usageTelemetry';

export interface WorkerRouterOptions {
  fetchFn?: typeof fetch;
  skipRateLimit?: boolean;
  skipTelemetry?: boolean;
}

function isPrivateRoute(path: string): boolean {
  return (
    path.startsWith('/v1/admin') ||
    path.startsWith('/v1/students/me') ||
    path.startsWith('/v1/auth') ||
    path.startsWith('/v1/agent') ||
    path.startsWith('/v1/tutor')
  );
}

function estimateRouteD1Cost(method: string, path: string, response: Response): { reads: number; writes: number } {
  const isEdgeOr304 =
    response.status === 304 || response.headers.get('X-Imtihan-Cache') === 'EDGE-HIT';
  if (isEdgeOr304) {
    return { reads: 0, writes: 0 };
  }
  if (method === 'HEAD') {
    return { reads: 2, writes: 0 };
  }
  if (method === 'GET') {
    if (path.startsWith('/v1/search')) return { reads: 80, writes: 0 };
    if (path.startsWith('/v1/students/me/insights')) return { reads: 120, writes: 0 };
    if (path.startsWith('/v1/admin/overview')) return { reads: 250, writes: 0 };
    return { reads: 25, writes: 0 };
  }
  if (path === '/v1/quizzes/submissions') return { reads: 20, writes: 12 };
  if (path === '/v1/students/me/progress/sync') return { reads: 15, writes: 6 };
  if (path === '/v1/tutor/ask') return { reads: 15, writes: 3 };
  return { reads: 10, writes: 3 };
}

export async function handleWorkerRequest(
  request: Request,
  env: Env,
  ctx?: ExecutionContext,
  options: WorkerRouterOptions = {}
): Promise<Response> {
  const startMs = Date.now();
  const method = request.method.toUpperCase();
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, '') || '/';
  const corsHeaders = buildCorsHeaders(request, env);
  const baseHeaders = { ...SECURITY_HEADERS, ...corsHeaders };

  if (method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: baseHeaders });
  }

  let response: Response;
  try {
    if (!options.skipRateLimit) {
      const ip = getClientIp(request);
      if (path.startsWith('/v1/agent')) {
        await enforceRateLimit(env, `agent:${ip}`, 30, 60_000);
      } else if ((method === 'GET' || method === 'HEAD') && !isPrivateRoute(path)) {
        // Zero-D1-write per-isolate guard for public GET/HEAD
        enforceInMemoryRateLimit(`get:${ip}`, 240, 60_000);
      } else if (!path.startsWith('/v1/admin/login') && !path.startsWith('/v1/admin/bootstrap')) {
        await enforceRateLimit(env, `api:${ip}`, 150, 60_000);
      }
    }

    const fetchFn = options.fetchFn ?? fetch;

    const routed =
      (await handleAgentRoutes(request, path, env, baseHeaders)) ||
      (await handleAdminRoutes(request, path, env, baseHeaders, fetchFn)) ||
      (await handleTutorRoutes(request, path, env, baseHeaders, fetchFn)) ||
      (await handleLearningRoutes(request, path, url, env, baseHeaders)) ||
      (await handleStudentAuthAndSyncRoutes(request, path, env, baseHeaders, fetchFn)) ||
      (await handlePublicStudentRoutes(request, path, url, env, baseHeaders, fetchFn));

    response =
      routed ||
      jsonError(404, 'ROUTE_NOT_FOUND', `No endpoint matches ${request.method} ${path}`, env, baseHeaders);
  } catch (err) {
    if (err instanceof HttpError) {
      response = jsonError(err.status, err.code, err.message, env, baseHeaders);
    } else {
      const message = err instanceof Error ? err.message : 'Unexpected server error.';
      response = jsonError(500, 'INTERNAL_SERVER_ERROR', message, env, baseHeaders);
    }
  }

  // Enforce strict isolation headers on private routes so CDN/browsers never share private student or admin state
  if (isPrivateRoute(path) && response.headers.get('Cache-Control') !== PRIVATE_NO_STORE_CACHE_CONTROL) {
    const clonedHeaders = new Headers(response.headers);
    clonedHeaders.set('Cache-Control', PRIVATE_NO_STORE_CACHE_CONTROL);
    clonedHeaders.set('Pragma', 'no-cache');
    response = new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: clonedHeaders,
    });
  }

  if (!options.skipTelemetry) {
    const latencyMs = Math.max(0, Date.now() - startMs);
    const est = estimateRouteD1Cost(method, path, response);
    recordUsageDelta({
      workerRequests: 1,
      d1ReadsEst: est.reads,
      d1WritesEst: est.writes,
      errorCount: response.status >= 500 ? 1 : 0,
      latencyMs,
    });
    if (shouldFlushUsageBuffer()) {
      const flushPromise = flushUsageTelemetryNow(env.DB);
      if (ctx) {
        ctx.waitUntil(flushPromise);
      } else {
        await flushPromise;
      }
    }
  }

  return response;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    return handleWorkerRequest(request, env, ctx);
  },
};

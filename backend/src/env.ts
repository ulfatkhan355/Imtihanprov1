import type { ApiEnvelope } from '../../shared-contracts/types';

export interface D1Meta {
  duration?: number;
  changes?: number;
  last_row_id?: number;
}

export interface D1Result<T = Record<string, unknown>> {
  results: T[];
  success: boolean;
  meta: D1Meta;
}

export interface D1PreparedStatement {
  bind(...values: unknown[]): D1PreparedStatement;
  first<T = Record<string, unknown>>(colName?: string): Promise<T | null>;
  all<T = Record<string, unknown>>(): Promise<D1Result<T>>;
  run(): Promise<D1Result<Record<string, unknown>>>;
}

export interface D1Database {
  prepare(query: string): D1PreparedStatement;
  batch<T = Record<string, unknown>>(statements: D1PreparedStatement[]): Promise<D1Result<T>[]>;
}

export interface ExecutionContext {
  waitUntil(promise: Promise<unknown>): void;
  passThroughOnException(): void;
}

export interface Env {
  DB: D1Database;
  API_VERSION?: string;
  B2_BUCKET_NAME?: string;
  B2_BUCKET_ID?: string;
  B2_ENDPOINT?: string;
  B2_REGION?: string;
  B2_KEY_ID?: string;
  B2_APPLICATION_KEY?: string;
  ADMIN_SESSION_SECRET?: string;
  ADMIN_BOOTSTRAP_SECRET?: string;
  AGENT_INGEST_SECRET?: string;
  GOOGLE_OAUTH_CLIENT_ID?: string;
  ALLOWED_CORS_ORIGINS?: string;
  GEMINI_API_KEY?: string;
  AI_PROVIDER_API_KEY?: string;
  AI_PROVIDER_MODEL?: string;
  AI_PROVIDER_BASE_URL?: string;
}

export class HttpError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function jsonEnvelope<T>(
  data: T,
  env: Env,
  status = 200,
  extraHeaders: Record<string, string> = {}
): Response {
  const body: ApiEnvelope<T> = {
    ok: status >= 200 && status < 300,
    data,
    meta: {
      apiVersion: env.API_VERSION || '2.0.0',
      source: 'cloudflare_stage_2',
      timestampIso: new Date().toISOString(),
    },
  };
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders,
    },
  });
}

export function jsonError(
  status: number,
  code: string,
  message: string,
  env: Env,
  extraHeaders: Record<string, string> = {}
): Response {
  const body: ApiEnvelope<null> = {
    ok: false,
    data: null,
    meta: {
      apiVersion: env.API_VERSION || '2.0.0',
      source: 'cloudflare_stage_2',
      timestampIso: new Date().toISOString(),
    },
    error: { code, message },
  };
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders,
    },
  });
}

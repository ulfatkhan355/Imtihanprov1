import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync, type SQLInputValue } from 'node:sqlite';
import type { D1Database, D1PreparedStatement, D1Result, Env } from '../../backend/src/env';

class SqliteD1Statement implements D1PreparedStatement {
  private readonly db: DatabaseSync;
  private readonly sql: string;
  private readonly params: SQLInputValue[];

  constructor(db: DatabaseSync, sql: string, params: SQLInputValue[] = []) {
    this.db = db;
    this.sql = sql;
    this.params = params;
  }

  bind(...values: unknown[]): D1PreparedStatement {
    const normalized = values.map((v) => {
      if (v === undefined) return null;
      if (typeof v === 'boolean') return v ? 1 : 0;
      return v as SQLInputValue;
    });
    return new SqliteD1Statement(this.db, this.sql, normalized);
  }

  async first<T = Record<string, unknown>>(colName?: string): Promise<T | null> {
    const stmt = this.db.prepare(this.sql);
    const row = stmt.get(...this.params) as Record<string, unknown> | undefined;
    if (!row) return null;
    if (colName) return (row[colName] as T) ?? null;
    return row as T;
  }

  async all<T = Record<string, unknown>>(): Promise<D1Result<T>> {
    const stmt = this.db.prepare(this.sql);
    const rows = stmt.all(...this.params) as T[];
    return {
      results: rows,
      success: true,
      meta: { duration: 1, changes: 0 },
    };
  }

  async run(): Promise<D1Result<Record<string, unknown>>> {
    const stmt = this.db.prepare(this.sql);
    const info = stmt.run(...this.params);
    return {
      results: [],
      success: true,
      meta: {
        duration: 1,
        changes: Number(info.changes ?? 0),
        last_row_id: Number(info.lastInsertRowid ?? 0),
      },
    };
  }
}

export function createTestD1Env(overrides: Partial<Env> = {}): { db: DatabaseSync; env: Env } {
  const db = new DatabaseSync(':memory:');
  const migrationsDir = path.resolve(process.cwd(), 'backend/migrations');
  const migrationFiles = fs
    .readdirSync(migrationsDir)
    .filter((f) => f.endsWith('.sql'))
    .sort();
  for (const file of migrationFiles) {
    const sql = fs.readFileSync(path.join(migrationsDir, file), 'utf8');
    db.exec(sql);
  }

  const d1: D1Database = {
    prepare(query: string) {
      return new SqliteD1Statement(db, query);
    },
    async batch<T = Record<string, unknown>>(statements: D1PreparedStatement[]): Promise<D1Result<T>[]> {
      const results: D1Result<T>[] = [];
      for (const s of statements) {
        results.push((await s.run()) as unknown as D1Result<T>);
      }
      return results;
    },
  };

  const env: Env = {
    DB: d1,
    API_VERSION: '2.0.0',
    B2_BUCKET_NAME: 'imtihan-pro-assets',
    B2_BUCKET_ID: '1bdf9ec6cd50dbdfad180110',
    B2_ENDPOINT: 'https://s3.eu-central-003.backblazeb2.com',
    B2_REGION: 'eu-central-003',
    B2_KEY_ID: 'test-b2-key-id',
    B2_APPLICATION_KEY: 'test-b2-application-key-secret',
    ADMIN_SESSION_SECRET: 'test-session-secret-key-min-32-chars!!',
    ADMIN_BOOTSTRAP_SECRET: 'test-bootstrap-secret-1234',
    AGENT_INGEST_SECRET: 'test-agent-ingest-secret-5678',
    GOOGLE_OAUTH_CLIENT_ID: '1234567890-testclient.apps.googleusercontent.com',
    ALLOWED_CORS_ORIGINS: 'http://localhost:3000',
    ...overrides,
  };

  return { db, env };
}

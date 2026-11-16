import { sha256Hex, toHex } from '../crypto/auth';
import { HttpError, type Env } from '../env';

const textEncoder = new TextEncoder();

export interface B2Config {
  bucketName: string;
  bucketId: string;
  endpoint: string;
  region: string;
  keyId: string;
  applicationKey: string;
}

export function resolveB2Config(env: Env): B2Config {
  const bucketName = (env.B2_BUCKET_NAME || 'imtihan-pro-assets').trim();
  const bucketId = (env.B2_BUCKET_ID || '1bdf9ec6cd50dbdfad180110').trim();
  const endpoint = (env.B2_ENDPOINT || 'https://s3.eu-central-003.backblazeb2.com')
    .trim()
    .replace(/\/+$/, '');
  const region = (env.B2_REGION || 'eu-central-003').trim();
  const keyId = (env.B2_KEY_ID || '').trim();
  const applicationKey = (env.B2_APPLICATION_KEY || '').trim();

  return { bucketName, bucketId, endpoint, region, keyId, applicationKey };
}

export function isB2Configured(env: Env): boolean {
  const cfg = resolveB2Config(env);
  return cfg.keyId.length > 0 && cfg.applicationKey.length > 0 && cfg.bucketName.length > 0;
}

export function buildB2ObjectKey(
  boardCode: string,
  classLevel: number,
  subjectNameOrSlug: string,
  filename: string
): string {
  const safeBoard = boardCode.toLowerCase().replace(/[^a-z0-9_-]/g, '');
  const safeSubject = subjectNameOrSlug
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  const safeFile = filename
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, '-')
    .replace(/^-+/, '');
  return `${safeBoard}/class-${classLevel}/${safeSubject}/${safeFile}`;
}

export const MAX_PDF_UPLOAD_BYTES = 50 * 1024 * 1024; // 50 MB
export const ALLOWED_PDF_MIME_TYPES = new Set(['application/pdf']);

export function validatePdfUploadBytes(
  bytes: Uint8Array,
  mimeType: string,
  filename: string
): { valid: boolean; reason?: string } {
  if (!ALLOWED_PDF_MIME_TYPES.has(mimeType) && !filename.toLowerCase().endsWith('.pdf')) {
    return { valid: false, reason: 'Only application/pdf uploads are permitted in PDF Library.' };
  }
  if (bytes.byteLength === 0) {
    return { valid: false, reason: 'Uploaded PDF file is empty.' };
  }
  if (bytes.byteLength > MAX_PDF_UPLOAD_BYTES) {
    return { valid: false, reason: 'PDF exceeds maximum 50 MB upload size limit.' };
  }
  // Check PDF magic bytes: %PDF-
  const header = String.fromCharCode(...bytes.slice(0, 5));
  if (header !== '%PDF-') {
    return { valid: false, reason: 'File failed magic-header validation (expected %PDF-).' };
  }
  return { valid: true };
}

async function hmacSha256(keyBytes: Uint8Array | ArrayBuffer, data: string): Promise<Uint8Array> {
  const raw = keyBytes instanceof Uint8Array ? new Uint8Array(keyBytes).buffer : keyBytes;
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    raw,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', cryptoKey, textEncoder.encode(data));
  return new Uint8Array(sig);
}

async function deriveSigningKey(
  secretAccessKey: string,
  dateStamp: string,
  region: string,
  service: string
): Promise<Uint8Array> {
  const kDate = await hmacSha256(textEncoder.encode(`AWS4${secretAccessKey}`), dateStamp);
  const kRegion = await hmacSha256(kDate, region);
  const kService = await hmacSha256(kRegion, service);
  return hmacSha256(kService, 'aws4_request');
}

function formatAmzDate(now: Date): { amzDate: string; dateStamp: string } {
  const iso = now.toISOString().replace(/[:-]|\.\d{3}/g, '');
  return {
    amzDate: iso,
    dateStamp: iso.slice(0, 8),
  };
}

function encodeS3Path(key: string): string {
  return key
    .split('/')
    .map((seg) => encodeURIComponent(seg))
    .join('/');
}

export interface SignedS3Request {
  url: string;
  headers: Record<string, string>;
}

/**
 * Generates AWS Signature Version 4 headers for Backblaze B2 S3-compatible requests.
 */
export async function signB2Request(params: {
  method: 'GET' | 'PUT' | 'DELETE' | 'HEAD';
  objectKey: string;
  cfg: B2Config;
  payloadSha256Hex: string;
  contentType?: string;
  now?: Date;
}): Promise<SignedS3Request> {
  const { method, objectKey, cfg, payloadSha256Hex, contentType, now = new Date() } = params;
  const endpointUrl = new URL(cfg.endpoint);
  const host = endpointUrl.host;
  const { amzDate, dateStamp } = formatAmzDate(now);

  const cleanKey = objectKey.replace(/^\/+/, '');
  const canonicalUri = cleanKey
    ? `/${encodeURIComponent(cfg.bucketName)}/${encodeS3Path(cleanKey)}`
    : `/${encodeURIComponent(cfg.bucketName)}`;

  const headerEntries: Array<[string, string]> = [
    ['host', host],
    ['x-amz-content-sha256', payloadSha256Hex],
    ['x-amz-date', amzDate],
  ];
  if (contentType) {
    headerEntries.push(['content-type', contentType]);
  }
  headerEntries.sort((a, b) => a[0].localeCompare(b[0]));

  const canonicalHeaders = headerEntries.map(([k, v]) => `${k}:${v}\n`).join('');
  const signedHeaders = headerEntries.map(([k]) => k).join(';');

  const canonicalRequest = [
    method,
    canonicalUri,
    '',
    canonicalHeaders,
    signedHeaders,
    payloadSha256Hex,
  ].join('\n');

  const credentialScope = `${dateStamp}/${cfg.region}/s3/aws4_request`;
  const canonicalReqHash = await sha256Hex(canonicalRequest);
  const stringToSign = ['AWS4-HMAC-SHA256', amzDate, credentialScope, canonicalReqHash].join('\n');

  const signingKey = await deriveSigningKey(cfg.applicationKey, dateStamp, cfg.region, 's3');
  const signatureHex = toHex(await hmacSha256(signingKey, stringToSign));

  const authorization =
    `AWS4-HMAC-SHA256 Credential=${cfg.keyId}/${credentialScope}, ` +
    `SignedHeaders=${signedHeaders}, Signature=${signatureHex}`;

  const headers: Record<string, string> = {
    Authorization: authorization,
    'x-amz-date': amzDate,
    'x-amz-content-sha256': payloadSha256Hex,
  };
  if (contentType) {
    headers['Content-Type'] = contentType;
  }

  return {
    url: `${cfg.endpoint}${canonicalUri}`,
    headers,
  };
}

/**
 * Generates an AWS SigV4 pre-signed GET URL for direct Backblaze B2 download.
 */
export async function generateB2PresignedGetUrl(
  objectKey: string,
  env: Env,
  expiresInSeconds = 900,
  now = new Date()
): Promise<string | null> {
  if (!isB2Configured(env)) return null;
  const cfg = resolveB2Config(env);
  const endpointUrl = new URL(cfg.endpoint);
  const host = endpointUrl.host;
  const { amzDate, dateStamp } = formatAmzDate(now);
  const credentialScope = `${dateStamp}/${cfg.region}/s3/aws4_request`;

  const cleanKey = objectKey.replace(/^\/+/, '');
  const canonicalUri = `/${encodeURIComponent(cfg.bucketName)}/${encodeS3Path(cleanKey)}`;

  const queryParams: Array<[string, string]> = [
    ['X-Amz-Algorithm', 'AWS4-HMAC-SHA256'],
    ['X-Amz-Credential', `${cfg.keyId}/${credentialScope}`],
    ['X-Amz-Date', amzDate],
    ['X-Amz-Expires', String(Math.max(60, Math.min(86400, expiresInSeconds)))],
    ['X-Amz-SignedHeaders', 'host'],
  ];
  queryParams.sort((a, b) => a[0].localeCompare(b[0]));

  const canonicalQuery = queryParams
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');

  const canonicalRequest = [
    'GET',
    canonicalUri,
    canonicalQuery,
    `host:${host}\n`,
    'host',
    'UNSIGNED-PAYLOAD',
  ].join('\n');

  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    credentialScope,
    await sha256Hex(canonicalRequest),
  ].join('\n');

  const signingKey = await deriveSigningKey(cfg.applicationKey, dateStamp, cfg.region, 's3');
  const signatureHex = toHex(await hmacSha256(signingKey, stringToSign));

  return `${cfg.endpoint}${canonicalUri}?${canonicalQuery}&X-Amz-Signature=${signatureHex}`;
}

export class BackblazeB2Client {
  private readonly env: Env;
  private readonly fetchFn: typeof fetch;

  constructor(env: Env, fetchFn: typeof fetch = fetch) {
    this.env = env;
    this.fetchFn = fetchFn;
  }

  private requireConfigured(): B2Config {
    if (!isB2Configured(this.env)) {
      throw new HttpError(
        503,
        'B2_NOT_CONFIGURED',
        'Backblaze B2 credentials (B2_KEY_ID / B2_APPLICATION_KEY) are not configured on the Worker.'
      );
    }
    return resolveB2Config(this.env);
  }

  async putObject(
    objectKey: string,
    bytes: Uint8Array,
    contentType = 'application/pdf'
  ): Promise<{ objectKey: string; checksumSha256: string; fileSizeBytes: number }> {
    const cfg = this.requireConfigured();
    const checksumSha256 = await sha256Hex(bytes);
    const signed = await signB2Request({
      method: 'PUT',
      objectKey,
      cfg,
      payloadSha256Hex: checksumSha256,
      contentType,
    });

    const copy = new Uint8Array(bytes.byteLength);
    copy.set(bytes);
    const res = await this.fetchFn(signed.url, {
      method: 'PUT',
      headers: signed.headers,
      body: copy.buffer,
    });
    if (!res.ok) {
      const detail = await res.text().catch(() => '');
      throw new HttpError(
        502,
        'B2_UPLOAD_FAILED',
        `Backblaze B2 upload failed with HTTP ${res.status}: ${detail.slice(0, 160)}`
      );
    }

    return {
      objectKey,
      checksumSha256,
      fileSizeBytes: bytes.byteLength,
    };
  }

  async getObject(objectKey: string): Promise<Response> {
    const cfg = this.requireConfigured();
    const emptyHash = await sha256Hex('');
    const signed = await signB2Request({
      method: 'GET',
      objectKey,
      cfg,
      payloadSha256Hex: emptyHash,
    });

    const res = await this.fetchFn(signed.url, {
      method: 'GET',
      headers: signed.headers,
    });
    if (res.status === 404) {
      throw new HttpError(
        404,
        'PDF_OBJECT_NOT_IN_B2',
        `Object "${objectKey}" was not found in Backblaze B2 bucket "${cfg.bucketName}". Upload the PDF binary first.`
      );
    }
    if (!res.ok) {
      throw new HttpError(502, 'B2_DOWNLOAD_FAILED', `Backblaze B2 returned HTTP ${res.status}.`);
    }
    return res;
  }

  async deleteObject(objectKey: string): Promise<void> {
    const cfg = this.requireConfigured();
    const emptyHash = await sha256Hex('');
    const signed = await signB2Request({
      method: 'DELETE',
      objectKey,
      cfg,
      payloadSha256Hex: emptyHash,
    });

    const res = await this.fetchFn(signed.url, {
      method: 'DELETE',
      headers: signed.headers,
    });
    if (!res.ok && res.status !== 404 && res.status !== 204) {
      throw new HttpError(502, 'B2_DELETE_FAILED', `Backblaze B2 delete failed (HTTP ${res.status}).`);
    }
  }

  async checkBucketHealth(): Promise<{ reachable: boolean; message: string }> {
    if (!isB2Configured(this.env)) {
      return {
        reachable: false,
        message: 'B2_KEY_ID / B2_APPLICATION_KEY not configured yet.',
      };
    }
    const cfg = resolveB2Config(this.env);
    try {
      const emptyHash = await sha256Hex('');
      const signed = await signB2Request({
        method: 'HEAD',
        objectKey: '',
        cfg,
        payloadSha256Hex: emptyHash,
      });
      const res = await this.fetchFn(signed.url, {
        method: 'HEAD',
        headers: signed.headers,
      });
      if (res.ok || res.status === 200) {
        return { reachable: true, message: `Connected to B2 bucket ${cfg.bucketName}` };
      }
      return { reachable: false, message: `B2 HEAD returned HTTP ${res.status}` };
    } catch (err) {
      return {
        reachable: false,
        message: err instanceof Error ? err.message : 'Unable to reach Backblaze B2 endpoint.',
      };
    }
  }
}

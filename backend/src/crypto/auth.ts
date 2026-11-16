import type { Env } from '../env';

const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();

export function toBase64Url(bytes: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

export function fromBase64Url(input: string): Uint8Array {
  const padded = input.replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((input.length + 3) % 4);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

export function toHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

export async function sha256Hex(data: Uint8Array | string): Promise<string> {
  const bytes = typeof data === 'string' ? textEncoder.encode(data) : data;
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
  const digest = await crypto.subtle.digest('SHA-256', copy.buffer);
  return toHex(new Uint8Array(digest));
}

export function timingSafeEqualStrings(a: string, b: string): boolean {
  const bufA = textEncoder.encode(a);
  const bufB = textEncoder.encode(b);
  if (bufA.length !== bufB.length) return false;
  let diff = 0;
  for (let i = 0; i < bufA.length; i++) {
    diff |= bufA[i] ^ bufB[i];
  }
  return diff === 0;
}

export async function hashPassword(password: string, existingSaltHex?: string): Promise<{
  hashHex: string;
  saltHex: string;
}> {
  const saltBytes = existingSaltHex
    ? hexToBytes(existingSaltHex)
    : crypto.getRandomValues(new Uint8Array(16));
  const saltHex = toHex(saltBytes);
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    textEncoder.encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const saltCopy = new Uint8Array(saltBytes.byteLength);
  saltCopy.set(saltBytes);
  const derived = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: saltCopy.buffer,
      iterations: 100_000,
      hash: 'SHA-256',
    },
    keyMaterial,
    256
  );
  return { hashHex: toHex(new Uint8Array(derived)), saltHex };
}

export async function verifyPassword(
  password: string,
  storedHashHex: string,
  storedSaltHex: string
): Promise<boolean> {
  const { hashHex } = await hashPassword(password, storedSaltHex);
  return timingSafeEqualStrings(hashHex, storedHashHex);
}

function hexToBytes(hex: string): Uint8Array {
  const out = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) {
    out[i] = Number.parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return out;
}

export interface SessionTokenPayload {
  sub: string;
  typ: 'admin' | 'student';
  role: 'student' | 'reviewer' | 'admin' | 'super_admin';
  email?: string | null;
  guestDeviceId?: string;
  iat: number;
  exp: number;
}

async function importHmacKey(secret: string): Promise<CryptoKey> {
  return crypto.subtle.importKey(
    'raw',
    textEncoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign', 'verify']
  );
}

export async function signSessionToken(
  payload: Omit<SessionTokenPayload, 'iat' | 'exp'>,
  secret: string,
  ttlSeconds: number
): Promise<{ token: string; expiresAtIso: string }> {
  if (!secret || secret.trim().length < 16) {
    throw new Error('ADMIN_SESSION_SECRET must be configured (>=16 chars).');
  }
  const nowSec = Math.floor(Date.now() / 1000);
  const fullPayload: SessionTokenPayload = {
    ...payload,
    iat: nowSec,
    exp: nowSec + ttlSeconds,
  };
  const headerPart = toBase64Url(textEncoder.encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' })));
  const payloadPart = toBase64Url(textEncoder.encode(JSON.stringify(fullPayload)));
  const signingInput = `${headerPart}.${payloadPart}`;
  const key = await importHmacKey(secret);
  const signature = await crypto.subtle.sign('HMAC', key, textEncoder.encode(signingInput));
  const sigPart = toBase64Url(new Uint8Array(signature));
  return {
    token: `${signingInput}.${sigPart}`,
    expiresAtIso: new Date(fullPayload.exp * 1000).toISOString(),
  };
}

export async function verifySessionToken(
  token: string,
  secret: string
): Promise<SessionTokenPayload | null> {
  if (!secret || !token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [headerPart, payloadPart, sigPart] = parts;
  try {
    const key = await importHmacKey(secret);
    const sigBytes = fromBase64Url(sigPart);
    const sigCopy = new Uint8Array(sigBytes.byteLength);
    sigCopy.set(sigBytes);
    const valid = await crypto.subtle.verify(
      'HMAC',
      key,
      sigCopy.buffer,
      textEncoder.encode(`${headerPart}.${payloadPart}`)
    );
    if (!valid) return null;
    const payloadJson = textDecoder.decode(fromBase64Url(payloadPart));
    const parsed = JSON.parse(payloadJson) as SessionTokenPayload;
    const nowSec = Math.floor(Date.now() / 1000);
    if (!parsed.exp || parsed.exp <= nowSec) return null;
    return parsed;
  } catch {
    return null;
  }
}

export interface VerifiedGoogleIdentity {
  googleUid: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

/**
 * Verifies a Google Sign-In ID token via Google's tokeninfo endpoint when configured.
 * Strictly validates audience (`GOOGLE_OAUTH_CLIENT_ID`) when set, and rejects invalid/expired tokens.
 */
export async function verifyGoogleIdToken(
  idToken: string,
  env: Env,
  fetchFn: typeof fetch = fetch
): Promise<VerifiedGoogleIdentity> {
  if (!idToken || idToken.trim().length < 20) {
    throw new Error('Missing or malformed Google ID token.');
  }
  if (!env.GOOGLE_OAUTH_CLIENT_ID || !env.GOOGLE_OAUTH_CLIENT_ID.trim()) {
    throw new Error(
      'Google Sign-In verification is not configured on this server (missing GOOGLE_OAUTH_CLIENT_ID).'
    );
  }

  const res = await fetchFn(
    `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken.trim())}`
  );
  if (!res.ok) {
    throw new Error('Google ID token verification failed.');
  }
  const info = (await res.json()) as {
    sub?: string;
    aud?: string;
    email?: string;
    email_verified?: string | boolean;
    name?: string;
    picture?: string;
    exp?: string;
  };

  if (!info.sub || !info.email) {
    throw new Error('Google token missing subject or email.');
  }
  if (info.email_verified !== true && info.email_verified !== 'true') {
    throw new Error('Google account email is not verified.');
  }
  if (info.aud !== env.GOOGLE_OAUTH_CLIENT_ID.trim()) {
    throw new Error('Google token audience does not match configured GOOGLE_OAUTH_CLIENT_ID.');
  }
  const expSec = Number.parseInt(info.exp || '0', 10);
  if (expSec > 0 && expSec <= Math.floor(Date.now() / 1000)) {
    throw new Error('Google ID token has expired.');
  }

  return {
    googleUid: info.sub,
    email: info.email,
    displayName: info.name?.trim() || info.email.split('@')[0],
    avatarUrl: info.picture || null,
  };
}

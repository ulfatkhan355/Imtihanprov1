export type ParsedByteRange =
  | { kind: 'valid'; start: number; end: number; length: number; header: string }
  | { kind: 'invalid' }
  | { kind: 'unsatisfiable' };

/**
 * Parses a single HTTP Range header (`bytes=start-end`, `bytes=start-`, or `bytes=-suffix`)
 * against a known resource total byte length. Multi-range requests are rejected as unsatisfiable/invalid
 * so mobile 2G/3G chunked clients always get predictable single-part 206 or 416 responses.
 */
export function parseByteRangeHeader(rangeHeader: string, totalBytes: number): ParsedByteRange {
  if (!Number.isFinite(totalBytes) || totalBytes <= 0) {
    return { kind: 'unsatisfiable' };
  }

  const trimmed = rangeHeader.trim();
  const match = /^bytes\s*=\s*(.+)$/i.exec(trimmed);
  if (!match) {
    return { kind: 'invalid' };
  }

  const spec = match[1].trim();
  if (!spec || spec.includes(',')) {
    return { kind: 'invalid' };
  }

  const rangeParts = /^(\d*)-(\d*)$/.exec(spec);
  if (!rangeParts) {
    return { kind: 'invalid' };
  }

  const [, startStr, endStr] = rangeParts;
  if (!startStr && !endStr) {
    return { kind: 'invalid' };
  }

  if (!startStr && endStr) {
    const suffixLength = Number.parseInt(endStr, 10);
    if (!Number.isFinite(suffixLength) || suffixLength <= 0) {
      return { kind: 'unsatisfiable' };
    }
    const start = Math.max(0, totalBytes - suffixLength);
    const end = totalBytes - 1;
    return {
      kind: 'valid',
      start,
      end,
      length: end - start + 1,
      header: `bytes=${start}-${end}`,
    };
  }

  const start = Number.parseInt(startStr, 10);
  if (!Number.isFinite(start) || start < 0 || start >= totalBytes) {
    return { kind: 'unsatisfiable' };
  }

  let end = totalBytes - 1;
  if (endStr) {
    const parsedEnd = Number.parseInt(endStr, 10);
    if (!Number.isFinite(parsedEnd) || parsedEnd < start) {
      return { kind: 'invalid' };
    }
    end = Math.min(parsedEnd, totalBytes - 1);
  }

  return {
    kind: 'valid',
    start,
    end,
    length: end - start + 1,
    header: `bytes=${start}-${end}`,
  };
}

/**
 * Evaluates HTTP `If-Range` against a PDF's ETag using strict strong comparison (RFC 9110 §13.1.5).
 * Weak ETags (`W/"..."`) never satisfy strong `If-Range` comparison and safely force a full 200.
 */
export function shouldHonorRangeWithIfRange(ifRangeHeader: string | null, pdfEtag: string): boolean {
  if (!ifRangeHeader || !ifRangeHeader.trim()) {
    return true;
  }
  const candidate = ifRangeHeader.trim();
  if (candidate.startsWith('W/') || pdfEtag.startsWith('W/')) {
    return false;
  }
  return candidate === pdfEtag;
}

export function isValidSha256Hex(value: string | null | undefined): boolean {
  return Boolean(value && /^[0-9a-f]{64}$/i.test(value.trim()));
}

export function buildStrongOrWeakPdfEtag(pdfId: string, version: string, checksumSha256?: string | null): string {
  const cleanSha = (checksumSha256 || '').trim().toLowerCase();
  if (isValidSha256Hex(cleanSha)) {
    return `"${cleanSha}"`;
  }
  return `W/"${pdfId}-${version}"`;
}

export function secondsUntilUtcMidnight(now = new Date()): number {
  const nextMidnight = Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate() + 1,
    0,
    0,
    0,
    0
  );
  return Math.max(60, Math.ceil((nextMidnight - now.getTime()) / 1000));
}

/**
 * Wraps an upstream ReadableStream so that:
 * 1. `onFirstByte` fires strictly when the first non-empty byte chunk is transmitted.
 * 2. `onFinalize(bytesServed, firstByteSeen)` fires once on either natural EOF OR downstream abort/cancel,
 *    guaranteeing Backblaze B2 daily egress quota is billed ONLY for bytes actually served.
 */
export function createMeteredPdfStream(
  upstreamBody: ReadableStream<Uint8Array>,
  callbacks: {
    onFirstByte?: () => Promise<void> | void;
    onFinalize: (bytesServed: number, firstByteSeen: boolean) => void;
  }
): ReadableStream<Uint8Array> {
  const reader = upstreamBody.getReader();
  let bytesServed = 0;
  let firstByteSeen = false;
  let finalized = false;

  const finalizeOnce = () => {
    if (finalized) return;
    finalized = true;
    callbacks.onFinalize(bytesServed, firstByteSeen);
  };

  return new ReadableStream<Uint8Array>({
    async pull(controller) {
      try {
        const { done, value } = await reader.read();
        if (done) {
          finalizeOnce();
          controller.close();
          return;
        }
        if (value && value.byteLength > 0) {
          if (!firstByteSeen) {
            firstByteSeen = true;
            if (callbacks.onFirstByte) {
              await Promise.resolve(callbacks.onFirstByte()).catch(() => undefined);
            }
          }
          bytesServed += value.byteLength;
          controller.enqueue(value);
        }
      } catch (err) {
        finalizeOnce();
        controller.error(err);
      }
    },
    async cancel(reason) {
      try {
        await reader.cancel(reason);
      } catch {
        // Ignore upstream reader cancel failures
      } finally {
        finalizeOnce();
      }
    },
  });
}

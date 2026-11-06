

const VERIFIER_BYTES = 48;
const STATE_BYTES = 24;

export interface PkcePair {
  readonly verifier: string;
  readonly challenge: string;
  readonly method: 'S256';
}

export interface PkceRecord {
  readonly verifier: string;
  readonly state: string;
  readonly returnUrl: string;
  readonly createdAt: number;
}

const PKCE_STORAGE_KEY = 'bankops.oidc.pkce';
let memoryRecord: PkceRecord | null = null;

export function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function randomUrlSafeString(byteLength = 32): string {
  if (!globalThis.crypto?.getRandomValues) {
    throw new Error('Web Crypto is not available in this browser.');
  }
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

export async function createPkcePair(): Promise<PkcePair> {
  const verifier = randomUrlSafeString(VERIFIER_BYTES);
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return {
    verifier,
    challenge: base64UrlEncode(new Uint8Array(digest)),
    method: 'S256',
  };
}

export function createOidcState(): string {
  return randomUrlSafeString(STATE_BYTES);
}

/**
 * Restricts post-login navigation to same-origin application paths.
 * Prevents the callback from being used as an open redirect.
 */
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || typeof value !== 'string') {
    return '/';
  }
  const trimmed = value.trim();
  if (
    !trimmed.startsWith('/') ||
    trimmed.startsWith('//') ||
    trimmed.startsWith('/auth/callback') ||
    /[\r\n\\]/.test(trimmed)
  ) {
    return '/';
  }
  return trimmed;
}

/**
 * The PKCE record holds the short-lived code verifier, the OIDC state and the
 * attempted URL. It is NOT a token and it is removed as soon as the callback
 * consumes it. `sessionStorage` is needed because the OIDC redirect performs a
 * full page navigation and in-memory state would otherwise be lost.
 */
export function savePkceRecord(record: PkceRecord): void {
  memoryRecord = record;
  try {
    sessionStorage.setItem(PKCE_STORAGE_KEY, JSON.stringify(record));
  } catch {
    // Storage can be unavailable. The in-memory copy still works when the IdP
    // redirects in the same document, and the callback reports a clear error
    // when it does not.
  }
}

export function readPkceRecord(): PkceRecord | null {
  if (memoryRecord) {
    return memoryRecord;
  }
  try {
    const raw = sessionStorage.getItem(PKCE_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed: unknown = JSON.parse(raw);
    if (!isPkceRecord(parsed)) {
      clearPkceRecord();
      return null;
    }
    memoryRecord = parsed;
    return parsed;
  } catch {
    return null;
  }
}

export function clearPkceRecord(): void {
  memoryRecord = null;
  try {
    sessionStorage.removeItem(PKCE_STORAGE_KEY);
  } catch {
    // Ignore unavailable storage.
  }
}

function isPkceRecord(value: unknown): value is PkceRecord {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record['verifier'] === 'string' &&
    record['verifier'].length >= 43 &&
    typeof record['state'] === 'string' &&
    record['state'].length > 0 &&
    typeof record['returnUrl'] === 'string' &&
    typeof record['createdAt'] === 'number'
  );
}

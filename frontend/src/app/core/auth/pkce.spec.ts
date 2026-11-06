import { base64UrlEncode, createPkcePair, randomUrlSafeString, safeReturnUrl } from './pkce';

describe('PKCE helpers', () => {
  it('creates a high-entropy verifier and a matching S256 challenge', async () => {
    const pair = await createPkcePair();

    expect(pair.method).toBe('S256');
    expect(pair.verifier.length).toBeGreaterThanOrEqual(43);
    expect(pair.verifier).toMatch(/^[A-Za-z0-9_-]+$/);
    expect(pair.challenge).toMatch(/^[A-Za-z0-9_-]{43}$/);

    const digest = await crypto.subtle.digest(
      'SHA-256',
      new TextEncoder().encode(pair.verifier),
    );
    expect(pair.challenge).toBe(base64UrlEncode(new Uint8Array(digest)));
  });

  it('generates different verifiers and state values', async () => {
    const first = await createPkcePair();
    const second = await createPkcePair();

    expect(first.verifier).not.toBe(second.verifier);
    expect(randomUrlSafeString()).not.toBe(randomUrlSafeString());
  });

  it('only allows same-origin return paths', () => {
    expect(safeReturnUrl('/admin/users?page=2')).toBe('/admin/users?page=2');
    expect(safeReturnUrl('https://evil.example')).toBe('/');
    expect(safeReturnUrl('//evil.example')).toBe('/');
    expect(safeReturnUrl('/auth/callback?code=x')).toBe('/');
    expect(safeReturnUrl(null)).toBe('/');
  });
});

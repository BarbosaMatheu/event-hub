import { describe, expect, it } from 'vitest';
import { decodeJwtPayload, isTokenExpired, tokenExpiryMs } from './token';

function makeToken(payload: object): string {
  const encode = (value: object) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(payload)}.signature`;
}

describe('decodeJwtPayload', () => {
  it('reads the payload', () => {
    const payload = decodeJwtPayload(makeToken({ sub: 'ann', role: 'ADMIN' }));
    expect(payload).toEqual({ sub: 'ann', role: 'ADMIN' });
  });

  it('returns null for garbage', () => {
    expect(decodeJwtPayload('not-a-token')).toBeNull();
    expect(decodeJwtPayload('a.b.c')).toBeNull();
  });
});

describe('token expiry', () => {
  const now = 1_800_000_000_000;

  it('converts exp (seconds) to milliseconds', () => {
    expect(tokenExpiryMs(makeToken({ exp: 1_800_000_100 }))).toBe(1_800_000_100_000);
  });

  it('detects expired and valid tokens', () => {
    expect(isTokenExpired(makeToken({ exp: 1_799_999_000 }), now)).toBe(true);
    expect(isTokenExpired(makeToken({ exp: 1_800_001_000 }), now)).toBe(false);
  });

  it('treats tokens without a readable exp as expired', () => {
    expect(isTokenExpired(makeToken({ sub: 'ann' }), now)).toBe(true);
    expect(isTokenExpired('garbage', now)).toBe(true);
  });
});

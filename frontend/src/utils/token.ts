/** Reads the payload of a JWT without verifying it (verification is the server's job). */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    const payload: unknown = JSON.parse(json);
    return typeof payload === 'object' && payload !== null ? (payload as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

/** Expiry time in epoch milliseconds, or null if the token has no readable "exp". */
export function tokenExpiryMs(token: string): number | null {
  const exp = decodeJwtPayload(token)?.exp;
  return typeof exp === 'number' ? exp * 1000 : null;
}

export function isTokenExpired(token: string, now: number = Date.now()): boolean {
  const expiry = tokenExpiryMs(token);
  return expiry === null || expiry <= now;
}

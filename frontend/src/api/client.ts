import type { FieldViolation } from './types';

const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1';

/** Any failed API call. status 0 means the server could not be reached at all. */
export class ApiError extends Error {
  readonly status: number;
  readonly violations: FieldViolation[];
  readonly requestId: string | null;

  constructor(status: number, message: string, violations: FieldViolation[] = [], requestId: string | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.violations = violations;
    this.requestId = requestId;
  }

  /** Message for a specific form field, if the server flagged it. */
  fieldMessage(field: string): string | undefined {
    return this.violations.find((v) => v.field === field)?.message;
  }
}

type QueryValue = string | number | boolean | undefined | null;

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  query?: Record<string, QueryValue>;
  signal?: AbortSignal;
}

let tokenProvider: () => string | null = () => null;
let unauthorizedHandler: (() => void) | null = null;

/** The auth layer registers how to read the current token and what to do when it is rejected. */
export function configureClient(options: { getToken: () => string | null; onUnauthorized: () => void }): void {
  tokenProvider = options.getToken;
  unauthorizedHandler = options.onUnauthorized;
}

export function buildQuery(query?: Record<string, QueryValue>): string {
  if (!query) return '';
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '' || value === false) continue;
    params.append(key, String(value));
  }
  const text = params.toString();
  return text ? `?${text}` : '';
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = tokenProvider();
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}${buildQuery(options.query)}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.');
  }

  const requestId = response.headers.get('X-Request-Id');
  if (response.status === 204) return undefined as T;

  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    const body = (data ?? {}) as { message?: string; violations?: FieldViolation[] };
    // A 401 while we hold a token means it expired or was revoked: let the auth layer log out.
    if (response.status === 401 && token && unauthorizedHandler) unauthorizedHandler();
    throw new ApiError(
      response.status,
      body.message ?? `Request failed (${response.status})`,
      body.violations ?? [],
      requestId,
    );
  }
  return data as T;
}

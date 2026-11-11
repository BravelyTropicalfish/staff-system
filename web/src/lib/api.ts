/**
 * The one fetch wrapper of the SPA. The server answers every failure in one JSON shape
 * `{code, message, details}`, so this keeps that shape instead of inventing a second one.
 *
 * <p>Every request carries this tab's token when it has one, so two tabs may hold two accounts; a
 * 401 forgets that token and tells the shell to go back to the sign-in screen.
 */

import { UNAUTHORIZED_EVENT, rememberToken, storedToken } from './session.ts';
export class ApiError extends Error {
  readonly status: number;

  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

type FailureBody = { code?: string; message?: string };

function headers(extra: Record<string, string> = {}): Record<string, string> {
  const token = storedToken();
  return token === null
    ? { Accept: 'application/json', ...extra }
    : { Accept: 'application/json', 'X-Session-Token': token, ...extra };
}

function refused(status: number, body: FailureBody): ApiError {
  if (status === 401) {
    rememberToken(null);
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
  }
  return new ApiError(status, body.code ?? '请求失败', body.message ?? `HTTP ${status}`);
}

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, { headers: headers() });
  if (!response.ok) {
    throw refused(response.status, (await response.json().catch(() => ({}))) as FailureBody);
  }
  return (await response.json()) as T;
}

/** The workbook upload is multipart, so it needs the same wrapper without a JSON body. */
export async function sendForm<T>(path: string, form: FormData): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: headers(),
    body: form,
  });
  if (!response.ok) {
    throw refused(response.status, (await response.json().catch(() => ({}))) as FailureBody);
  }
  return (await response.json()) as T;
}

/** The writes speak the same error shape, so a refused save reads like a refused read. */
export async function sendJson<T>(method: 'POST' | 'PATCH' | 'DELETE', path: string, body?: unknown): Promise<T> {
  const extra: Record<string, string> = {};
  if (body !== undefined) {
    extra['Content-Type'] = 'application/json';
  }
  const response = await fetch(path, {
    method,
    headers: headers(extra),
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw refused(response.status, (await response.json().catch(() => ({}))) as FailureBody);
  }
  return (await response.json()) as T;
}

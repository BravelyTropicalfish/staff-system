/**
 * The one fetch wrapper of the SPA. The server answers every failure in one JSON shape
 * `{code, message, details}`, so this keeps that shape instead of inventing a second one.
 */
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

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as FailureBody;
    throw new ApiError(
      response.status,
      body.code ?? 'request failed',
      body.message ?? `HTTP ${response.status}`,
    );
  }
  return (await response.json()) as T;
}

/** The workbook upload is multipart, so it needs the same wrapper without a JSON body. */
export async function sendForm<T>(path: string, form: FormData): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { Accept: 'application/json' },
    body: form,
  });
  if (!response.ok) {
    const failure = (await response.json().catch(() => ({}))) as FailureBody;
    throw new ApiError(
      response.status,
      failure.code ?? 'request failed',
      failure.message ?? `HTTP ${response.status}`,
    );
  }
  return (await response.json()) as T;
}

/** The writes speak the same error shape, so a refused save reads like a refused read. */
export async function sendJson<T>(method: 'POST' | 'PATCH' | 'DELETE', path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    const failure = (await response.json().catch(() => ({}))) as FailureBody;
    throw new ApiError(
      response.status,
      failure.code ?? 'request failed',
      failure.message ?? `HTTP ${response.status}`,
    );
  }
  return (await response.json()) as T;
}

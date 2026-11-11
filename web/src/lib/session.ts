/**
 * The sign-in screen's own readings. The server owns the accounts — `POST /api/session` answers the
 * one shape below — so the form only decides whether it is worth sending a request at all.
 */

export type Credentials = { username: string; password: string };

export type SignedIn = { username: string; role: string; displayName: string };

/** The account name is trimmed because a trailing space is a typo, not a case-sensitive secret. */
export function credentials(username: string, password: string): Credentials {
  return { username: username.trim(), password };
}

/** Stops the form from sending a request the server would only refuse with 401. */
export function credentialsProblem(username: string, password: string): string | null {
  if (username.trim() === '') {
    return 'Enter the account name.';
  }
  if (password === '') {
    return 'Enter the password.';
  }
  return null;
}

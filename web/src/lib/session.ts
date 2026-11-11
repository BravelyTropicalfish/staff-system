/**
 * The sign-in screen's own readings. The server owns the accounts — `POST /api/session` answers the
 * one shape below — so the form only decides whether it is worth sending a request at all.
 */

export type Credentials = { username: string; password: string };

export type SignedIn = {
  username: string;
  role: string;
  displayName: string;
  /** 这个标签页自己的身份；服务端的会话 cookie 是整个浏览器共用的。 */
  token: string;
};

/** The account name is trimmed because a trailing space is a typo, not a case-sensitive secret. */
export function credentials(username: string, password: string): Credentials {
  return { username: username.trim(), password };
}

/** 拦住表单，不去发一个服务端只会回 401 的请求。 */
export function credentialsProblem(username: string, password: string): string | null {
  if (username.trim() === '') {
    return '请输入账号。';
  }
  if (password === '') {
    return '请输入密码。';
  }
  return null;
}

const TOKEN_KEY = 'staffing.session-token';

/** 身份失效时发出的事件；壳层听到它就跳回登录页。 */
export const UNAUTHORIZED_EVENT = 'staffing:unauthorized';

/**
 * 令牌存在 sessionStorage：它按标签页隔离，所以两个页面可以各登录一个账号。浏览器没有
 * sessionStorage（比如测试或隐私模式）时退回“没有令牌”，页面只是需要重新登录。
 */
function tabStorage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  } catch {
    return null;
  }
}

export function storedToken(): string | null {
  return tabStorage()?.getItem(TOKEN_KEY) ?? null;
}

export function rememberToken(token: string | null): void {
  const storage = tabStorage();
  if (storage === null) {
    return;
  }
  if (token === null) {
    storage.removeItem(TOKEN_KEY);
  } else {
    storage.setItem(TOKEN_KEY, token);
  }
}

import assert from 'node:assert/strict';
import test from 'node:test';
import { credentials, credentialsProblem, rememberToken, storedToken } from '../src/lib/session.ts';

/**
 * The sign-in screen's readings: the account name is trimmed because a trailing space is a typo,
 * the password is sent exactly as typed, and an empty form never reaches the server.
 */

test('the account name is trimmed and the password is left alone', () => {
  assert.deepEqual(credentials('  coordinator ', ' staffing-demo'), {
    username: 'coordinator',
    password: ' staffing-demo',
  });
});

test('an empty field is named instead of sent', () => {
  assert.equal(credentialsProblem('   ', 'staffing-demo'), '请输入账号。');
  assert.equal(credentialsProblem('coordinator', ''), '请输入密码。');
  assert.equal(credentialsProblem(' coordinator ', 'staffing-demo'), null);
});

test('the token lives in this tab\'s sessionStorage and is forgotten on demand', () => {
  const entries = new Map<string, string>();
  const fake = {
    getItem: (key: string) => entries.get(key) ?? null,
    setItem: (key: string, value: string) => {
      entries.set(key, value);
    },
    removeItem: (key: string) => {
      entries.delete(key);
    },
  };
  const hadWindow = 'window' in globalThis;
  (globalThis as { window?: unknown }).window = { sessionStorage: fake };

  try {
    assert.equal(storedToken(), null);
    rememberToken('tab-token');
    assert.equal(storedToken(), 'tab-token');
    rememberToken(null);
    assert.equal(storedToken(), null);
  } finally {
    if (!hadWindow) {
      delete (globalThis as { window?: unknown }).window;
    }
  }
});

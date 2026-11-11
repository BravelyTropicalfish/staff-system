import assert from 'node:assert/strict';
import test from 'node:test';
import { credentials, credentialsProblem } from '../src/lib/session.ts';

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
  assert.equal(credentialsProblem('   ', 'staffing-demo'), 'Enter the account name.');
  assert.equal(credentialsProblem('coordinator', ''), 'Enter the password.');
  assert.equal(credentialsProblem(' coordinator ', 'staffing-demo'), null);
});

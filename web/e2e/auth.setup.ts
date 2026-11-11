import { expect, test as setup } from '@playwright/test';
import { API, COORDINATOR, LEAD_ONE, LEAD_TWO, PASSWORD } from './support/env';

/**
 * One sign-in per account, kept as a storage state. The session cookie is the identity the whole
 * suite reuses, so a spec starts already logged in and never drives the login form itself.
 */
const ACCOUNTS = [
  { username: 'coordinator', file: COORDINATOR },
  { username: 'lead.one', file: LEAD_ONE },
  { username: 'lead.two', file: LEAD_TWO },
];

for (const account of ACCOUNTS) {
  setup(`signs in as ${account.username}`, async ({ playwright }) => {
    const context = await playwright.request.newContext({ baseURL: API });
    const response = await context.post('/api/session', {
      data: { username: account.username, password: PASSWORD },
    });
    expect(response.status(), `sign-in for ${account.username}`).toBe(200);
    await context.storageState({ path: account.file });
    await context.dispose();
  });
}

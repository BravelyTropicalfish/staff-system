import { defineConfig, devices } from '@playwright/test';
import { API, APP } from './e2e/support/env';

/** The Maven wrapper's name on the machine the suite runs on; CI is Linux, a dev box is Windows. */
const MAVEN = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';

/**
 * T26, the browser harness. The backend is started with an in-memory H2 database, so every run
 * begins from `schema.sql` plus the seed and no run can see another's rows; the two accounts sign
 * in once through `auth.setup.ts` and their storage states are reused by the specs.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: 0,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: APP,
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'e2e',
      testMatch: /.*\.spec\.ts/,
      dependencies: ['setup'],
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      // A fresh in-memory database per run is the reset: the file database of a dev run is never
      // touched, and `reuseExistingServer` stays off so an old backend cannot serve the suite.
      command:
        `${MAVEN} spring-boot:run -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:e2e`,
      cwd: '../backend',
      url: `${API}/api/health`,
      timeout: 240_000,
      reuseExistingServer: false,
    },
    {
      command: 'npm run dev -- --port 5173 --strictPort',
      url: APP,
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
});

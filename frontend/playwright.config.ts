import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for the manual end-to-end login test.
 *
 * No `webServer` block on purpose: `npx playwright test` never starts Angular,
 * the backend or Keycloak. Start the local stack first, then run the test.
 *
 *   npx playwright install chromium
 *   DEMO_USER=ops@bankops.local DEMO_PASSWORD=demo1234 npx playwright test
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env['CI']),
  retries: 0,
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: [['list']],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    viewport: { width: 1440, height: 900 },
    ignoreHTTPSErrors: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});

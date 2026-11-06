import { expect, test } from '@playwright/test';

const DEMO_USER = process.env['DEMO_USER'] ?? 'ops@bankops.local';
const DEMO_PASSWORD = process.env['DEMO_PASSWORD'] ?? 'demo1234';
const AUTHORITY = process.env['KEYCLOAK_AUTHORITY'] ?? 'http://localhost:8180/realms/bankops';


test('signs in through Keycloak and shows the dashboard', async ({ page }) => {
  await page.goto('/');

  // Verify the container serves language assets to nginx workers.
  for (const lang of ['en', 'de']) {
    const response = await page.request.get(`/i18n/${lang}.json`);
    expect(response.ok()).toBeTruthy();
    expect((await response.json()).app.name).toBe('BankOps AI');
  }

  // The auth guard starts the authorization-code + PKCE flow.
  await page.waitForURL(new RegExp(`${escapeRegExp(AUTHORITY)}/protocol/openid-connect/auth`), {
    timeout: 45_000,
  });

  await page.locator('#username').fill(DEMO_USER);
  await page.locator('#password').fill(DEMO_PASSWORD);
  await page.locator('#kc-login').click();

  // The callback exchanges the code in memory, loads /auth/me, then returns to /.
  const dashboard = page.getByTestId('dashboard');
  await expect(dashboard).toBeVisible({ timeout: 45_000 });
  await expect(page.getByRole('heading', { name: 'Dashboard', exact: true })).toBeVisible();
  await expect(page.getByTestId('dashboard-user-email')).toHaveText(DEMO_USER);
  await expect(page.getByTestId('widget-card').first()).toBeVisible();

  // Language switches and re-renders the shell.
  await page.getByRole('radio', { name: 'DE', exact: true }).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'de');
  await expect(page.locator('.sidebar').getByRole('link', { name: 'Fälle', exact: true })).toBeVisible();
});

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

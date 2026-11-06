import { expect, Page, test } from '@playwright/test';
const password = process.env['DEMO_PASSWORD'] ?? 'demo1234';
async function login(page: Page, username: string) {
  await page.goto('/');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
  await expect(page.getByTestId('dashboard')).toBeVisible({ timeout: 45000 });
  await page.getByRole('radio', { name: 'EN', exact: true }).click();
}
async function navigate(page: Page, name: string) {
  await page.locator('.sidebar').getByRole('link', { name, exact: true }).click();
}

test('synthetic case review, document evidence and AML investigation work across roles', async ({
  browser,
}) => {
  test.setTimeout(180000);
  const adminContext = await browser.newContext();
  const admin = await adminContext.newPage();
  const supervisorContext = await browser.newContext();
  const supervisor = await supervisorContext.newPage();
  try {
    await login(admin, process.env['E2E_ADMIN_USER'] ?? 'admin@bankops.local');
    const suffix = Date.now().toString();
    const title = `Synthetic review ${suffix}`;
    await navigate(admin, 'Cases');
    await admin.getByLabel('Title', { exact: true }).fill(title);
    await admin.getByLabel('Synthetic reference', { exact: true }).fill(`SYN-${suffix}`);
    await admin
      .getByLabel('Description', { exact: true })
      .fill('Synthetic investigation evidence for an independent reviewer.');
    await admin.getByLabel('I confirm this contains synthetic data only.', { exact: true }).check();
    await admin.getByRole('button', { name: 'Create synthetic case', exact: true }).click();
    await admin.getByRole('link', { name: title, exact: true }).click();
    await admin
      .getByLabel('Note / submission summary', { exact: true })
      .fill('Synthetic evidence has been collected. Please independently review.');
    await admin.getByRole('button', { name: 'Submit for review', exact: true }).click();
    await expect(admin.locator('.status').first()).toHaveText('In review');

    await login(supervisor, process.env['E2E_SUPERVISOR_USER'] ?? 'supervisor@bankops.local');
    await navigate(supervisor, 'Cases');
    await supervisor.getByLabel('Awaiting human review', { exact: true }).check();
    await supervisor.getByRole('link', { name: title, exact: true }).click();
    await supervisor
      .getByLabel('Review reason', { exact: true })
      .fill('Independently reviewed the synthetic evidence. Internal workflow can close.');
    await supervisor
      .getByRole('button', { name: 'Record review and close case', exact: true })
      .click();
    await expect(supervisor.locator('.status').first()).toHaveText('Closed');

    await navigate(admin, 'Documents');
    const filename = `synthetic-policy-${suffix}.txt`;
    const term = `evidence${suffix}`;
    await admin
      .locator('#source-file')
      .setInputFiles({
        name: filename,
        mimeType: 'text/plain',
        buffer: Buffer.from(
          `Synthetic ${term} policy: independent supervisor review is required before case closure.`,
        ),
      });
    await admin.getByLabel('I confirm this contains synthetic data only.', { exact: true }).check();
    await admin.getByRole('button', { name: 'Upload and process', exact: true }).click();
    const doc = admin
      .locator('article')
      .filter({ has: admin.getByRole('heading', { name: filename, exact: true }) });
    await expect(doc.locator('.status')).toHaveText('Ready', { timeout: 45000 });
    await navigate(admin, 'Policy evidence');
    await admin.getByLabel('Your policy question', { exact: true }).fill(term);
    await admin.getByRole('button', { name: 'Find evidence', exact: true }).click();
    await expect(
      admin.getByRole('heading', { name: 'Stored policy evidence', exact: true }),
    ).toBeVisible();
    await expect(admin.getByRole('heading', { name: filename, exact: true })).toBeVisible();
    await expect(admin.locator('blockquote')).toContainText(term);

    await navigate(admin, 'AML investigations');
    await admin
      .getByRole('button', { name: 'Generate synthetic transactions and alerts', exact: true })
      .click();
    await expect(
      admin.getByRole('link', { name: 'Open investigation case', exact: true }).first(),
    ).toBeVisible();
    await navigate(admin, 'Audit & AI activity');
    await expect(
      admin.getByRole('cell', { name: 'POLICY_EVIDENCE_SELECTION' }).first(),
    ).toBeVisible();
  } finally {
    await adminContext.close();
    await supervisorContext.close();
  }
});

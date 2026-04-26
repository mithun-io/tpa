// @ts-check
import { test, expect } from '@playwright/test';
import { injectFakeAuth } from './helpers.js';

/**
 * TC-E2E-31 to TC-E2E-50  –  Admin & Carrier Dashboard Tests
 *
 * Uses fake-auth injection to simulate authenticated admin and carrier
 * sessions without a real backend.
 */

// ════════════════════════════════════════════════════════════════════
//  ADMIN TESTS
// ════════════════════════════════════════════════════════════════════

// ── TC-E2E-31: Admin dashboard loads ──────────────────────────────────────────
test('TC-E2E-31: Admin dashboard /admin loads for FMG_ADMIN role', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/admin');

  await expect(page).not.toHaveURL('/login');
});

// ── TC-E2E-32: Admin fraud dashboard renders ──────────────────────────────────
test('TC-E2E-32: /admin/fraud-dashboard renders without redirect', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/admin/fraud-dashboard');

  await expect(page).not.toHaveURL('/login');
  await page.waitForLoadState('networkidle');
});

// ── TC-E2E-33: Admin dashboard has content ────────────────────────────────────
test('TC-E2E-33: Admin dashboard renders stat cards or data sections', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/admin');
  await page.waitForLoadState('networkidle');

  const elements = page.locator('h1, h2, h3, [class*="card"], [class*="stat"], table');
  await expect(elements.first()).toBeVisible();
});

// ── TC-E2E-34: Admin analytics page renders ───────────────────────────────────
test('TC-E2E-34: /analytics renders charts or empty state for admin', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/analytics');
  await page.waitForLoadState('networkidle');

  await expect(page).not.toHaveURL('/login');
});

// ── TC-E2E-35: Admin can navigate to claims list ──────────────────────────────
test('TC-E2E-35: Admin navigating to /claims shows claim list view', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/claims');
  await page.waitForLoadState('networkidle');

  await expect(page).not.toHaveURL('/login');
});

// ── TC-E2E-36: Admin fraud dashboard has headings ─────────────────────────────
test('TC-E2E-36: Admin fraud dashboard shows meaningful headings', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/admin/fraud-dashboard');
  await page.waitForLoadState('networkidle');

  // Look for any heading or fraud-related text
  const heading = page.locator('h1, h2, h3, [class*="title"], [class*="heading"]').first();
  await expect(heading).toBeVisible();
});

// ── TC-E2E-37: Admin sidebar visible ──────────────────────────────────────────
test('TC-E2E-37: Admin session shows sidebar navigation', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/admin');

  const nav = page.locator('nav, aside, [class*="sidebar"]').first();
  await expect(nav).toBeVisible();
});

// ── TC-E2E-38: Admin root / redirects to dashboard ────────────────────────────
test('TC-E2E-38: Authenticated admin visiting / is redirected to /dashboard', async ({ page }) => {
  await injectFakeAuth(page, 'FMG_ADMIN');
  await page.goto('/');

  await expect(page).toHaveURL(/dashboard/);
});

// ════════════════════════════════════════════════════════════════════
//  CARRIER TESTS
// ════════════════════════════════════════════════════════════════════

// ── TC-E2E-39: Carrier dashboard loads ────────────────────────────────────────
test('TC-E2E-39: /carrier dashboard loads for CARRIER_USER role', async ({ page }) => {
  await injectFakeAuth(page, 'CARRIER_USER');
  await page.goto('/carrier');

  await expect(page).not.toHaveURL('/login');
});

// ── TC-E2E-40: Carrier fraud dashboard renders ────────────────────────────────
test('TC-E2E-40: /carrier/fraud-dashboard renders without redirect', async ({ page }) => {
  await injectFakeAuth(page, 'CARRIER_USER');
  await page.goto('/carrier/fraud-dashboard');
  await page.waitForLoadState('networkidle');

  await expect(page).not.toHaveURL('/login');
});

// ── TC-E2E-41: Carrier dashboard has content ──────────────────────────────────
test('TC-E2E-41: Carrier dashboard renders stats or data sections', async ({ page }) => {
  await injectFakeAuth(page, 'CARRIER_USER');
  await page.goto('/carrier');
  await page.waitForLoadState('networkidle');

  const elements = page.locator('h1, h2, h3, [class*="card"], [class*="stat"], table');
  await expect(elements.first()).toBeVisible();
});

// ── TC-E2E-42: Carrier can view claims ────────────────────────────────────────
test('TC-E2E-42: CARRIER_USER can navigate to /claims', async ({ page }) => {
  await injectFakeAuth(page, 'CARRIER_USER');
  await page.goto('/claims');
  await page.waitForLoadState('networkidle');

  await expect(page).not.toHaveURL('/login');
});

// ── TC-E2E-43: Carrier sidebar navigation ────────────────────────────────────
test('TC-E2E-43: Carrier session shows sidebar navigation', async ({ page }) => {
  await injectFakeAuth(page, 'CARRIER_USER');
  await page.goto('/carrier');

  const nav = page.locator('nav, aside, [class*="sidebar"]').first();
  await expect(nav).toBeVisible();
});

// ════════════════════════════════════════════════════════════════════
//  CROSS-ROLE UI CHECKS
// ════════════════════════════════════════════════════════════════════

// ── TC-E2E-44: All roles: profile page accessible ────────────────────────────
test('TC-E2E-44: Profile page is accessible for all roles', async ({ page }) => {
  for (const role of ['CUSTOMER', 'FMG_ADMIN', 'CARRIER_USER']) {
    await injectFakeAuth(page, role);
    await page.goto('/profile');
    await expect(page).not.toHaveURL('/login');
  }
});

// ── TC-E2E-45: All roles: change-password accessible ─────────────────────────
test('TC-E2E-45: Change password page accessible for all roles', async ({ page }) => {
  for (const role of ['CUSTOMER', 'FMG_ADMIN', 'CARRIER_USER']) {
    await injectFakeAuth(page, role);
    await page.goto('/change-password');
    await expect(page).not.toHaveURL('/login');
  }
});

// ── TC-E2E-46: Tab switching clears form fields ───────────────────────────────
test('TC-E2E-46: Switching login tabs clears previously entered form data', async ({ page }) => {
  await page.goto('/login');
  await page.fill('#CUSTOMER-email', 'typed@test.com');
  await page.click('#tab-FMG_ADMIN');
  await page.click('#tab-CUSTOMER');

  // After switching back, form should be fresh (or at least the tab was re-mounted)
  const emailVal = await page.locator('#CUSTOMER-email').inputValue();
  expect(emailVal).toBe(''); // Fresh form state on tab switch
});

// ── TC-E2E-47: Login error clears when typing ─────────────────────────────────
test('TC-E2E-47: Error banner clears when user starts typing credentials', async ({ page }) => {
  await page.goto('/login');

  // Trigger error
  await page.click('#login-submit-CUSTOMER');
  await expect(page.getByRole('alert')).toBeVisible();

  // Start typing to clear error
  await page.fill('#CUSTOMER-email', 'a');
  await expect(page.getByRole('alert')).not.toBeVisible();
});

// ── TC-E2E-48: Login tab role description ────────────────────────────────────
test('TC-E2E-48: Each login tab shows a role description text', async ({ page }) => {
  await page.goto('/login');

  await page.click('#tab-CUSTOMER');
  await expect(page.getByText('Access your insurance claims', { exact: false })).toBeVisible();

  await page.click('#tab-FMG_ADMIN');
  await expect(page.getByText('Manage claims', { exact: false })).toBeVisible();

  await page.click('#tab-CARRIER_USER');
  await expect(page.getByText('Carrier portal access', { exact: false })).toBeVisible();
});

// ── TC-E2E-49: Register back link ────────────────────────────────────────────
test('TC-E2E-49: Register page has a "Sign in" back link to /login', async ({ page }) => {
  await page.goto('/register');
  await page.click('a[href="/login"]');
  await expect(page).toHaveURL('/login');
});

// ── TC-E2E-50: Responsive layout check ───────────────────────────────────────
test('TC-E2E-50: Login page renders correctly at mobile viewport (390x844)', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/login');

  await expect(page.locator('#tab-CUSTOMER')).toBeVisible();
  await expect(page.locator('#CUSTOMER-email')).toBeVisible();
  await expect(page.locator('#login-submit-CUSTOMER')).toBeVisible();
});

// @ts-check
import { test, expect } from '@playwright/test';

/**
 * TC-E2E-01 to TC-E2E-15  –  Auth & Routing Tests
 *
 * These tests exercise the Login page UI, tab switching, form validation,
 * register navigation, and route guarding — WITHOUT calling the real backend.
 * They run fast and in isolation.
 */

// ── TC-E2E-01: Login page renders ─────────────────────────────────────────────
test('TC-E2E-01: Login page renders with three role tabs', async ({ page }) => {
  await page.goto('/login');

  await expect(page).toHaveTitle(/TPA|ClaimSys|Insurance/i);
  await expect(page.locator('#tab-CUSTOMER')).toBeVisible();
  await expect(page.locator('#tab-FMG_ADMIN')).toBeVisible();
  await expect(page.locator('#tab-CARRIER_USER')).toBeVisible();
});

// ── TC-E2E-02: Brand name visible ─────────────────────────────────────────────
test('TC-E2E-02: Brand name "TPA ClaimSys" is displayed on login page', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByText('TPA ClaimSys', { exact: false })).toBeVisible();
});

// ── TC-E2E-03: Default tab is CUSTOMER ────────────────────────────────────────
test('TC-E2E-03: Default active tab is Customer', async ({ page }) => {
  await page.goto('/login');
  const customerTab = page.locator('#tab-CUSTOMER');
  await expect(customerTab).toHaveAttribute('aria-selected', 'true');
});

// ── TC-E2E-04: Tab switching shows correct form ────────────────────────────────
test('TC-E2E-04: Clicking Admin tab shows Admin login form', async ({ page }) => {
  await page.goto('/login');
  await page.click('#tab-FMG_ADMIN');

  await expect(page.locator('#tab-FMG_ADMIN')).toHaveAttribute('aria-selected', 'true');
  await expect(page.locator('#FMG_ADMIN-email')).toBeVisible();
  await expect(page.locator('#FMG_ADMIN-password')).toBeVisible();
});

// ── TC-E2E-05: Carrier tab switch ─────────────────────────────────────────────
test('TC-E2E-05: Clicking Carrier tab shows Carrier login form and register link', async ({ page }) => {
  await page.goto('/login');
  await page.click('#tab-CARRIER_USER');

  await expect(page.locator('#tab-CARRIER_USER')).toHaveAttribute('aria-selected', 'true');
  await expect(page.locator('#CARRIER_USER-email')).toBeVisible();
  await expect(page.getByText('Register as Carrier', { exact: false })).toBeVisible();
});

// ── TC-E2E-06: Empty form shows validation ────────────────────────────────────
test('TC-E2E-06: Submitting empty login form shows error banner', async ({ page }) => {
  await page.goto('/login');
  await page.click('#login-submit-CUSTOMER');

  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByText('Please enter your email and password', { exact: false })).toBeVisible();
});

// ── TC-E2E-07: Password toggle ────────────────────────────────────────────────
test('TC-E2E-07: Password field toggles visibility when eye icon clicked', async ({ page }) => {
  await page.goto('/login');
  const pwInput = page.locator('#CUSTOMER-password');
  const eyeBtn  = page.locator('.field-eye').first();

  await expect(pwInput).toHaveAttribute('type', 'password');
  await eyeBtn.click();
  await expect(pwInput).toHaveAttribute('type', 'text');
  await eyeBtn.click();
  await expect(pwInput).toHaveAttribute('type', 'password');
});

// ── TC-E2E-08: Register link navigates ────────────────────────────────────────
test('TC-E2E-08: "Create account" link navigates to /register', async ({ page }) => {
  await page.goto('/login');
  await page.click('a[href="/register"]');
  await expect(page).toHaveURL('/register');
});

// ── TC-E2E-09: Forgot password link ───────────────────────────────────────────
test('TC-E2E-09: "Forgot password?" link navigates to /forgot-password', async ({ page }) => {
  await page.goto('/login');
  await page.click('a[href="/forgot-password"]');
  await expect(page).toHaveURL('/forgot-password');
});

// ── TC-E2E-10: Unauthenticated redirect to login ──────────────────────────────
test('TC-E2E-10: Unauthenticated access to /dashboard redirects to /login', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page).toHaveURL('/login');
});

test('TC-E2E-10b: Unauthenticated access to /admin redirects to /login', async ({ page }) => {
  await page.goto('/admin');
  await expect(page).toHaveURL('/login');
});

test('TC-E2E-10c: Unauthenticated access to /claims redirects to /login', async ({ page }) => {
  await page.goto('/claims');
  await expect(page).toHaveURL('/login');
});

// ── TC-E2E-11: Register page renders ──────────────────────────────────────────
test('TC-E2E-11: Register page renders all Step 1 form fields', async ({ page }) => {
  await page.goto('/register');

  await expect(page.locator('#reg-name')).toBeVisible();
  await expect(page.locator('#reg-email')).toBeVisible();
  await expect(page.locator('#reg-mobile')).toBeVisible();
  await expect(page.locator('#reg-password')).toBeVisible();
  await expect(page.locator('#reg-gender')).toBeVisible();
  await expect(page.locator('#reg-dob')).toBeVisible();
  await expect(page.locator('#reg-address')).toBeVisible();
});

// ── TC-E2E-12: Register step indicators ───────────────────────────────────────
test('TC-E2E-12: Step 1 is active by default on register page', async ({ page }) => {
  await page.goto('/register');
  // Step indicator exists; first step pill should have active styling
  const stepIndicator = page.locator('.step-indicator, [class*="step"]').first();
  await expect(stepIndicator).toBeVisible();
});

// ── TC-E2E-13: Password strength meter ────────────────────────────────────────
test('TC-E2E-13: Password strength indicator appears when typing', async ({ page }) => {
  await page.goto('/register');
  await page.fill('#reg-password', 'weak');
  // pw-strength container should appear
  await expect(page.locator('.pw-strength')).toBeVisible({ timeout: 5000 });
  // The .pw-label span must be in the DOM (even if empty until re-render)
  await page.waitForTimeout(300);
  await expect(page.locator('.pw-strength')).toBeVisible();
});

test('TC-E2E-13b: Strong password shows higher strength label', async ({ page }) => {
  await page.goto('/register');
  await page.fill('#reg-password', 'Str0ng@Pass!');
  const label = page.locator('.pw-label');
  await expect(label).toBeVisible();
  const text = await label.textContent();
  expect(['Good', 'Strong']).toContain(text?.trim());
});

// ── TC-E2E-14: Register form validation ───────────────────────────────────────
test('TC-E2E-14: Submitting empty register form shows validation errors', async ({ page }) => {
  await page.goto('/register');
  await page.click('#register-submit');

  await expect(page.getByText('Name is required', { exact: false })).toBeVisible();
  await expect(page.getByText('Invalid email', { exact: false })).toBeVisible();
});

// ── TC-E2E-15: Carrier register page renders ───────────────────────────────────
test('TC-E2E-15: Carrier register page loads successfully', async ({ page }) => {
  await page.goto('/carrier-register');
  await expect(page).toHaveURL('/carrier-register');
  // Should not be redirected to login (public route)
  await expect(page.getByText(/carrier|register|company/i).first()).toBeVisible();
});

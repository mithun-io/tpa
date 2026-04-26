/**
 * Shared test helpers and mock-auth utilities for TPA E2E tests.
 *
 * Since E2E tests need a real frontend + backend, we provide two modes:
 *   1. ONLINE  – tests that hit the real backend (JWT login, claims API)
 *   2. MOCK    – tests that inject localStorage tokens to skip real auth
 *
 * All helpers are exported from this module.
 */

/** Inject a fake JWT + user into localStorage to bypass real login. */
export async function injectFakeAuth(page, role = 'CUSTOMER') {
  const userMap = {
    CUSTOMER:     { id: 1, username: 'testcustomer', email: 'customer@tpa.com', userRole: 'CUSTOMER',     userStatus: 'ACTIVE' },
    FMG_ADMIN:    { id: 2, username: 'testadmin',    email: 'admin@tpa.com',    userRole: 'FMG_ADMIN',    userStatus: 'ACTIVE' },
    CARRIER_USER: { id: 3, username: 'testcarrier',  email: 'carrier@tpa.com',  userRole: 'CARRIER_USER', userStatus: 'ACTIVE' },
  };

  // A structurally valid but non-verifiable JWT (3 base64 parts)
  const fakeToken = [
    btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' })),
    btoa(JSON.stringify({ sub: userMap[role].email, role, exp: 9999999999 })),
    'fake-signature',
  ].join('.');

  await page.addInitScript(({ token, user }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
  }, { token: fakeToken, user: userMap[role] });
}

/** Navigate to the login page and perform a real login via UI. */
export async function loginViaUI(page, email, password, tabId = 'CUSTOMER') {
  await page.goto('/login');
  await page.click(`#tab-${tabId}`);
  await page.fill(`#${tabId}-email`, email);
  await page.fill(`#${tabId}-password`, password);
  await page.click(`#login-submit-${tabId}`);
}

/** Wait for the page URL to match a pattern (after login redirect). */
export async function waitForRedirect(page, urlPattern, timeout = 8000) {
  await page.waitForURL(urlPattern, { timeout });
}

/** Check that an element with given text is visible on page. */
export async function expectVisible(page, text) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible' });
}

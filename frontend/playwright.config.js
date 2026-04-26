// @ts-check
import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E configuration for TPA ClaimSys frontend.
 * - Base URL: http://localhost:3000  (Vite dev server)
 * - API is proxied at /api/v1 → http://localhost:8080
 * - Tests run headlessly in Chromium; can run against a mock API in CI.
 */
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,        // serial so login state doesn't collide
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  timeout: 30_000,
  expect: { timeout: 8_000 },

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL: 'http://localhost:3002',
    headless: true,
    viewport: { width: 1280, height: 800 },
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /**
   * Uncomment to auto-start the Vite dev server before running tests.
   * Requires the backend to be running separately on port 8080.
   */
  // webServer: {
  //   command: 'npm run dev',
  //   url: 'http://localhost:3000',
  //   reuseExistingServer: !process.env.CI,
  //   timeout: 60_000,
  // },
});

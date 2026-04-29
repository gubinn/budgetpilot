import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  timeout: 30000,
  expect: {
    timeout: 10000,
  },
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:6060',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
    viewport: { width: 1280, height: 720 },
  },
  projects: [
    {
      name: 'chrome',
      use: {
        channel: 'chrome',
      },
    },
  ],
});

import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/live',
  outputDir: './test-results/live',
  timeout: 60_000,
  use: {
    baseURL: 'http://127.0.0.1:5173',
    browserName: 'chromium',
    channel: process.env.PLAYWRIGHT_CHANNEL || undefined,
    headless: true,
  },
  webServer: {
    command: 'pnpm dev --host 127.0.0.1',
    url: 'http://127.0.0.1:5173/login',
    reuseExistingServer: true,
    timeout: 30_000,
  },
})

import { defineConfig, devices } from '@playwright/test'

if (!process.env.ACCEPTANCE_BASE_URL || !process.env.ACCEPTANCE_MYSQL_CONFIG) {
  throw new Error('请通过 scripts/acceptance.py 启动独立原生验收环境')
}

export default defineConfig({
  testDir: './tests/acceptance',
  outputDir: './test-results/acceptance',
  timeout: 45_000,
  expect: { timeout: 12_000 },
  workers: 1,
  use: { baseURL: process.env.ACCEPTANCE_BASE_URL, headless: true, trace: 'retain-on-failure' },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
})

import { execFileSync } from 'node:child_process'
import { expect, type Page } from '@playwright/test'

export function database(query: string): string {
  const config = process.env.ACCEPTANCE_MYSQL_CONFIG
  if (!config || !config.includes('/.local/acceptance/run-')) throw new Error('拒绝非隔离验收数据库')
  const env = Object.fromEntries(Object.entries(process.env).filter(([key]) => !key.startsWith('MYSQL_')))
  // --defaults-file 仍会读取机器的登录文件；指定本次目录中的不存在路径。
  env.MYSQL_TEST_LOGIN_FILE = `${config}.disabled-login`
  return execFileSync('mysql', [`--defaults-file=${config}`, '--batch', '--raw', '--skip-column-names'],
    { input: query, encoding: 'utf8', env }).trim()
}

export async function login(page: Page, username: string) {
  await page.goto('/login')
  await page.getByPlaceholder('例如 student1').fill(username)
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page).not.toHaveURL(/\/login/)
}

export async function api(page: Page, path: string, method = 'GET', data?: unknown) {
  const token = await page.evaluate(() => localStorage.getItem('csms_token'))
  const response = await page.request.fetch(`/api${path}`, {
    method, headers: { Authorization: `Bearer ${token}` }, data,
  })
  return { status: response.status(), body: await response.json(), headers: response.headers() }
}

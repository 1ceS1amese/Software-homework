import { expect, test } from '@playwright/test'
import { mockApi } from './mockApi'

test.beforeEach(async ({ page }) => { await mockApi(page) })

const accounts = [
  { username: 'student1', route: '/student/courses', title: '课程选择' },
  { username: 'teacher1', route: '/teacher/classes', title: '我的教学班' },
  { username: 'admin', route: '/admin/dashboard', title: '教务概览' },
]

for (const account of accounts) {
  test(`${account.username} 可以登录并打开工作台`, async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', error => errors.push(error.message))
    await page.goto('/login')
    await page.getByRole('button', { name: account.username === 'student1' ? '学生' : account.username === 'teacher1' ? '教师' : '管理员' }).click()
    await page.getByRole('button', { name: '登录系统' }).click()
    await expect(page).toHaveURL(new RegExp(account.route.replaceAll('/', '\\/')))
    await expect(page.getByRole('heading', { name: account.title })).toBeVisible()
    expect(errors).toEqual([])
  })
}

test('登录页在手机与平板宽度没有横向溢出', async ({ page }) => {
  for (const width of [390, 1024]) {
    await page.setViewportSize({ width, height: 850 })
    await page.goto('/login')
    await expect(page.getByRole('button', { name: '登录系统' })).toBeVisible()
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
    expect(overflow).toBe(0)
  }
  await page.setViewportSize({ width: 390, height: 620 })
  await page.goto('/login')
  await expect(page.getByPlaceholder('例如 student1')).toBeVisible()
  const mobile = await page.evaluate(() => ({
    top: document.querySelector('main')!.getBoundingClientRect().top,
    overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    targets: [...document.querySelectorAll('input, button')].map(element => element.getBoundingClientRect().height),
  }))
  expect(mobile.top).toBeLessThanOrEqual(32)
  expect(mobile.overflow).toBe(0)
  expect(mobile.targets.every(height => height >= 44)).toBe(true)
})

test('登录页在系统深色模式下仍保持统一浅色表单', async ({ page }) => {
  const requestedTerms: string[] = []
  page.on('request', request => {
    if (new URL(request.url()).pathname === '/api/terms') requestedTerms.push(request.url())
  })
  await page.emulateMedia({ colorScheme: 'dark' })
  await page.goto('/login')
  const username = page.getByPlaceholder('例如 student1')
  await expect(username).toBeVisible()
  await expect(username).toHaveAttribute('aria-invalid', 'false')
  await expect(page.getByPlaceholder('请输入密码')).toHaveAttribute('aria-invalid', 'false')
  const colors = await username.evaluate(input => {
    const luminance = (value: string) => {
      const rgb = (value.match(/[\d.]+/g) ?? []).slice(0, 3).map(Number)
      return rgb.reduce((sum, channel, index) => {
        const normalized = channel / 255
        const linear = normalized <= 0.04045 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4
        return sum + linear * [0.2126, 0.7152, 0.0722][index]!
      }, 0)
    }
    const placeholder = getComputedStyle(input, '::placeholder').color
    return {
      background: getComputedStyle(input).backgroundColor,
      text: getComputedStyle(input).color,
      placeholderContrast: 1.05 / (luminance(placeholder) + 0.05),
    }
  })
  expect(colors.background).toBe('rgb(255, 255, 255)')
  expect(colors.text).not.toBe('rgb(255, 255, 255)')
  expect(colors.placeholderContrast).toBeGreaterThanOrEqual(4.5)
  expect(requestedTerms).toEqual([])
})

const routeGroups = [
  {
    username: 'student1', button: '学生',
    routes: ['/student/courses', '/student/timetable', '/student/grades', '/student/summary', '/profile', '/profile/password'],
  },
  {
    username: 'teacher1', button: '教师',
    routes: ['/teacher/classes', '/teacher/classes/1/roster', '/teacher/classes/1/grades', '/profile'],
  },
  {
    username: 'admin', button: '管理员',
    routes: ['/admin/dashboard', '/admin/users', '/admin/depts', '/admin/terms', '/admin/courses', '/admin/teaching-classes', '/admin/stats', '/admin/audit-logs', '/admin/login-logs', '/admin/configs', '/profile'],
  },
]

for (const group of routeGroups) {
  test(`${group.username} 页面巡检与响应式宽度`, async ({ page }, testInfo) => {
    test.setTimeout(120_000)
    const errors: string[] = []
    page.on('pageerror', error => errors.push(error.message))
    await page.setViewportSize({ width: 390, height: 850 })
    await page.goto('/login')
    await page.getByRole('button', { name: group.button }).click()
    await page.getByRole('button', { name: '登录系统' }).click()
    await expect(page).not.toHaveURL(/\/login/)
    for (const route of group.routes) {
      await page.goto(route, { waitUntil: 'domcontentloaded' })
      await expect(page.locator('h1').first()).toBeVisible()
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
      expect(overflow, `${route} at 390px`).toBe(0)
      if (route === group.routes[0]) await page.screenshot({ path: testInfo.outputPath(`${group.username}-home-mobile.png`), fullPage: true })
    }
    await page.screenshot({ path: testInfo.outputPath(`${group.username}-mobile.png`), fullPage: true })
    await page.setViewportSize({ width: 1024, height: 850 })
    for (const route of group.routes) {
      await page.goto(route, { waitUntil: 'domcontentloaded' })
      await expect(page.locator('h1').first()).toBeVisible()
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
      expect(overflow, `${route} at 1024px`).toBe(0)
      if (route === group.routes[0]) await page.screenshot({ path: testInfo.outputPath(`${group.username}-home-tablet.png`), fullPage: true })
    }
    expect(errors).toEqual([])
  })
}

test('学生不能进入管理员页面', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: '学生' }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.goto('/admin/dashboard')
  await expect(page).toHaveURL(/\/403$/)
  await expect(page.getByRole('heading', { name: /403/ })).toBeVisible()
})

test('统计页不显示虚构的全校成绩分布', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: '管理员' }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.goto('/admin/stats')
  await page.getByRole('tab', { name: '成绩分布' }).click()
  await expect(page.getByText('成绩分布暂不可用')).toBeVisible()
})

test('课程接口失败时显示明确错误而非演示课程', async ({ page }) => {
  await page.route('**/api/teaching-classes?**', route => route.abort())
  await page.goto('/login')
  await page.getByRole('button', { name: '学生' }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page.getByText('课程列表加载失败')).toBeVisible()
  await expect(page.getByText('高等数学(上)')).toHaveCount(0)
})

test('登录失败时保留输入并显示服务端错误', async ({ page }) => {
  await page.route(url => url.pathname === '/api/auth/login', route => route.fulfill({
    status: 401,
    contentType: 'application/json',
    body: JSON.stringify({ code: 20001, message: '用户名或密码错误', data: null }),
  }))
  await page.goto('/login')
  await page.getByRole('button', { name: '学生' }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('用户名或密码错误', { exact: true })).toBeVisible()
  await expect(page.getByPlaceholder('例如 student1')).toHaveValue('student1')
})

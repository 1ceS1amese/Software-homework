import { expect, test, type Page } from '@playwright/test'

async function login(page: Page, role: '学生' | '教师' | '管理员') {
  await page.goto('/login')
  await page.getByRole('button', { name: role, exact: true }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page).not.toHaveURL(/\/login/)
}

async function readApi(page: Page, path: string) {
  const token = await page.evaluate(() => localStorage.getItem('csms_token'))
  expect(token).toBeTruthy()
  const response = await page.request.get(path, { headers: { Authorization: `Bearer ${token}` } })
  expect(response.status()).toBe(200)
  return response.json()
}

test('学生能读取真实课程、课表和本人学分', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  await page.setViewportSize({ width: 390, height: 850 })
  await login(page, '学生')
  const courses = await readApi(page, '/api/teaching-classes?page=1&size=10')
  expect(courses.code).toBe(0)
  expect(courses.data.total).toBeGreaterThan(0)
  const precheck = await readApi(page, `/api/enrollments/precheck/${courses.data.records[0].id}`)
  expect(precheck.code).toBe(0)
  await page.goto('/student/courses')
  await expect(page.getByRole('heading', { name: '课程选择' })).toBeVisible()

  const user = await page.evaluate(() => JSON.parse(localStorage.getItem('csms_user') || '{}'))
  const summary = await readApi(page, `/api/stats/students/${user.id}/summary`)
  expect(summary.code).toBe(0)
  await page.goto('/student/summary')
  await expect(page.getByRole('heading', { name: '学分与学业概览' })).toBeVisible()
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow).toBe(0)
  expect(errors).toEqual([])
})

test('教师能读取本人教学班、名单与成绩权重', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  await login(page, '教师')
  const classes = await readApi(page, '/api/teaching-classes/my')
  expect(classes.code).toBe(0)
  expect(classes.data.length).toBeGreaterThan(0)
  await page.goto('/teacher/classes')
  const classId = classes.data[0].id
  const grades = await readApi(page, `/api/grades/class/${classId}`)
  expect(grades.code).toBe(0)
  await page.goto(`/teacher/classes/${classId}/grades`)
  await expect(page.getByText(/当前权重：平时/)).toBeVisible()
  await page.goto(`/teacher/classes/${classId}/roster`)
  await expect(page.getByRole('heading', { name: /教学班选课名单/ })).toBeVisible()
  expect(errors).toEqual([])
})

test('管理员能读取真实统计与管理列表', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  await login(page, '管理员')
  const stats = await readApi(page, '/api/stats/enrollment/overview')
  expect(stats.code).toBe(0)
  expect(stats.data.classCount).toBeGreaterThan(0)
  await page.goto('/admin/dashboard')
  await expect(page.getByRole('heading', { name: '教务概览' })).toBeVisible()
  await page.goto('/admin/users')
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  await page.goto('/admin/configs')
  await expect(page.getByRole('heading', { name: '系统参数' })).toBeVisible()
  expect(errors).toEqual([])
})

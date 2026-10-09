import { readFile } from 'node:fs/promises'
import { expect, test } from '@playwright/test'
import { mockApi } from './mockApi'

test('教师可按学号姓名检索，导出仅包含当前筛选名单且保留特殊字符', async ({ page }) => {
  await mockApi(page)
  const base = {
    id: 1, studentId: 10, studentUsername: '00010', studentName: '李"华',
    teachingClassId: 1, className: '数据结构,01班', courseId: 1,
    courseName: '数据结构', termId: 1, credit: 4, status: 'ENROLLED',
    enrolledAt: '2026-10-09', source: 'PORTAL',
  }
  await page.route(url => url.pathname === '/api/enrollments/class/1/students', route => route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, data: [base, { ...base, id: 2, studentId: 20, studentUsername: '00020', studentName: '王同学' }] }),
  }))
  await page.goto('/login')
  await page.getByRole('button', { name: '教师', exact: true }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page.getByRole('heading', { name: '我的教学班', exact: true })).toBeVisible()
  await page.goto('/teacher/classes/1/roster')
  await expect(page.getByText('显示 2 / 2 名学生')).toBeVisible()
  await page.getByPlaceholder('输入学号、姓名或教学班').fill('00010')
  await expect(page.getByText('显示 1 / 2 名学生')).toBeVisible()
  await expect(page.getByText('王同学', { exact: true })).toHaveCount(0)
  const downloadEvent = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出筛选名单' }).click()
  const download = await downloadEvent
  const path = await download.path()
  expect(path).not.toBeNull()
  const csv = await readFile(path!, 'utf8')
  expect(csv).toContain('"00010","李""华"')
  expect(csv).not.toContain('王同学')
  await page.getByPlaceholder('输入学号、姓名或教学班').fill('不存在的姓名')
  await expect(page.getByText('没有符合筛选条件的学生。')).toBeVisible()
  await expect(page.getByRole('button', { name: '导出筛选名单' })).toBeDisabled()
  await page.getByRole('button', { name: '重置筛选' }).click()
  await expect(page.getByText('显示 2 / 2 名学生')).toBeVisible()
})

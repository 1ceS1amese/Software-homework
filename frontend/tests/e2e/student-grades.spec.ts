import { expect, test, type Page } from '@playwright/test'
import { mockApi } from './mockApi'

async function prepare(page: Page) {
  await mockApi(page)
  const first = {
    id: 1, code: '2026-1', name: '测试第一学期', startDate: '2026-09-01', endDate: '2027-01-20',
    enrollStart: '2026-09-01 08:00:00', enrollEnd: '2026-12-01 18:00:00',
    withdrawEnd: '2026-12-15 18:00:00', status: 'ENROLLING',
  }
  await page.route(url => url.pathname === '/api/terms', route => route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, data: [first, { ...first, id: 2, code: '2026-2', name: '测试第二学期' }] }),
  }))
  await page.route(url => url.pathname === '/api/grades/my', route => {
    const termId = Number(new URL(route.request().url()).searchParams.get('termId'))
    const rows = termId === 2
      ? [{ id: 3, courseName: '第二学期课程', credit: 4, totalScore: 88, gradePoint: 3.1, isPass: 1, status: 'PUBLISHED', termId }]
      : [
        { id: 1, courseName: '数据结构', credit: 4, totalScore: 90, gradePoint: 3.3, isPass: 1, status: 'PUBLISHED', termId },
        { id: 2, courseName: '高等数学', credit: 3, totalScore: 50, gradePoint: 0, isPass: 0, status: 'PUBLISHED', termId },
        { id: 4, courseName: '未发布课程', credit: 2, totalScore: 99, gradePoint: 4, isPass: 1, status: 'DRAFT', termId },
      ]
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 0, data: rows }) })
  })
  await page.goto('/login')
  await page.getByRole('button', { name: '学生', exact: true }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page.getByRole('heading', { name: '课程选择' })).toBeVisible()
}

async function chooseSecondTerm(page: Page) {
  await page.getByRole('combobox', { name: '选择学期' }).click()
  await page.getByRole('option', { name: '测试第二学期' }).click()
}

test('成绩支持课程与考核结果筛选，统计保留完整学期口径', async ({ page }) => {
  await prepare(page)
  await page.getByRole('link', { name: '我的成绩', exact: true }).click()
  await expect(page.getByText('显示 2 / 2 门课程')).toBeVisible()
  await expect(page.getByText('未发布课程', { exact: true })).toHaveCount(0)
  await expect(page.getByText('72.86', { exact: true })).toBeVisible()
  await page.getByPlaceholder('搜索课程名称').fill('数学')
  await expect(page.getByText('显示 1 / 2 门课程')).toBeVisible()
  await expect(page.getByText('数据结构', { exact: true })).toHaveCount(0)
  await expect(page.getByText('72.86', { exact: true })).toBeVisible()
  await page.getByRole('combobox', { name: '筛选考核结果' }).click()
  await page.getByRole('option', { name: '通过', exact: true }).click()
  await expect(page.getByText('没有符合筛选条件的成绩。')).toBeVisible()
  await page.getByRole('button', { name: '重置筛选' }).click()
  await expect(page.getByText('显示 2 / 2 门课程')).toBeVisible()
})

test('切换全局学期后，成绩与学业概览自动重新加载', async ({ page }) => {
  await prepare(page)
  await page.getByRole('link', { name: '我的成绩', exact: true }).click()
  await expect(page.getByText('数据结构', { exact: true })).toBeVisible()
  await chooseSecondTerm(page)
  await expect(page.getByText('第二学期课程', { exact: true })).toBeVisible()
  await expect(page.getByText('数据结构', { exact: true })).toHaveCount(0)
  await page.route(url => url.pathname.includes('/summary'), route => {
    const termId = Number(new URL(route.request().url()).searchParams.get('termId'))
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify({
      code: 0, data: { studentId: 1, termId, totalEnrolledCredits: termId === 2 ? 14 : 7,
        earnedCredits: 4, gpa: 3, courseCount: 1, passCount: 1, failCount: 0 },
    }) })
  })
  await page.getByRole('link', { name: '我的学分统计', exact: true }).click()
  await expect(page.getByText('14', { exact: true })).toBeVisible()
  await page.getByRole('combobox', { name: '选择学期' }).click()
  await page.getByRole('option', { name: '测试第一学期' }).click()
  await expect(page.getByText('7', { exact: true })).toBeVisible()
  await expect(page.getByText('14', { exact: true })).toHaveCount(0)
})

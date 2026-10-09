import { expect, test, type Route } from '@playwright/test'
import { mockApi } from './mockApi'

test('快速连续查询时，课程列表保留最新关键词的结果', async ({ page }) => {
  await mockApi(page)
  let previous: Route | undefined
  const classroom = (name: string) => ({
    id: 2, courseId: 2, courseCode: 'CS102', courseName: name, termId: 1,
    teacherId: 2, teacherName: '测试教师', className: name + ' 01 班',
    capacity: 40, enrolledCount: 0, credit: 3, startWeek: 1, endWeek: 16,
    status: 'PUBLISHED', schedules: [],
  })
  const responseBody = (name: string) => JSON.stringify({
    code: 0, message: 'OK', data: { records: [classroom(name)], total: 1, page: 1, size: 10 },
  })
  await page.route(url => url.pathname === '/api/teaching-classes', async route => {
    const keyword = new URL(route.request().url()).searchParams.get('keyword')
    if (keyword === '旧课程') { previous = route; return }
    if (keyword === '新课程') {
      await route.fulfill({ contentType: 'application/json', body: responseBody('新课程') })
      return
    }
    await route.fallback()
  })
  await page.goto('/login')
  await page.getByRole('button', { name: '学生', exact: true }).click()
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page.getByText('数据结构', { exact: true })).toBeVisible()
  const search = page.getByPlaceholder('输入关键词')
  await search.fill('旧课程')
  await search.press('Enter')
  await expect.poll(() => !!previous).toBe(true)
  await search.fill('新课程')
  await search.press('Enter')
  await expect(page.getByText('新课程', { exact: true })).toBeVisible()
  const oldResponse = page.waitForResponse(response => new URL(response.url()).searchParams.get('keyword') === '旧课程')
  await previous!.fulfill({ contentType: 'application/json', body: responseBody('旧课程') })
  await (await oldResponse).finished()
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))))
  await expect(page.getByText('新课程', { exact: true })).toBeVisible()
  await expect(page.getByText('旧课程', { exact: true })).toHaveCount(0)
})

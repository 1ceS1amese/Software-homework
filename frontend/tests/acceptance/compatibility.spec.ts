import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

for (const width of [390, 768, 1440]) {
  test(`AC-06 ${width}px 三角色导航、课程表单、课表及旧接口兼容`, async ({ page }, info) => {
    await page.setViewportSize({ width, height: 900 })
    const errors: string[] = []
    const externalIcons: string[] = []
    page.on('pageerror', error => errors.push(error.message))
    page.on('request', request => {
      if (/^https:\/\/api\.(?:iconify\.design|simplesvg\.com|unisvg\.com)\//.test(request.url())) externalIcons.push(request.url())
    })
    const fits = async () => {
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
      expect(overflow).toBeLessThanOrEqual(2)
    }
    await login(page, 'admin')
    await page.goto('/admin/courses')
    await expect(page.getByRole('heading', { name: '课程目录', exact: true })).toBeVisible()
    await fits()
    await page.getByRole('button', { name: '新增课程', exact: true }).click()
    const dialog = page.getByRole('dialog')
    const bounds = await dialog.boundingBox()
    expect(bounds?.width).toBeLessThanOrEqual(width)
    const code = `VIEW-${info.project.name}-${width}-${Date.now() % 10000000}`
    await dialog.getByLabel('课程代码', { exact: true }).fill(code)
    await dialog.getByLabel('课程名称', { exact: true }).fill('跨屏验收课程')
    await dialog.getByLabel('开课院系', { exact: true }).selectOption('1')
    await dialog.getByRole('button', { name: '保存课程', exact: true }).click()
    await expect(dialog).not.toBeVisible()
    expect(database(`SELECT name FROM course WHERE course_code='${code}'`)).toBe('跨屏验收课程')
    const id = Number(database(`SELECT id FROM course WHERE course_code='${code}'`))
    expect((await api(page, `/courses/${id}`, 'DELETE')).body.code).toBe(0)
    for (const route of ['/admin/depts', '/admin/terms', '/admin/users', '/admin/teaching-classes', '/admin/grades', '/admin/audit-logs', '/admin/login-logs', '/admin/configs', '/admin/stats']) {
      await page.goto(route)
      await expect(page.locator('main').getByRole('heading').first()).toBeVisible()
      await fits()
    }
    await page.evaluate(() => localStorage.clear())
    await login(page, 'teacher1')
    await page.goto('/teacher/classes')
    await expect(page.getByRole('heading', { name: '我的教学班', exact: true })).toBeVisible()
    if (width < 1024) {
      await page.getByRole('button', { name: '打开菜单' }).click()
      await page.getByRole('navigation', { name: '主导航' }).getByRole('link', { name: '我的教学班', exact: true }).click()
      await expect(page.getByRole('button', { name: '关闭菜单' })).not.toBeVisible()
    }
    await fits()
    expect((await api(page, '/enrollments/class/9101/students')).body.code).toBe(0)
    expect((await api(page, '/enrollments/class/9102/students')).status).toBe(403)
    await page.evaluate(() => localStorage.clear())
    await login(page, 'acceptance_student')
    await page.goto('/student/grades')
    await expect(page.getByRole('heading', { name: '我的成绩', exact: true })).toBeVisible()
    await fits()
    await page.getByRole('combobox', { name: '选择学期' }).click()
    await page.getByRole('option', { name: '独立验收学期', exact: true }).click()
    for (const path of ['/enrollments/my?termId=9001', '/grades/my?termId=9001', '/stats/students/9003/summary?termId=9001']) {
      const result = await api(page, path)
      expect(result.status).toBe(200)
      expect(result.body.code).toBe(0)
      expect(result.body.traceId).toBeTruthy()
      expect(result.body.traceId).toBe(result.headers['x-trace-id'])
    }
    expect((await api(page, '/stats/students/9201/summary?termId=9001')).status).toBe(403)
    await page.goto('/student/timetable')
    await expect(page.getByRole('heading', { name: '我的课表 (Timetable)' })).toBeVisible()
    await fits()
    expect(errors).toEqual([])
    expect(externalIcons).toEqual([])
  })
}

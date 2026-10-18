import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('REL-09D 三角色统计范围、成绩分布与真实页面导出', async ({ page }) => {
  const headers: Record<string, { Authorization: string }> = {}
  for (const username of ['admin', 'teacher1', 'student1']) {
    const r = await page.request.post('/api/v1/auth/login', { data: { username, password: '123456' } })
    headers[username] = { Authorization: `Bearer ${(await r.json()).data.accessToken}` }
  }
  async function get(role: string, path: string) {
    const r = await page.request.get('/api/v1/stats' + path, { headers: headers[role] })
    return { status: r.status(), body: await r.json() }
  }
  const student = (await get('student1', '/enrollment/overview')).body.data
  expect(student.totalEnrollments).toBe(Number(database("SELECT COUNT(*) FROM enrollment WHERE student_id=3 AND deleted=0 AND status IN ('ENROLLED','COMPLETED')")))
  const teacher = (await get('teacher1', '/enrollment/overview')).body.data
  expect(teacher.classCount).toBe(Number(database('SELECT COUNT(*) FROM teaching_class WHERE teacher_id=2 AND deleted=0')))
  expect((await get('teacher1', '/capacity-analysis')).status).toBe(403)
  expect((await get('student1', '/grade-distribution')).status).toBe(403)
  expect((await get('teacher1', '/students/2/summary')).status).toBe(403)
  expect((await get('student1', '/enrollment/by-teacher')).status).toBe(403)
  expect((await get('teacher1', '/enrollment/by-course?deptId=999999')).body.data).toEqual([])
  const teachers = (await get('admin', '/enrollment/by-teacher')).body.data
  expect(teachers.some((t: any) => t.teacherId === '2' && t.classCount === teacher.classCount)).toBe(true)
  const distribution = (await get('admin', '/grade-distribution')).body.data
  expect(distribution.map((b: any) => b.bucket)).toEqual(['0-59', '60-69', '70-79', '80-89', '90-100'])
  const expected = Number(database("SELECT COUNT(*) FROM grade g JOIN enrollment e ON e.id=g.enrollment_id JOIN teaching_class tc ON tc.id=g.teaching_class_id WHERE g.status='PUBLISHED' AND g.deleted=0 AND e.deleted=0 AND e.status='COMPLETED' AND tc.deleted=0"))
  expect(distribution.reduce((n: number, b: any) => n + b.count, 0)).toBe(expected)
  expect(distribution.every((b: any) => b.n === expected)).toBe(true)
  const own = (await get('teacher1', '/grade-distribution')).body.data
  expect(own.reduce((n: number, b: any) => n + b.count, 0)).toBe(Number(database("SELECT COUNT(*) FROM grade g JOIN teaching_class tc ON tc.id=g.teaching_class_id JOIN enrollment e ON e.id=g.enrollment_id WHERE tc.teacher_id=2 AND tc.deleted=0 AND g.deleted=0 AND g.status='PUBLISHED' AND e.deleted=0 AND e.status='COMPLETED'")))
  await login(page, 'admin'); await page.goto('/admin/stats')
  await page.getByRole('tab', { name: '成绩分布' }).click()
  await expect(page.getByRole('row').filter({ hasText: '90-100' })).toBeVisible()
  const terms = (await api(page, '/terms')).body.data
  const historicalId = Number(database("SELECT term_id FROM grade WHERE status='PUBLISHED' AND deleted=0 ORDER BY id LIMIT 1"))
  const historical = terms.find((t: any) => t.id === historicalId)
  expect(historical).toBeTruthy()
  await page.getByRole('combobox', { name: '选择学期' }).click()
  await page.getByRole('option', { name: historical.name, exact: true }).click()
  // 全局切换学期会重建当前页面；重新选择要导出的报表。
  await page.getByRole('tab', { name: '成绩分布' }).click()
  await expect(page.getByRole('button', { name: '导出当前报表' })).toBeEnabled()
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出当前报表' }).click()
  const file = await download
  expect(file.suggestedFilename()).toBe('教务报表_gradeDist.csv')
  const stream = await file.createReadStream(); const chunks: Buffer[] = []
  for await (const chunk of stream!) chunks.push(Buffer.from(chunk))
  expect(Buffer.concat(chunks).toString('utf8')).toContain('90-100')
  expect(Buffer.concat(chunks).toString('utf8')).toContain('总人数')
})

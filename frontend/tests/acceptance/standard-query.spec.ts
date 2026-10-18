import { expect, test } from '@playwright/test'
import { api, login } from './helpers'

test('REL-09B 基础数据与教学班筛选、白名单排序、分页和有效截止时间', async ({ page }) => {
  await login(page, 'admin')
  const token = await page.evaluate(() => localStorage.getItem('csms_token'))
  const get = async (path: string) => {
    const r = await page.request.get('/api/v1' + path, { headers: { Authorization: `Bearer ${token}` } })
    return { status: r.status(), body: await r.json() }
  }
  const tree = await get('/depts?tree=true&status=ACTIVE')
  expect(tree.status).toBe(200)
  expect(tree.body.data.length).toBeGreaterThan(0)
  expect(tree.body.data.every((d: any) => d.status === 'ACTIVE' && d.children.every((m: any) => m.deptId === d.id))).toBe(true)
  expect((await get('/majors?deptId=999999')).body.data).toEqual([])
  expect((await get('/majors?status=INACTIVE')).body.data).toEqual([])
  const courses = (await get('/courses?termId=9001&keyword=ACCEPT-&size=999&sortBy=credit&sortOrder=desc')).body.data
  expect(courses.size).toBe(100)
  expect(courses.total).toBe(3)
  expect(courses.records.map((c: any) => Number(c.credit))).toEqual([3, 2, 1])
  expect((await get('/courses?termId=999999')).body.data.total).toBe(0)
  expect((await get('/courses?sortBy=credit%3BDROP')).status).toBe(400)
  expect((await get('/users?sortOrder=invalid')).status).toBe(400)
  const users = (await get('/users?role=TEACHER&sortBy=username&sortOrder=desc&size=1')).body.data
  expect(users.records).toHaveLength(1); expect(users.pages).toBe(users.total)
  expect(users.records[0].userType).toBe('TEACHER')
  const stats = (await get('/users/statistics')).body.data
  expect(stats.maleCount + stats.femaleCount + stats.unknownGenderCount).toBe(stats.total)
  expect((await get('/teaching-classes?termId=9001&deptId=999999')).body.data.total).toBe(0)
  expect((await get('/teaching-classes?termId=9001&sortBy=start_time&sortOrder=asc')).status).toBe(200)
  expect((await get('/teaching-classes?hasConflict=true')).status).toBe(403)
  const configs = (await api(page, '/configs')).body.data
  const mode = configs.find((c: any) => c.configKey === 'enroll.withdraw_deadline_mode').configValue
  try {
    expect((await api(page, '/configs/enroll.withdraw_deadline_mode', 'PUT', { configValue: 'TERM_ENROLL_END' })).status).toBe(200)
    expect((await get('/terms')).body.data.every((t: any) => t.effectiveWithdrawEnd === t.enrollEnd)).toBe(true)
    expect((await api(page, '/configs/enroll.withdraw_deadline_mode', 'PUT', { configValue: 'CUSTOM' })).status).toBe(200)
    expect((await get('/terms')).body.data.every((t: any) => t.effectiveWithdrawEnd === (t.withdrawEnd || t.enrollEnd))).toBe(true)
  } finally { await api(page, '/configs/enroll.withdraw_deadline_mode', 'PUT', { configValue: mode }) }
  await page.evaluate(() => localStorage.clear()); await login(page, 'student1')
  const filtered = await api(page, '/teaching-classes?termId=9001&hasConflict=false')
  expect(filtered.status).toBe(200)
  expect((await api(page, '/teaching-classes?termId=9001&hasConflict=true')).body.data.total).toBe(0)
})

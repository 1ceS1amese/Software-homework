import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('REL-07 CSV预览不改成绩、无效整批拒绝、失效与越权拦截、确认写入草稿', async ({ page }, info) => {
  await login(page, 'admin')
  const admin = await page.evaluate(() => localStorage.getItem('csms_token'))
  const name = `IMPORT-${info.project.name}-${Date.now() % 1000000}`
  const course = (await api(page, '/courses', 'POST', { courseCode: name, name, credit: 2, creditHours: 32, courseType: 'ELECTIVE', deptId: 1 })).body.data.id
  const id = (await api(page, '/teaching-classes', 'POST', { courseId: course, termId: 9001, teacherId: 2, className: name, capacity: 3, startWeek: 1, endWeek: 16, openClassDate: new Date(Date.now() + 60 * 86400000).toISOString().slice(0, 10) })).body.data.id
  await api(page, `/teaching-classes/${id}/schedules`, 'POST', { dayOfWeek: 7, startSection: 7, endSection: 8, startMinute: 780, endMinute: 840, startWeek: 1, endWeek: 16 })
  await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'PUBLISH' })
  const enrollments: number[] = []
  for (const studentId of [9003, 9202]) enrollments.push((await api(page, '/enrollments/admin-enroll', 'POST', { studentId, teachingClassId: id })).body.data.id)
  await page.evaluate(() => localStorage.clear()); await login(page, 'teacher1')
  const scores = enrollments.map(enrollmentId => ({ enrollmentId, regularScore: 70, midtermScore: 70, finalScore: 70 }))
  expect((await api(page, `/grades/class/${id}`, 'PUT', scores)).body.code).toBe(0)
  const snapshot = () => database(`SELECT CONCAT(enrollment_id,'|',COALESCE(regular_score,'NULL'),'|',COALESCE(midterm_score,'NULL'),'|',COALESCE(final_score,'NULL'),'|',status) FROM grade WHERE teaching_class_id=${id} ORDER BY enrollment_id`)
  const before = snapshot(), header = 'enrollmentId,regularScore,midtermScore,finalScore\r\n'
  await page.goto(`/teacher/classes/${id}/grades`)
  await page.getByRole('button', { name: 'CSV导入', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: 'CSV成绩导入' })
  const upload = async (csv: string) => {
    await dialog.getByLabel('选择CSV文件', { exact: true }).setInputFiles({ name: 'grades.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) })
    const response = page.waitForResponse(r => r.url().endsWith(`/teaching-classes/${id}/grades/import`) && r.request().method() === 'POST')
    await dialog.getByRole('button', { name: '预览导入', exact: true }).click()
    return response
  }
  const invalid = await upload(header + `${enrollments[0]},80,80,80\r\n${enrollments[1]},100.001,90,90`)
  expect(invalid.status()).toBe(200)
  await expect(dialog.getByRole('status')).toContainText('1 条须修正')
  await expect(dialog.getByRole('button', { name: '确认导入', exact: true })).toBeDisabled()
  expect(snapshot()).toBe(before)
  const csv = '\uFEFF' + header + `${enrollments[0]},"85.25",90,95\r\n${enrollments[1]},,,90\r\n`
  const preview = await upload(csv)
  const token = preview.headers()['x-import-token']
  expect(token?.length).toBe(43); expect(snapshot()).toBe(before)
  const otherToken = (await (await page.request.post('/api/auth/login', { data: { username: 'acceptance_teacher', password: '123456' } })).json()).data
  expect((await page.request.post(`/api/teaching-classes/${id}/grades/import/confirm`, { headers: { Authorization: `Bearer ${otherToken}` }, data: { previewToken: token } })).status()).toBe(403)
  expect((await api(page, `/grades/class/${id}`, 'PUT', [{ ...scores[0], regularScore: 71 }])).body.code).toBe(0)
  const changed = snapshot()
  expect((await api(page, `/teaching-classes/${id}/grades/import/confirm`, 'POST', { previewToken: token })).status).toBe(409)
  expect(snapshot()).toBe(changed)
  const refreshed = await upload(csv), confirmedToken = refreshed.headers()['x-import-token']
  await dialog.getByRole('button', { name: '确认导入', exact: true }).click(); await expect(dialog).not.toBeVisible()
  expect(database(`SELECT CONCAT(regular_score,'|',total_score,'|',status) FROM grade WHERE enrollment_id=${enrollments[0]}`)).toBe('85.25|90.58|DRAFT')
  expect(database(`SELECT regular_score IS NULL AND total_score IS NULL FROM grade WHERE enrollment_id=${enrollments[1]}`)).toBe('1')
  expect((await api(page, `/teaching-classes/${id}/grades/import/confirm`, 'POST', { previewToken: confirmedToken })).status).toBe(409)
  const teacherToken = await page.evaluate(() => localStorage.getItem('csms_token'))
  const rawPreview = (text: string) => page.request.post(`/api/teaching-classes/${id}/grades/import`, { headers: { Authorization: `Bearer ${teacherToken}` }, multipart: { file: { name: 'grades.csv', mimeType: 'text/csv', buffer: Buffer.from(text) } } })
  const duplicate = await rawPreview(header + `${enrollments[0]},90,90,90\n${enrollments[0]},90,90,90`)
  expect((await duplicate.json()).data.every((r: { valid: boolean }) => !r.valid)).toBe(true)
  expect(duplicate.headers()['x-import-token']).toBe('')
  expect((await rawPreview('wrong,headers\n1,2')).status()).toBe(400)
  expect((await rawPreview('x'.repeat(1024 * 1024 + 1))).status()).toBe(400)
  const raceToken = (await rawPreview(csv)).headers()['x-import-token']
  const race = await Promise.all([api(page, `/teaching-classes/${id}/grades/import/confirm`, 'POST', { previewToken: raceToken }), api(page, `/teaching-classes/${id}/grades/import/confirm`, 'POST', { previewToken: raceToken })])
  expect(race.map(r => r.status).sort()).toEqual([200, 409])
  expect((await api(page, `/grades/class/${id}/publish`, 'POST')).body.code).toBe(60102)
  await api(page, `/grades/class/${id}`, 'PUT', [{ enrollmentId: enrollments[1], regularScore: 90, midtermScore: 90, finalScore: 90 }])
  expect((await api(page, `/grades/class/${id}/publish`, 'POST')).body.code).toBe(0)
  expect((await rawPreview(csv)).status()).toBe(409)
  await page.reload(); await expect(page.getByRole('button', { name: 'CSV导入', exact: true })).toBeDisabled()
  expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE target_id=${id} AND action='IMPORT_GRADES'`)).toBe('2')
  const adminApi = (path: string, data: unknown) => page.request.post('/api' + path, { headers: { Authorization: `Bearer ${admin}` }, data })
  expect((await (await adminApi(`/grades/class/${id}/unlock`, { reason: '兼容性测试结束恢复演示关系' })).json()).code).toBe(0)
  expect((await (await page.request.put(`/api/teaching-classes/${id}/status`, { headers: { Authorization: `Bearer ${admin}` }, data: { action: 'CANCEL', reason: '兼容性测试结束取消演示班' } })).json()).code).toBe(0)
})

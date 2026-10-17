import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('REL-05 教学班页面增改删、排课编辑与完整状态流转', async ({ page }, info) => {
  await login(page, 'admin')
  const name = `CLASS-${info.project.name}-${Date.now() % 1000000}`
  const course = (await api(page, '/courses', 'POST', { courseCode: name, name: '开班验收课程', credit: 2, creditHours: 32, courseType: 'ELECTIVE', deptId: 1 })).body.data.id
  const form = { className: name, courseId: course, termId: 9001, teacherId: 2, capacity: 10, startWeek: 1, endWeek: 16, openClassDate: new Date(Date.now() + 60 * 86400000).toISOString().slice(0, 10) }
  await page.goto('/admin/teaching-classes')
  await page.getByRole('button', { name: '新增教学班', exact: true }).click()
  const editor = page.getByRole('dialog', { name: '新增教学班', exact: true })
  await editor.getByLabel('教学班名称', { exact: true }).fill(name)
  await editor.getByLabel('课程', { exact: true }).selectOption(String(course))
  await editor.getByLabel('学期', { exact: true }).selectOption('9001')
  await editor.getByLabel('授课教师', { exact: true }).selectOption('2')
  await editor.getByLabel('容量', { exact: true }).fill('10')
  await editor.getByLabel('开课日期', { exact: true }).fill(form.openClassDate)
  await editor.getByRole('button', { name: '保存', exact: true }).click(); await expect(editor).not.toBeVisible()
  const id = Number(database(`SELECT id FROM teaching_class WHERE class_name='${name}'`))
  expect(database(`SELECT CONCAT(status,'|',credit) FROM teaching_class WHERE id=${id}`)).toBe('DRAFT|2.0')
  expect((await api(page, '/teaching-classes', 'POST', form)).body.code).toBe(40101)
  expect((await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'PUBLISH' })).body.code).toBe(40103)
  const row = page.getByRole('row').filter({ hasText: name })
  await row.getByRole('button', { name: '编辑', exact: true }).click()
  const editing = page.getByRole('dialog', { name: '编辑教学班', exact: true })
  await editing.getByLabel('容量', { exact: true }).fill('20')
  await editing.getByRole('button', { name: '保存', exact: true }).click(); await expect(editing).not.toBeVisible()
  expect(database(`SELECT capacity FROM teaching_class WHERE id=${id}`)).toBe('20')
  await row.getByRole('button', { name: '排课', exact: true }).click()
  const schedules = page.getByRole('dialog', { name: '教学班排课', exact: true })
  await schedules.getByRole('button', { name: '新增排课', exact: true }).click()
  const scheduleForm = page.getByRole('dialog', { name: '新增排课', exact: true })
  await scheduleForm.getByLabel('星期', { exact: true }).selectOption('7')
  await scheduleForm.getByLabel('开始时刻', { exact: true }).fill('18:00')
  await scheduleForm.getByLabel('结束时刻', { exact: true }).fill('19:30')
  await scheduleForm.getByRole('button', { name: '保存', exact: true }).click(); await expect(scheduleForm).not.toBeVisible()
  await expect(schedules.getByRole('listitem')).toContainText('18:00')
  expect((await api(page, `/teaching-classes/${id}/schedules`, 'POST', { dayOfWeek: 7, startSection: 1, endSection: 2, startMinute: 1100, endMinute: 1160, startWeek: 1, endWeek: 16 })).body.code).toBe(40104)
  await schedules.getByRole('button', { name: '编辑排课', exact: true }).click()
  const scheduleEdit = page.getByRole('dialog', { name: '编辑排课', exact: true })
  await scheduleEdit.getByLabel('开始时刻', { exact: true }).fill('18:05')
  await scheduleEdit.getByRole('button', { name: '保存', exact: true }).click(); await expect(scheduleEdit).not.toBeVisible()
  await expect(schedules.getByRole('listitem')).toContainText('18:05')
  await page.keyboard.press('Escape'); await expect(schedules).not.toBeVisible()
  const confirm = page.getByRole('dialog', { name: '确认教学班操作' })
  for (const label of ['发布', '退回草稿', '发布', '关闭选课']) {
    await row.getByRole('button', { name: label, exact: true }).click()
    await confirm.getByRole('button', { name: '确认执行', exact: true }).click(); await expect(confirm).not.toBeVisible()
  }
  await page.reload(); await expect(row).toContainText(name)
  expect(database(`SELECT status FROM teaching_class WHERE id=${id}`)).toBe('CLOSED')
  expect((await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'CANCEL' })).body.code).toBe(40103)
  await row.getByRole('button', { name: '删除', exact: true }).click()
  await confirm.getByRole('button', { name: '确认执行', exact: true }).click(); await expect(confirm).not.toBeVisible()
  expect(database(`SELECT deleted FROM teaching_class WHERE id=${id}`)).toBe('1')
})

test('REL-05 教师自动归属、越权与容量保护、有学生取消同事务释放名额', async ({ page }, info) => {
  await login(page, 'teacher1')
  const name = `OWN-${info.project.name}-${Date.now() % 1000000}`
  const body = { className: name, courseId: 9001, termId: 9001, teacherId: 9002, capacity: 2, startWeek: 1, endWeek: 16, openClassDate: new Date(Date.now() + 60 * 86400000).toISOString().slice(0, 10) }
  const created = await api(page, '/teaching-classes', 'POST', body)
  expect(created.body.code).toBe(0); expect(created.body.data.teacherId).toBe(2)
  const id = created.body.data.id
  expect((await api(page, '/teaching-classes/9102', 'PUT', { capacity: 30 })).status).toBe(403)
  const slot = { dayOfWeek: 7, startSection: 9, endSection: 10, startMinute: 1020, endMinute: 1080, startWeek: 1, endWeek: 16 }
  expect((await api(page, `/teaching-classes/${id}/schedules`, 'POST', slot)).body.code).toBe(0)
  expect((await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'PUBLISH' })).body.code).toBe(0)
  expect((await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'ROLLBACK_DRAFT' })).status).toBe(403)
  const admin = (await (await page.request.post('/api/auth/login', { data: { username: 'admin', password: '123456' } })).json()).data
  const adminApi = (path: string, method: string, data?: unknown) => page.request.fetch('/api' + path, { method, headers: { Authorization: `Bearer ${admin}` }, data })
  for (const studentId of [9003, 9201]) expect((await (await adminApi('/enrollments/admin-enroll', 'POST', { studentId, teachingClassId: id })).json()).code).toBe(0)
  expect((await api(page, `/teaching-classes/${id}`, 'PUT', { capacity: 1 })).body.code).toBe(40102)
  const second = (await api(page, `/teaching-classes/${id}/schedules`, 'POST', { ...slot, dayOfWeek: 6 })).body.data.id
  expect((await api(page, `/teaching-classes/${id}/schedules/${second}`, 'DELETE')).body.code).toBe(40103)
  expect((await api(page, `/teaching-classes/${id}/schedules/${second}?confirm=true`, 'DELETE')).body.code).toBe(0)
  await page.goto('/teacher/manage')
  const row = page.getByRole('row').filter({ hasText: name })
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '取消开班', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '确认教学班操作' })
  await dialog.getByLabel('取消原因').fill('验收安排调整取消开班')
  await dialog.getByRole('button', { name: '确认执行', exact: true }).click(); await expect(dialog).not.toBeVisible()
  expect(database(`SELECT CONCAT(status,'|',enrolled_count) FROM teaching_class WHERE id=${id}`)).toBe('CANCELLED|0')
  expect(database(`SELECT COUNT(*) FROM enrollment WHERE teaching_class_id=${id} AND status='WITHDRAWN'`)).toBe('2')
  expect((await (await adminApi(`/teaching-classes/${id}`, 'DELETE')).json()).code).toBe(40105)
  expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE target_type='teaching_class' AND target_id=${id} AND action='CLASS_CANCEL'`)).toBe('1')
})

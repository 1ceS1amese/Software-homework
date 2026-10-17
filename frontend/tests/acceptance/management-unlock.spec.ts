import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('REL-06 管理员页面解锁，学生自动撤下结果，教师更正再发布', async ({ page, browser }, info) => {
  await login(page, 'admin')
  const name = `UNLOCK-${info.project.name}-${Date.now() % 1000000}`
  const course = (await api(page, '/courses', 'POST', { courseCode: name, name, credit: 2, creditHours: 32, courseType: 'ELECTIVE', deptId: 1 })).body.data.id
  const id = (await api(page, '/teaching-classes', 'POST', { courseId: course, termId: 9001, teacherId: 2, className: name, capacity: 2, startWeek: 1, endWeek: 16, openClassDate: new Date(Date.now() + 60 * 86400000).toISOString().slice(0, 10) })).body.data.id
  expect((await api(page, `/teaching-classes/${id}/schedules`, 'POST', { dayOfWeek: 7, startSection: 12, endSection: 12, startMinute: 1320, endMinute: 1380, startWeek: 1, endWeek: 16 })).body.code).toBe(0)
  expect((await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'PUBLISH' })).body.code).toBe(0)
  const enrollment = (await api(page, '/enrollments/admin-enroll', 'POST', { studentId: 9003, teachingClassId: id })).body.data.id
  const teacherContext = await browser.newContext(), studentContext = await browser.newContext()
  const teacher = await teacherContext.newPage(), student = await studentContext.newPage()
  try {
    await login(teacher, 'teacher1'); await login(student, 'acceptance_student')
    const scores = (score: number) => [{ enrollmentId: enrollment, regularScore: score, midtermScore: score, finalScore: score }]
    expect((await api(teacher, `/grades/class/${id}`, 'PUT', scores(88))).body.code).toBe(0)
    expect((await api(teacher, `/grades/class/${id}/publish`, 'POST')).body.code).toBe(0)
    await student.goto('/student/grades')
    const studentRow = student.getByRole('row').filter({ hasText: name })
    await expect(studentRow).toContainText('88')
    await page.goto('/admin/grades')
    await page.getByPlaceholder('搜索教学班或课程').fill(name)
    await page.getByRole('button', { name: '查询', exact: true }).click()
    const row = page.getByRole('row').filter({ hasText: name })
    await expect(row).toContainText('已发布')
    await row.getByRole('button', { name: '查看成绩', exact: true }).click()
    const details = page.getByRole('dialog', { name: '教学班成绩详情' })
    await expect(details.getByRole('row').filter({ hasText: '验收学生' })).toContainText('88')
    await page.keyboard.press('Escape'); await expect(details).not.toBeVisible()
    expect((await api(page, `/grades/class/${id}/unlock`, 'POST', { reason: '短' })).body.code).toBe(60105)
    expect((await api(teacher, `/grades/class/${id}/unlock`, 'POST', { reason: '教师不能自己解锁成绩' })).body.code).toBe(60104)
    await row.getByRole('button', { name: '解锁', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '管理员解锁成绩' })
    await expect(dialog.getByRole('button', { name: '确认解锁', exact: true })).toBeDisabled()
    await dialog.getByLabel('解锁原因', { exact: true }).fill('成绩复核后需要更正分数')
    await dialog.getByRole('button', { name: '确认解锁', exact: true }).click(); await expect(dialog).not.toBeVisible()
    await expect(studentRow).toHaveCount(0)
    expect(database(`SELECT CONCAT(status,'|',unlock_count) FROM grade WHERE enrollment_id=${enrollment}`)).toBe('DRAFT|1')
    expect(database(`SELECT status FROM enrollment WHERE id=${enrollment}`)).toBe('ENROLLED')
    expect((await api(teacher, `/grades/class/${id}`, 'PUT', scores(90))).body.code).toBe(0)
    expect((await api(teacher, `/grades/class/${id}/publish`, 'POST')).body.code).toBe(0)
    await expect(studentRow).toContainText('90')
    expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE target_type='teaching_class' AND target_id=${id} AND action='UNLOCK_GRADES' AND user_id=1`)).toBe('1')
    // 仅清理本例拥有的演示关系，避免其他浏览器项目在同一时段互相干扰。
    expect((await api(page, `/grades/class/${id}/unlock`, 'POST', { reason: '兼容性测试结束恢复演示关系' })).body.code).toBe(0)
    expect((await api(page, `/teaching-classes/${id}/status`, 'PUT', { action: 'CANCEL', reason: '兼容性测试结束取消演示班' })).body.code).toBe(0)
  } finally { await teacherContext.close(); await studentContext.close() }
})

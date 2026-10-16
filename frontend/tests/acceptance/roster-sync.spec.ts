import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('AC-03 学生页面选退课后，另一独立教师会话自动接收名单与通知', async ({ page, browser }) => {
  database("DELETE FROM grade WHERE teaching_class_id=9101; UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9101; UPDATE teaching_class SET enrolled_count=0 WHERE id=9101;")
  const teacherContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const teacher = await teacherContext.newPage()
  try {
    await login(teacher, 'teacher1')
    await teacher.goto('/teacher/classes/9101/roster')
    await expect(teacher.getByText('当前教学班暂无选课学生。')).toBeVisible()
    await teacher.getByPlaceholder('输入学号、姓名或教学班').fill('验收')
    await login(page, 'acceptance_student')
    await page.goto('/student/courses')
    await page.getByPlaceholder('输入关键词').fill('ACCEPT-A')
    await page.getByRole('button', { name: '查询', exact: true }).click()
    const row = page.getByRole('row').filter({ hasText: '验收互动班' })
    await row.getByRole('button', { name: '选课', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: '确认', exact: true }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
    expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe('1')
    await expect(teacher.getByRole('row').filter({ hasText: 'acceptance_student' })).toContainText('验收学生')
    await expect(teacher.getByPlaceholder('输入学号、姓名或教学班')).toHaveValue('验收')
    await expect(teacher.getByRole('status').filter({ hasText: '新增选课' })).toContainText('验收学生')
    await row.getByRole('button', { name: '退课', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: '确认', exact: true }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
    expect(database("SELECT status FROM enrollment WHERE student_id=9003 AND teaching_class_id=9101")).toBe('WITHDRAWN')
    expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe('0')
    await expect(teacher.getByRole('row').filter({ hasText: 'acceptance_student' })).toHaveCount(0)
    await expect(teacher.getByRole('status').filter({ hasText: '退课通知' })).toContainText('验收学生')
  } finally { await teacherContext.close() }
})

test('AC-03 教师教学班列表自动更新人数并提示变化', async ({ page, browser }) => {
  database("DELETE FROM grade WHERE teaching_class_id=9101; UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9101; UPDATE teaching_class SET enrolled_count=0 WHERE id=9101;")
  await login(page, 'teacher1')
  await page.goto('/teacher/classes')
  const row = page.getByRole('row').filter({ hasText: '验收互动班' })
  await expect(row).toContainText('0 / 30')
  const studentContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const student = await studentContext.newPage()
  try {
    await login(student, 'acceptance_student')
    expect((await api(student, '/enrollments', 'POST', { teachingClassId: 9101 })).body.code).toBe(0)
    await expect(row).toContainText('1 / 30')
    await expect(page.getByRole('status').filter({ hasText: '选课人数已更新' })).toContainText('验收互动班 1 人')
    expect((await api(student, '/enrollments/9101', 'DELETE')).body.code).toBe(0)
    await expect(row).toContainText('0 / 30')
  } finally { await studentContext.close() }
})

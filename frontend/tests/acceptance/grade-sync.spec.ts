import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

function prepare() {
  database(`DELETE FROM grade WHERE teaching_class_id=9101;
    UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9101 OR (student_id=9003 AND teaching_class_id=9102);
    UPDATE teaching_class SET enrolled_count=0 WHERE id=9102;
    INSERT INTO enrollment (student_id,teaching_class_id,course_id,term_id,credit,status)
    VALUES (9003,9101,9001,9001,3,'ENROLLED') ON DUPLICATE KEY UPDATE status='ENROLLED';
    UPDATE teaching_class SET enrolled_count=1 WHERE id=9101;
    INSERT INTO class_schedule (id,teaching_class_id,day_of_week,start_section,end_section,start_minute,end_minute,location,week_desc,start_week,end_week)
    VALUES (9301,9101,1,1,2,480,570,'验收教室','1-16周',1,16) ON DUPLICATE KEY UPDATE location='验收教室';`)
}

test('AC-05 教师页面发布与管理员解锁后，独立学生端自动显示或撤下成绩及提示', async ({ page, browser }) => {
  prepare()
  const studentContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const student = await studentContext.newPage()
  const summaryContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const summary = await summaryContext.newPage()
  const adminContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const admin = await adminContext.newPage()
  try {
    await login(student, 'acceptance_student')
    await student.goto('/student/grades')
    await expect(student.getByText('本学期暂无已发布成绩。')).toBeVisible()
    await login(summary, 'acceptance_student')
    await summary.goto('/student/summary')
    const earned = summary.getByText('已获学分', { exact: true }).locator('..')
    await expect(earned).toContainText('0')
    await login(page, 'teacher1')
    await page.goto('/teacher/classes/9101/grades')
    await page.getByLabel('平时成绩', { exact: true }).fill('80')
    await page.getByLabel('期中成绩', { exact: true }).fill('90')
    await page.getByLabel('期末成绩', { exact: true }).fill('100')
    await page.getByRole('button', { name: '正式发布', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: '确认发布', exact: true }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
    const row = student.getByRole('row').filter({ hasText: '验收互动课程' })
    await expect(row).toContainText('91')
    await expect(student.getByRole('status').filter({ hasText: '成绩已更新' })).toContainText('验收互动课程')
    await expect(earned.getByText('3', { exact: true })).toBeVisible()
    await expect(summary.getByText('平均学分绩点', { exact: true }).locator('..')).toContainText('3.33')
    await login(admin, 'admin')
    expect((await api(admin, '/grades/class/9101/unlock', 'POST', { reason: '验收更正后重新审核成绩' })).body.code).toBe(0)
    await expect(row).toHaveCount(0)
    await expect(student.getByRole('status').filter({ hasText: '成绩已撤回' })).toContainText('验收互动课程')
    await expect(earned.getByText('0', { exact: true })).toBeVisible()
  } finally { await studentContext.close(); await summaryContext.close(); await adminContext.close() }
})

test('AC-05 成绩发布后课表及已选学分仍保留，已完成课程不可退课', async ({ page, browser }) => {
  prepare()
  await login(page, 'teacher1')
  const id = Number(database('SELECT id FROM enrollment WHERE student_id=9003 AND teaching_class_id=9101'))
  expect((await api(page, '/grades/class/9101', 'PUT', [{ enrollmentId: id, regularScore: 90, midtermScore: 90, finalScore: 90 }])).body.code).toBe(0)
  expect((await api(page, '/grades/class/9101/publish', 'POST')).body.code).toBe(0)
  const studentContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const student = await studentContext.newPage()
  try {
    await login(student, 'acceptance_student')
    const schedule = await api(student, '/enrollments/my/schedule?termId=9001')
    expect(schedule.body.data.map((row: { id: number }) => row.id)).toContain(9101)
    const result = await api(student, '/stats/students/9003/summary?termId=9001')
    expect(result.body.data.totalEnrolledCredits).toBe(3)
    expect(result.body.data.earnedCredits).toBe(3)
    expect(result.body.data.gpa).toBe(3.25)
    const previousMax = database("SELECT config_value FROM sys_config WHERE config_key='enroll.max_credit_per_term'")
    try {
      database("UPDATE sys_config SET config_value='4' WHERE config_key='enroll.max_credit_per_term'")
      expect((await api(student, '/enrollments', 'POST', { teachingClassId: 9102 })).body.code).toBe(50109)
    } finally {
      database(`UPDATE sys_config SET config_value='${previousMax.replace(/'/g, "''")}' WHERE config_key='enroll.max_credit_per_term'`)
    }
    expect((await api(student, '/enrollments/9101', 'DELETE')).body.code).toBe(60103)
    expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe('1')
    await student.goto('/student/timetable')
    await student.getByText('验收互动课程', { exact: true }).first().click()
    await expect(student.getByRole('dialog').getByRole('button', { name: '退选本门课程' })).toBeDisabled()
  } finally { await studentContext.close() }
})

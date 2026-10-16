import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('AC-02 教师不能作为学生选课，也不能读取其他教师的学生名单', async ({ page }) => {
  await login(page, 'teacher1')
  const before = database('SELECT enrolled_count FROM teaching_class WHERE id=9101')
  expect((await api(page, '/enrollments/class/9102/students')).status).toBe(403)
  expect((await api(page, '/enrollments', 'POST', { teachingClassId: 9101 })).status).toBe(403)
  expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe(before)
  expect(database('SELECT COUNT(*) FROM enrollment WHERE student_id=2 AND teaching_class_id=9101')).toBe('0')
})

test('AC-02 学生选退课、重复请求、重选与容量在数据库中保持一致', async ({ page }) => {
  database("DELETE FROM grade WHERE teaching_class_id=9101; UPDATE enrollment SET status='WITHDRAWN' WHERE student_id=9003 AND teaching_class_id=9101; UPDATE teaching_class SET enrolled_count=0 WHERE id=9101;")
  await login(page, 'acceptance_student')
  const enrolled = await api(page, '/enrollments', 'POST', { teachingClassId: 9101 })
  expect(enrolled.body.code).toBe(0)
  expect((await api(page, '/enrollments', 'POST', { teachingClassId: 9101 })).body.code).toBe(50101)
  expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe('1')
  expect((await api(page, '/enrollments/9101', 'DELETE')).body.code).toBe(0)
  expect((await api(page, '/enrollments/9101', 'DELETE')).body.code).toBe(0)
  expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe('0')
  expect(database('SELECT status FROM enrollment WHERE student_id=9003 AND teaching_class_id=9101')).toBe('WITHDRAWN')
  expect((await api(page, '/enrollments', 'POST', { teachingClassId: 9101 })).body.code).toBe(0)
  expect(database('SELECT COUNT(*) FROM enrollment WHERE student_id=9003 AND teaching_class_id=9101')).toBe('1')
  expect((await api(page, '/enrollments/9101', 'DELETE')).body.code).toBe(0)
})

test('AC-02 20 个真实账号并发争抢 1 个名额，只有 1 次成功且计数一致', async ({ request }) => {
  database("UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9103; UPDATE teaching_class SET enrolled_count=0 WHERE id=9103;")
  const tokens = await Promise.all(Array.from({ length: 20 }, async (_, index) => {
    const response = await request.post('/api/auth/login', { data: { username: `acceptance_race_${index + 1}`, password: '123456' } })
    const result = await response.json()
    expect(result.code).toBe(0)
    expect(typeof result.data).toBe('string')
    return result.data as string
  }))
  const results = await Promise.all(tokens.map(async token => {
    const response = await request.post('/api/enrollments', {
      headers: { Authorization: `Bearer ${token}` }, data: { teachingClassId: 9103 },
    })
    return (await response.json()).code as number
  }))
  expect(results.filter(code => code === 0)).toHaveLength(1)
  expect(results.filter(code => code === 50102)).toHaveLength(19)
  expect(database("SELECT CONCAT(tc.enrolled_count, '|', COUNT(e.id)) FROM teaching_class tc LEFT JOIN enrollment e ON e.teaching_class_id=tc.id AND e.status IN ('ENROLLED','COMPLETED') AND e.deleted=0 WHERE tc.id=9103 GROUP BY tc.id")).toBe('1|1')
})

test('AC-02 管理员代选必须校验学生身份并记录来源，停用后旧令牌也不能写入', async ({ page, browser }) => {
  database("DELETE FROM grade WHERE teaching_class_id=9101; UPDATE enrollment SET status='WITHDRAWN' WHERE student_id=9003 AND teaching_class_id=9101; UPDATE teaching_class SET enrolled_count=0 WHERE id=9101;")
  await login(page, 'admin')
  expect((await api(page, '/enrollments/admin-enroll', 'POST', { studentId: 2, teachingClassId: 9101 })).body.code).toBe(50111)
  const selected = await api(page, '/enrollments/admin-enroll', 'POST', { studentId: 9003, teachingClassId: 9101 })
  expect(selected.body.code).toBe(0)
  expect(selected.body.data.source).toBe('ADMIN')
  expect(database("SELECT source FROM enrollment WHERE student_id=9003 AND teaching_class_id=9101")).toBe('ADMIN')
  expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE action='ENROLL' AND user_id=1
    AND target_id=${selected.body.data.id} AND JSON_UNQUOTE(JSON_EXTRACT(after_json, '$.source'))='ADMIN'`)).not.toBe('0')
  const context = await browser.newContext()
  const student = await context.newPage()
  try {
    await login(student, 'acceptance_student')
    database("UPDATE sys_user SET status='DISABLED' WHERE id=9003")
    const rejected = await api(student, '/enrollments/9101', 'DELETE')
    expect(rejected.body.code).not.toBe(0)
    expect(database('SELECT enrolled_count FROM teaching_class WHERE id=9101')).toBe('1')
  } finally {
    database("UPDATE sys_user SET status='ACTIVE' WHERE id=9003")
    await context.close()
  }
})

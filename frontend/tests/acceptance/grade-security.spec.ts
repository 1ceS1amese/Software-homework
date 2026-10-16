import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('AC-04 教师伪造跨班选课记录时必须整批拒绝，不能修改其他班成绩', async ({ page }) => {
  database('DELETE FROM grade WHERE teaching_class_id IN (9101,9102)')
  database(`INSERT INTO enrollment (student_id,teaching_class_id,course_id,term_id,credit,status)
    VALUES (9003,9102,9002,9001,2,'ENROLLED') ON DUPLICATE KEY UPDATE status='ENROLLED';
    UPDATE teaching_class SET enrolled_count=1 WHERE id=9102;`)
  const id = Number(database('SELECT id FROM enrollment WHERE student_id=9003 AND teaching_class_id=9102'))
  const before = database(`SELECT COUNT(*) FROM grade WHERE enrollment_id=${id}`)
  await login(page, 'teacher1')
  const result = await api(page, '/grades/class/9101', 'PUT', [{ enrollmentId: id, regularScore: 90, midtermScore: 90, finalScore: 90 }])
  expect(result.status).toBe(403)
  expect(database(`SELECT COUNT(*) FROM grade WHERE enrollment_id=${id}`)).toBe(before)
})

test('AC-04 成绩批量保存校验归属、重复ID、缺失ID和非法分数，失败后整批无变化', async ({ page }) => {
  database(`DELETE FROM grade WHERE teaching_class_id IN (9101,9102);
    UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9101;
    INSERT INTO enrollment (student_id,teaching_class_id,course_id,term_id,credit,status)
    VALUES (9003,9101,9001,9001,3,'ENROLLED'),(9201,9101,9001,9001,3,'ENROLLED')
    ON DUPLICATE KEY UPDATE status='ENROLLED'; UPDATE teaching_class SET enrolled_count=2 WHERE id=9101;`)
  const ids = database('SELECT id FROM enrollment WHERE teaching_class_id=9101 AND status="ENROLLED" ORDER BY id').split('\n').map(Number)
  const foreign = Number(database('SELECT id FROM enrollment WHERE student_id=9003 AND teaching_class_id=9102'))
  const score = (enrollmentId: number) => ({ enrollmentId, regularScore: 92, midtermScore: 85, finalScore: 88, totalScore: 100 })
  await login(page, 'teacher1')
  for (const invalid of [score(foreign), { ...score(ids[1]!), finalScore: 101 },
    { ...score(ids[1]!), regularScore: 'abc' }, { ...score(ids[1]!), regularScore: 0.123 },
    { regularScore: 90 }, score(ids[0]!), { ...score(ids[1]!), enrollmentId: 0.5 }]) {
    const result = await api(page, '/grades/class/9101', 'PUT', [score(ids[0]!), invalid])
    expect(result.body.code).not.toBe(0)
    expect(result.status).toBeLessThan(500)
    expect(database('SELECT COUNT(*) FROM grade WHERE teaching_class_id=9101')).toBe('0')
  }
  expect((await api(page, '/grades/class/9101', 'PUT', ids.map(score))).body.code).toBe(0)
  expect(database('SELECT GROUP_CONCAT(total_score ORDER BY enrollment_id) FROM grade WHERE teaching_class_id=9101')).toBe('88.30,88.30')
  expect((await api(page, '/grades/class/9101', 'PUT', ids.map((id, index) => ({ enrollmentId: id,
    regularScore: index * 100, midtermScore: index * 100, finalScore: index * 100 })))).body.code).toBe(0)
  expect(database('SELECT GROUP_CONCAT(CONCAT(total_score, ":", grade_point) ORDER BY enrollment_id) FROM grade WHERE teaching_class_id=9101')).toBe('0.00:0.00,100.00:4.00')
  const before = database('SELECT GROUP_CONCAT(total_score ORDER BY enrollment_id) FROM grade WHERE teaching_class_id=9101')
  expect((await api(page, '/grades/class/9101', 'PUT', [{ ...score(ids[0]!), finalScore: 100 }, { ...score(ids[1]!), finalScore: -1 }])).body.code).toBe(60101)
  expect(database('SELECT GROUP_CONCAT(total_score ORDER BY enrollment_id) FROM grade WHERE teaching_class_id=9101')).toBe(before)
})

test('AC-04 教师页面保存修改与发布落库，发布锁和管理员解锁审计有效', async ({ page, browser }) => {
  database(`DELETE FROM grade WHERE teaching_class_id=9101;
    UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9101;
    INSERT INTO enrollment (student_id,teaching_class_id,course_id,term_id,credit,status)
    VALUES (9003,9101,9001,9001,3,'ENROLLED') ON DUPLICATE KEY UPDATE status='ENROLLED';
    UPDATE teaching_class SET enrolled_count=1 WHERE id=9101;`)
  await login(page, 'teacher1')
  expect((await api(page, '/grades/class/9101/publish', 'POST')).body.code).toBe(60102)
  await page.goto('/teacher/classes/9101/grades')
  await page.getByLabel('平时成绩', { exact: true }).fill('92')
  await page.getByLabel('期中成绩', { exact: true }).fill('85')
  await page.getByLabel('期末成绩', { exact: true }).fill('88')
  await page.getByRole('button', { name: '暂存草稿', exact: true }).click()
  await expect(page.getByRole('button', { name: '暂存草稿', exact: true })).toBeEnabled()
  expect(database('SELECT CONCAT(total_score,"|",grade_point,"|",status) FROM grade WHERE teaching_class_id=9101')).toBe('88.30|3.12|DRAFT')
  await page.getByLabel('期末成绩', { exact: true }).fill('100')
  await page.getByRole('button', { name: '暂存草稿', exact: true }).click()
  await expect(page.getByLabel('期末成绩', { exact: true })).toHaveValue('100')
  await expect.poll(() => database('SELECT total_score FROM grade WHERE teaching_class_id=9101')).toBe('93.10')
  const studentContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const student = await studentContext.newPage()
  const adminContext = await browser.newContext({ baseURL: process.env.ACCEPTANCE_BASE_URL })
  const admin = await adminContext.newPage()
  try {
    await login(student, 'acceptance_student')
    expect((await api(student, '/grades/my?termId=9001')).body.data).toHaveLength(0)
    await page.getByRole('button', { name: '正式发布', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: '确认发布', exact: true }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
    await expect(page.getByLabel('平时成绩', { exact: true })).toBeDisabled()
    expect(database('SELECT CONCAT(g.status,"|",e.status) FROM grade g JOIN enrollment e ON e.id=g.enrollment_id WHERE g.teaching_class_id=9101')).toBe('PUBLISHED|COMPLETED')
    const id = Number(database('SELECT enrollment_id FROM grade WHERE teaching_class_id=9101'))
    expect((await api(page, '/grades/class/9101', 'PUT', [{ enrollmentId: id, regularScore: 0 }])).body.code).toBe(60103)
    expect((await api(page, '/grades/class/9101/unlock', 'POST', { reason: '教师无权解锁测试' })).body.code).toBe(60104)
    expect((await api(student, '/grades/my?termId=9001')).body.data[0].totalScore).toBe(93.1)
    await login(admin, 'admin')
    expect((await api(admin, '/grades/class/9101/unlock', 'POST', { reason: '短' })).body.code).toBe(60105)
    expect((await api(admin, '/grades/class/9101/unlock', 'POST', { reason: '长'.repeat(301) })).status).toBe(400)
    const auditBefore = Number(database("SELECT COUNT(*) FROM sys_audit_log WHERE action='UNLOCK_GRADES' AND target_id=9101"))
    expect((await api(admin, '/grades/class/9101/unlock', 'POST', { reason: '验收发现录入错误需要更正' })).body.code).toBe(0)
    expect(database('SELECT CONCAT(g.status,"|",e.status,"|",g.unlock_count) FROM grade g JOIN enrollment e ON e.id=g.enrollment_id WHERE g.teaching_class_id=9101')).toBe('DRAFT|ENROLLED|1')
    expect(Number(database("SELECT COUNT(*) FROM sys_audit_log WHERE action='UNLOCK_GRADES' AND target_id=9101"))).toBe(auditBefore + 1)
    expect((await api(student, '/grades/my?termId=9001')).body.data).toHaveLength(0)
  } finally { await studentContext.close(); await adminContext.close() }
})

test('AC-04 发布末尾审计写入故障时，成绩和选课状态必须一起回滚，恢复后可重试', async ({ page }) => {
  database(`DELETE FROM grade WHERE teaching_class_id=9101;
    UPDATE enrollment SET status='WITHDRAWN' WHERE teaching_class_id=9101;
    INSERT INTO enrollment (student_id,teaching_class_id,course_id,term_id,credit,status)
    VALUES (9003,9101,9001,9001,3,'ENROLLED') ON DUPLICATE KEY UPDATE status='ENROLLED';
    UPDATE teaching_class SET enrolled_count=1 WHERE id=9101;`)
  await login(page, 'teacher1')
  const id = Number(database('SELECT id FROM enrollment WHERE student_id=9003 AND teaching_class_id=9101'))
  expect((await api(page, '/grades/class/9101', 'PUT', [{ enrollmentId: id, regularScore: 90, midtermScore: 90, finalScore: 90 }])).body.code).toBe(0)
  const before = database("SELECT COUNT(*) FROM sys_audit_log WHERE action='PUBLISH_GRADES' AND target_id=9101")
  database(`CREATE TRIGGER acceptance_fail_publish BEFORE INSERT ON sys_audit_log FOR EACH ROW
    SET NEW.result=IF(NEW.action='PUBLISH_GRADES' AND NEW.target_id=9101, NULL, NEW.result);`)
  try {
    expect((await api(page, '/grades/class/9101/publish', 'POST')).status).toBe(500)
    expect(database('SELECT CONCAT(g.status,"|",e.status) FROM grade g JOIN enrollment e ON e.id=g.enrollment_id WHERE g.teaching_class_id=9101')).toBe('DRAFT|ENROLLED')
    expect(database("SELECT COUNT(*) FROM sys_audit_log WHERE action='PUBLISH_GRADES' AND target_id=9101")).toBe(before)
  } finally { database('DROP TRIGGER acceptance_fail_publish') }
  expect((await api(page, '/grades/class/9101/publish', 'POST')).body.code).toBe(0)
  expect(database('SELECT CONCAT(g.status,"|",e.status) FROM grade g JOIN enrollment e ON e.id=g.enrollment_id WHERE g.teaching_class_id=9101')).toBe('PUBLISHED|COMPLETED')
})

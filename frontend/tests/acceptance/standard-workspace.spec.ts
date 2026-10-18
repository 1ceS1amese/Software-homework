import { expect, test } from '@playwright/test'
import { database } from './helpers'

test('REL-09C 标准选课/教学/成绩完整链路、批次隔离及旧退课语义', async ({ page }, info) => {
  const tokens: Record<string, string> = {}
  for (const username of ['admin', 'teacher1', 'acceptance_student', 'acceptance_teacher']) {
    const r = await page.request.post('/api/v1/auth/login', { data: { username, password: '123456' } })
    expect(r.status()).toBe(200); tokens[username] = (await r.json()).data.accessToken
  }
  async function call(role: string, path: string, method = 'GET', data?: unknown) {
    const r = await page.request.fetch('/api/v1' + path, { method, data, headers: { Authorization: `Bearer ${tokens[role]}` } })
    return { status: r.status(), body: await r.json() }
  }
  const created = await call('teacher1', '/teaching-classes', 'POST', { courseId: '9001', termId: '9001', className: `标准链路-${info.project.name}-${Date.now()}`, capacity: 10, startWeek: 1, endWeek: 16, openClassDate: '2090-01-01' })
  expect(created.status).toBe(200)
  const id = created.body.data.id
  try {
    expect((await call('teacher1', `/teaching-classes/${id}/schedules`, 'POST', { dayOfWeek: 7, startSection: 11, endSection: 12, startMinute: 1200, endMinute: 1320, startWeek: 1, endWeek: 16, location: '标准验收' })).status).toBe(200)
    expect((await call('teacher1', `/teaching-classes/${id}/status`, 'PUT', { action: 'PUBLISH' })).status).toBe(200)
    expect((await call('acceptance_student', '/enrollments/check', 'POST', { teachingClassId: id })).body.data.canEnroll).toBe(true)
    expect(database(`SELECT enrolled_count FROM teaching_class WHERE id=${id}`)).toBe('0')
    const batch = await call('acceptance_student', '/enrollments/batch', 'POST', { teachingClassIds: [id, '999999', id] })
    expect(batch.body.data).toMatchObject({ successCount: 1, failureCount: 2 })
    expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE trace_id='${batch.body.traceId}' AND action='ENROLL_BATCH_FAILED' AND result='FAIL'`)).toBe('2')
    const enrollmentId = batch.body.data.successes[0].id
    expect((await call('acceptance_student', '/enrollments/check', 'POST', { teachingClassId: id })).body.data.reasonCode).toBe(50101)
    const classes = await call('teacher1', '/my/teaching-classes?termId=9001')
    expect(classes.body.data.some((c: any) => c.id === id)).toBe(true)
    const students = await call('teacher1', `/my/teaching-classes/${id}/students?size=1&keyword=acceptance_student`)
    expect(students.body.data.total).toBe(1)
    expect(students.body.data.records[0]).toMatchObject({ studentId: '9003', enrollStatus: 'ENROLLED', hasConflict: false, majorName: expect.any(String) })
    expect((await call('acceptance_teacher', `/my/teaching-classes/${id}/roster`)).status).toBe(403)
    expect((await call('admin', '/my/teaching-classes')).status).toBe(403)
    const timetable = (await call('acceptance_student', '/my/timetable?termId=9001&week=1')).body.data
    expect(timetable.grid).toHaveLength(7); expect(timetable.grid[6]).toHaveLength(12)
    expect(timetable.grid[6][10].some((c: any) => c.teachingClassId === id)).toBe(true)
    expect(timetable.courses.some((c: any) => c.enrollmentId === enrollmentId)).toBe(true)
    expect(timetable.term.id).toBe('9001'); expect(timetable.conflictedEnrollmentIds).toEqual([])
    expect((await call('acceptance_student', `/my/enrollments?courseId=9001&keyword=标准链路`)).body.data.some((e: any) => e.id === enrollmentId)).toBe(true)
    expect((await call('acceptance_student', '/my/enrollments/statistics?termId=9001')).body.data.totalEnrolledCredits).toBeGreaterThanOrEqual(3)
    const stranger = await call('admin', `/enrollments/${enrollmentId}`, 'DELETE')
    expect(stranger.status).toBe(403)
    const withdrew = await call('acceptance_student', `/enrollments/${enrollmentId}`, 'DELETE')
    expect(withdrew.body.data).toMatchObject({ teachingClassId: id, enrolledCount: 0, remaining: 10 })
    expect((await call('acceptance_student', `/enrollments/${enrollmentId}`, 'DELETE')).body.message).toBe('已退课')
    expect((await call('acceptance_student', '/my/enrollments?termId=9001&status=WITHDRAWN')).body.data.some((e: any) => e.id === enrollmentId)).toBe(true)
    const enrolled = await call('acceptance_student', '/enrollments', 'POST', { teachingClassId: id })
    expect(enrolled.body.data.enrollment.id).toBe(enrollmentId)
    expect(enrolled.body.data.teachingClass).toMatchObject({ id, remaining: 9 })
    const item = { enrollmentId, regularScore: 80, midtermScore: 80, finalScore: 80 }
    expect((await call('teacher1', `/teaching-classes/${id}/grades`, 'POST', { items: [item, { ...item, enrollmentId: '999999' }] })).status).toBe(403)
    expect(database(`SELECT COUNT(*) FROM grade WHERE teaching_class_id=${id}`)).toBe('0')
    expect((await call('teacher1', `/teaching-classes/${id}/grades`, 'POST', { items: [item] })).status).toBe(200)
    const gradeId = (await call('teacher1', `/my/teaching-classes/${id}/roster`)).body.data[0].id
    expect((await call('teacher1', `/grades/${gradeId}`, 'PUT', { finalScore: 90 })).status).toBe(200)
    expect((await call('teacher1', `/teaching-classes/${id}/grades/publish`, 'POST', { confirm: false })).status).toBe(400)
    expect((await call('teacher1', `/teaching-classes/${id}/grades/publish`, 'POST', { confirm: true })).status).toBe(200)
    expect((await call('teacher1', `/teaching-classes/${id}/grades/publish`, 'POST', { confirm: true })).body.message).toBe('成绩已发布')
    expect((await call('teacher1', `/grades/${gradeId}`, 'PUT', { finalScore: 70 })).body.code).toBe(60103)
    expect((await call('acceptance_student', '/my/grades?termId=9001')).body.data.some((g: any) => g.id === gradeId && Number(g.totalScore) === 84)).toBe(true)
    expect((await call('admin', `/teaching-classes/${id}/grades/unpublish`, 'POST', { reason: '标准接口验收解锁' })).status).toBe(200)
    expect((await call('acceptance_student', '/my/grades?termId=9001')).body.data.some((g: any) => g.id === gradeId)).toBe(false)
    const old = await page.request.delete(`/api/enrollments/${id}`, { headers: { Authorization: `Bearer ${tokens.acceptance_student}` } })
    expect(old.status()).toBe(200)
    expect(database(`SELECT status FROM enrollment WHERE id=${enrollmentId}`)).toBe('WITHDRAWN')
  } finally {
    await call('admin', `/teaching-classes/${id}/grades/unpublish`, 'POST', { reason: '隔离验收结束清理' })
    await call('admin', `/teaching-classes/${id}/status`, 'PUT', { action: 'CANCEL', reason: '隔离验收结束清理' })
  }
})

import type { Page } from '@playwright/test'

const term = {
  id: 1, code: '2026-1', name: '2026 学年第一学期',
  startDate: '2026-09-01', endDate: '2027-01-20',
  enrollStart: '2026-09-01 08:00:00', enrollEnd: '2026-12-01 18:00:00',
  withdrawEnd: '2026-12-15 18:00:00', status: 'ENROLLING',
}
const classroom = {
  id: 1, courseId: 1, courseCode: 'CS101', courseName: '数据结构', termId: 1,
  teacherId: 2, teacherName: '测试教师', className: '数据结构 01 班',
  capacity: 40, enrolledCount: 1, credit: 4, startWeek: 1, endWeek: 16,
  status: 'PUBLISHED', schedules: [],
}
const configs = [
  { id: 1, configKey: 'grade.weight.regular', configValue: '0.3', valueType: 'NUMBER', editable: 1 },
  { id: 2, configKey: 'grade.weight.midterm', configValue: '0.3', valueType: 'NUMBER', editable: 1 },
  { id: 3, configKey: 'grade.weight.final', configValue: '0.4', valueType: 'NUMBER', editable: 1 },
]
const emptyPage = { records: [], total: 0, page: 1, size: 20 }

export async function mockApi(page: Page) {
  let username = 'student1'
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace(/^\/api/, '')
    let data: unknown = []
    if (path === '/auth/login') {
      username = String(request.postDataJSON()?.username || 'student1')
      data = 'test-token'
    } else if (path === '/auth/me') {
      const userType = username === 'admin' ? 'ADMIN' : username === 'teacher1' ? 'TEACHER' : 'STUDENT'
      data = { id: userType === 'ADMIN' ? 3 : userType === 'TEACHER' ? 2 : 1,
        username, realName: userType === 'ADMIN' ? '测试管理员' : userType === 'TEACHER' ? '测试教师' : '测试学生',
        userType, status: 'ACTIVE', mustChangePwd: false }
    } else if (path === '/auth/permissions') data = []
    else if (path === '/terms') data = [term]
    else if (path === '/configs') data = configs
    else if (path === '/teaching-classes/my') data = [classroom]
    else if (path === '/teaching-classes') data = { records: [classroom], total: 1, page: 1, size: 20 }
    else if (path.startsWith('/teaching-classes/')) data = classroom
    else if (path === '/enrollments/my' || path === '/enrollments/my/schedule' || path.includes('/students')) data = []
    else if (path === '/grades/my' || path.startsWith('/grades/class/')) data = []
    else if (path === '/stats/enrollment/overview') data = { termId: 1, classCount: 1, totalCapacity: 40, totalEnrollments: 1, studentCount: 3, avgFillRate: 2.5 }
    else if (path === '/stats/enrollment/by-course') data = [{ courseId: 1, courseName: '数据结构', capacity: 40, enrolledCount: 1, fillRate: 2.5 }]
    else if (path === '/stats/capacity-analysis') data = { fullClasses: [], idleClasses: [classroom], normalClasses: [] }
    else if (path.includes('/summary')) data = { studentId: 1, termId: 1, totalEnrolledCredits: 0, earnedCredits: 0, gpa: 0, courseCount: 0, passCount: 0, failCount: 0 }
    else if (path === '/users' || path === '/courses' || path === '/audit-logs' || path === '/login-logs') data = emptyPage
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, message: 'OK', data }) })
  })
}

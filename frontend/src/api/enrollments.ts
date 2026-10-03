import { request } from './request'
import type { EnrollmentItem, TeachingClassItem } from './types'

// E-01: 选课
export function enrollClass(teachingClassId: number) {
  return request<EnrollmentItem>({
    url: '/enrollments',
    method: 'POST',
    data: { teachingClassId },
  })
}

// E-02: 退课
export function withdrawClass(teachingClassId: number) {
  return request<void>({
    url: `/enrollments/${teachingClassId}`,
    method: 'DELETE',
  })
}

// E-03: 学生本人的已选课程
export function getMyEnrollments(termId?: number) {
  return request<EnrollmentItem[]>({
    url: '/enrollments/my',
    method: 'GET',
    params: { termId },
  })
}

// E-04: 我的课表
export function getMySchedule(termId?: number) {
  return request<TeachingClassItem[]>({
    url: '/enrollments/my/schedule',
    method: 'GET',
    params: { termId },
  })
}

// E-05: 选课预检
export function precheckEnroll(teachingClassId: number) {
  return request<{ eligible: boolean; reasons: string[] }>({
    url: `/enrollments/precheck/${teachingClassId}`,
    method: 'GET',
  })
}

// E-06: 教学班学生名单 (教师/管理员)
export function getClassStudents(teachingClassId: number) {
  return request<EnrollmentItem[]>({
    url: `/enrollments/class/${teachingClassId}/students`,
    method: 'GET',
  })
}

// E-08: 管理员代选课
export function adminEnroll(studentId: number, teachingClassId: number) {
  return request<EnrollmentItem>({
    url: '/enrollments/admin-enroll',
    method: 'POST',
    data: { studentId, teachingClassId },
  })
}

// Compatibility exports
export const enrollClassApi = enrollClass
export const withdrawClassApi = withdrawClass
export const getMyEnrollmentsApi = getMyEnrollments
export const getMyScheduleApi = getMySchedule
export const precheckEnrollApi = precheckEnroll
export const adminEnrollApi = adminEnroll

import { request } from './request'
import type { StatsOverview, StudentCreditSummary } from './types'

// S-01: 选课总览 (STA)
export function getEnrollmentOverview(termId?: number) {
  return request<StatsOverview>({
    url: '/stats/enrollment/overview',
    method: 'GET',
    params: { termId },
  })
}

// S-02: 按课程统计 (STA)
export function getStatsByCourse(params?: { termId?: number; deptId?: number; limit?: number }) {
  return request<{ courseId: number; courseName: string; capacity: number; enrolledCount: number; fillRate: number }[]>({
    url: '/stats/enrollment/by-course',
    method: 'GET',
    params,
  })
}

// S-04: 满员度分析 (A)
export function getCapacityAnalysis(termId?: number) {
  return request<{ fullClasses: CapacityClass[]; idleClasses: CapacityClass[]; normalClasses: CapacityClass[] }>({
    url: '/stats/capacity-analysis',
    method: 'GET',
    params: { termId },
  })
}

export interface CapacityClass {
  id: number
  className: string
  courseName: string
  teacherName: string | null
  capacity: number
  enrolledCount: number
}

// S-06: 学生学分与修读明细 (S本人 / A)
export function getStudentSummary(studentId: number, termId?: number) {
  return request<StudentCreditSummary>({
    url: `/stats/students/${studentId}/summary`,
    method: 'GET',
    params: { termId },
  })
}

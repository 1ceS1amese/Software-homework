import { request } from './request'
import type { GradeItem } from './types'

// G-01: 获取教学班学生成绩列表 (教师/管理员)
export function getClassGrades(teachingClassId: number) {
  return request<GradeItem[]>({
    url: `/grades/class/${teachingClassId}`,
    method: 'GET',
  })
}

// G-02: 暂存/批量保存成绩 (教师)
export function saveGrades(teachingClassId: number, grades: Partial<GradeItem>[]) {
  return request<void>({
    url: `/grades/class/${teachingClassId}`,
    method: 'PUT',
    data: grades,
  })
}

// G-03: 正式发布成绩 (教师/管理员)
export function publishGrades(teachingClassId: number) {
  return request<void>({
    url: `/grades/class/${teachingClassId}/publish`,
    method: 'POST',
  })
}

// G-04: 解锁成绩 (管理员需提供 reason ≥ 5 字)
export function unlockGrades(teachingClassId: number, reason: string) {
  return request<void>({
    url: `/grades/class/${teachingClassId}/unlock`,
    method: 'POST',
    data: { reason },
  })
}

// G-05: 学生查看本人成绩
export function getMyGrades(termId?: number) {
  return request<GradeItem[]>({
    url: '/grades/my',
    method: 'GET',
    params: { termId },
  })
}

// G-06: 教学班成绩分数段分布
export function getClassGradeDistribution(teachingClassId: number) {
  return request<{ bucket: string; count: number }[]>({
    url: `/grades/class/${teachingClassId}/distribution`,
    method: 'GET',
  })
}

// Compatibility exports
export const getClassGradesApi = getClassGrades
export const saveGradesApi = saveGrades
export const publishGradesApi = publishGrades
export const unlockGradesApi = unlockGrades
export const getMyGradesApi = getMyGrades

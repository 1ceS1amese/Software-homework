import { request } from './request'
import type { PageResult, TeachingClassItem, ClassScheduleItem } from './types'

// C-01: 查询教学班列表（含筛选）
export function getTeachingClasses(params: {
  termId?: number
  courseId?: number
  teacherId?: number
  keyword?: string
  status?: string
  onlyAvailable?: boolean
  myMajorOnly?: boolean
  page?: number
  size?: number
}) {
  return request<PageResult<TeachingClassItem>>({
    url: '/teaching-classes',
    method: 'GET',
    params,
  })
}

// C-02: 教学班详情
export function getTeachingClassDetail(id: number) {
  return request<TeachingClassItem>({
    url: `/teaching-classes/${id}`,
    method: 'GET',
  })
}

export function getMyTeachingClasses(termId?: number) {
  return request<TeachingClassItem[]>({
    url: '/teaching-classes/my',
    method: 'GET',
    params: { termId },
  })
}

// C-03: 创建教学班
export function createTeachingClass(data: Partial<TeachingClassItem>) {
  return request<TeachingClassItem>({
    url: '/teaching-classes',
    method: 'POST',
    data,
  })
}

// C-04: 更新教学班
export function updateTeachingClass(id: number, data: Partial<TeachingClassItem>) {
  return request<TeachingClassItem>({
    url: `/teaching-classes/${id}`,
    method: 'PUT',
    data,
  })
}

// C-05: 删除/取消教学班
export function deleteTeachingClass(id: number) {
  return request<void>({
    url: `/teaching-classes/${id}`,
    method: 'DELETE',
  })
}

// C-06: 状态流转 (PUBLISH, CLOSE, CANCEL, ROLLBACK_DRAFT)
export function updateTeachingClassStatus(id: number, action: string, reason?: string) {
  return request<void>({
    url: `/teaching-classes/${id}/status`,
    method: 'PUT',
    data: { action, reason },
  })
}

// C-07: 保存/更新时间段
export function updateClassSchedules(id: number, schedules: ClassScheduleItem[]) {
  return request<void>({
    url: `/teaching-classes/${id}/schedules`,
    method: 'PUT',
    data: schedules,
  })
}

// Compatibility exports
export const getTeachingClassesApi = getTeachingClasses
export const getTeachingClassDetailApi = getTeachingClassDetail
export const createTeachingClassApi = createTeachingClass
export const updateTeachingClassApi = updateTeachingClass
export const updateTeachingClassStatusApi = updateTeachingClassStatus

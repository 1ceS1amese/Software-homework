import { request } from './client'
import type { TeachingClassItem } from './types'

// C-01: 查询教学班列表（含筛选）
export function getTeachingClassesApi(params: {
  termId?: number
  courseId?: number
  teacherId?: number
  keyword?: string
  status?: string
  page?: number
  size?: number
}) {
  return request<{ records: TeachingClassItem[]; total: number }>({
    url: '/teaching-classes',
    method: 'GET',
    params,
  })
}

// C-02: 教学班详情
export function getTeachingClassDetailApi(id: number) {
  return request<TeachingClassItem>({
    url: `/teaching-classes/${id}`,
    method: 'GET',
  })
}

// C-03: 创建教学班
export function createTeachingClassApi(data: Partial<TeachingClassItem>) {
  return request<TeachingClassItem>({
    url: '/teaching-classes',
    method: 'POST',
    data,
  })
}

// C-04: 更新教学班
export function updateTeachingClassApi(id: number, data: Partial<TeachingClassItem>) {
  return request<TeachingClassItem>({
    url: `/teaching-classes/${id}`,
    method: 'PUT',
    data,
  })
}

// C-07: 变更教学班状态 (PUBLISH, CLOSE, CANCEL, ROLLBACK_DRAFT)
export function updateTeachingClassStatusApi(id: number, action: string, reason?: string) {
  return request<void>({
    url: `/teaching-classes/${id}/status`,
    method: 'PUT',
    data: { action, reason },
  })
}

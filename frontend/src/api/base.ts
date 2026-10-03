import { request } from './request'
import type { DeptItem, MajorItem, TermItem, CourseItem, SysConfigItem, PageResult } from './types'

// Depts (B-01..B-04)
export function getDepts(params?: { status?: string }) {
  return request<DeptItem[]>({
    url: '/depts',
    method: 'GET',
    params,
  })
}

export function createDept(data: Partial<DeptItem>) {
  return request<DeptItem>({
    url: '/depts',
    method: 'POST',
    data,
  })
}

export function updateDept(id: number, data: Partial<DeptItem>) {
  return request<DeptItem>({
    url: `/depts/${id}`,
    method: 'PUT',
    data,
  })
}

export function deleteDept(id: number) {
  return request<void>({
    url: `/depts/${id}`,
    method: 'DELETE',
  })
}

// Majors (B-05..B-08)
export function getMajors(params?: { deptId?: number; status?: string }) {
  return request<MajorItem[]>({
    url: '/majors',
    method: 'GET',
    params,
  })
}

export function createMajor(data: Partial<MajorItem>) {
  return request<MajorItem>({
    url: '/majors',
    method: 'POST',
    data,
  })
}

export function updateMajor(id: number, data: Partial<MajorItem>) {
  return request<MajorItem>({
    url: `/majors/${id}`,
    method: 'PUT',
    data,
  })
}

// Terms (B-09..B-13)
export function getTerms() {
  return request<TermItem[]>({
    url: '/terms',
    method: 'GET',
  })
}

export function getCurrentTerm() {
  return request<TermItem>({
    url: '/terms/current',
    method: 'GET',
  })
}

export function createTerm(data: Partial<TermItem>) {
  return request<TermItem>({
    url: '/terms',
    method: 'POST',
    data,
  })
}

export function updateTermStatus(id: number, status: string) {
  return request<void>({
    url: `/terms/${id}/status`,
    method: 'PUT',
    data: { status },
  })
}

// Courses (B-14..B-20)
export function getCourses(params?: { deptId?: number; keyword?: string; page?: number; size?: number }) {
  return request<PageResult<CourseItem>>({
    url: '/courses',
    method: 'GET',
    params,
  })
}

export function createCourse(data: Partial<CourseItem>) {
  return request<CourseItem>({
    url: '/courses',
    method: 'POST',
    data,
  })
}

export function updateCourse(id: number, data: Partial<CourseItem>) {
  return request<CourseItem>({
    url: `/courses/${id}`,
    method: 'PUT',
    data,
  })
}

export function deleteCourse(id: number) {
  return request<void>({
    url: `/courses/${id}`,
    method: 'DELETE',
  })
}

// Sys Configs
export function getConfigs() {
  return request<SysConfigItem[]>({
    url: '/configs',
    method: 'GET',
  })
}

export function updateConfig(id: number, configValue: string) {
  return request<void>({
    url: `/configs/${id}`,
    method: 'PUT',
    data: { configValue },
  })
}

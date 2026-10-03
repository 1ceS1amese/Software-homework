import { request } from './request'
import type { PageResult, UserItem, UserProfile } from './types'

// U-01: 分页查询用户
export function getUsers(params: {
  role?: string
  deptId?: number
  majorId?: number
  status?: string
  keyword?: string
  page?: number
  size?: number
}) {
  return request<PageResult<UserItem>>({
    url: '/users',
    method: 'GET',
    params,
  })
}

// U-02: 新增用户
export function createUser(data: Partial<UserItem>) {
  return request<UserItem>({
    url: '/users',
    method: 'POST',
    data,
  })
}

// U-03: 用户详情
export function getUserDetail(id: number) {
  return request<UserItem>({
    url: `/users/${id}`,
    method: 'GET',
  })
}

// U-04: 更新用户信息
export function updateUser(id: number, data: Partial<UserItem>) {
  return request<UserItem>({
    url: `/users/${id}`,
    method: 'PUT',
    data,
  })
}

// U-05: 逻辑删除用户
export function deleteUser(id: number) {
  return request<void>({
    url: `/users/${id}`,
    method: 'DELETE',
  })
}

// U-06: 重置密码
export function resetUserPassword(id: number) {
  return request<{ initialPassword: string }>({
    url: `/users/${id}/password`,
    method: 'PUT',
  })
}

// U-07: 启停用用户
export function updateUserStatus(id: number, status: string) {
  return request<void>({
    url: `/users/${id}/status`,
    method: 'PUT',
    data: { status },
  })
}

// U-08: 分配角色
export function updateUserRoles(id: number, roleCodes: string[]) {
  return request<void>({
    url: `/users/${id}/roles`,
    method: 'PUT',
    data: { roleCodes },
  })
}

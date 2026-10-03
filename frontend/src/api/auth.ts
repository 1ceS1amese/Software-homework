import { request } from './request'
import type { LoginPayload, PasswordChangePayload, UserProfile } from './types'

// A-01: 登录
export function login(payload: LoginPayload) {
  return request<string>({
    url: '/auth/login',
    method: 'POST',
    data: payload,
  })
}

// A-02: 登出
export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'POST',
  })
}

// A-03: 获取当前登录人信息
export function getMe() {
  return request<UserProfile>({
    url: '/auth/me',
    method: 'GET',
  })
}

// A-04: 修改密码
export function changePassword(payload: PasswordChangePayload) {
  return request<void>({
    url: '/auth/password',
    method: 'PUT',
    data: payload,
  })
}

// A-05: 权限码
export function getPermissions() {
  return request<string[]>({
    url: '/auth/permissions',
    method: 'GET',
  })
}

// Compatibility exports
export const loginApi = login
export const logoutApi = logout
export const getMeApi = getMe
export const changePasswordApi = changePassword

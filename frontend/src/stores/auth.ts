import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getMe as getMeApi, changePassword as changePasswordApi, getPermissions as getPermissionsApi } from '@/api/auth'
import type { LoginPayload, PasswordChangePayload, UserProfile } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('csms_token') || '')
  
  const getInitialUser = (): UserProfile | null => {
    try {
      const cached = localStorage.getItem('csms_user')
      return cached ? JSON.parse(cached) : null
    } catch {
      return null
    }
  }
  const user = ref<UserProfile | null>(getInitialUser())
  const permissions = ref<string[]>([])
  const loaded = ref(false)

  const isAuthenticated = computed(() => !!token.value)
  const userType = computed(() => user.value?.userType || '')
  const isStudent = computed(() => userType.value === 'STUDENT')
  const isTeacher = computed(() => userType.value === 'TEACHER')
  const isAdmin = computed(() => userType.value === 'ADMIN')
  const mustChangePwd = computed(() => !!user.value?.mustChangePwd)

  function hasPerm(code: string): boolean {
    if (isAdmin.value) return true // 管理员拥有全权限
    return permissions.value.includes(code)
  }

  async function login(payload: LoginPayload) {
    const jwtToken = await loginApi(payload)
    token.value = jwtToken
    localStorage.setItem('csms_token', jwtToken)
    await fetchMe()
    return user.value
  }

  function clearSession() {
    token.value = ''
    user.value = null
    permissions.value = []
    loaded.value = false
    localStorage.removeItem('csms_token')
    localStorage.removeItem('csms_user')
  }

  async function fetchMe() {
    try {
      const profile = await getMeApi()
      user.value = profile
      localStorage.setItem('csms_user', JSON.stringify(profile))
      try {
        const perms = await getPermissionsApi()
        permissions.value = perms || []
      } catch {
        // default fallback
        permissions.value = profile.permissions || []
      }
      loaded.value = true
      return profile
    } catch (e) {
      // Token 已失效：只清理本地状态，不再调用登出接口
      // （那次请求同样会 401，导致重复的重定向处理）
      clearSession()
      throw e
    }
  }

  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch {
      // ignore
    } finally {
      clearSession()
    }
  }

  async function changePassword(payload: PasswordChangePayload) {
    await changePasswordApi(payload)
    await logout()
  }

  return {
    token,
    user,
    permissions,
    loaded,
    isAuthenticated,
    userType,
    isStudent,
    isTeacher,
    isAdmin,
    mustChangePwd,
    hasPerm,
    login,
    fetchMe,
    logout,
    clearSession,
    changePassword,
  }
})

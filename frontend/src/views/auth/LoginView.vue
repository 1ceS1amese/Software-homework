<template>
  <div>
    <header>
      <p class="text-xs font-semibold text-(--csms-accent)">统一身份认证</p>
      <h2 class="mt-3 text-2xl font-semibold text-(--csms-ink) text-balance sm:text-3xl">欢迎回来</h2>
      <p class="mt-2 text-sm leading-6 text-(--csms-muted) text-pretty">请使用校园账号登录，继续你的教务工作。</p>
    </header>

    <form class="mt-7 space-y-5" @submit.prevent="handleLogin">
      <UFormField label="用户名" name="username" :error="errors.username || undefined">
        <UInput v-model="form.username" class="w-full" size="lg" :ui="{ base: 'min-h-11' }" autocomplete="username" placeholder="例如 student1" :disabled="loading" />
      </UFormField>
      <UFormField label="登录密码" name="password" :error="errors.password || undefined">
        <UInput v-model="form.password" class="w-full" size="lg" :ui="{ base: 'min-h-11' }" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="loading" />
      </UFormField>
      <UButton type="submit" size="lg" block class="min-h-11 justify-center font-medium" :loading="loading">登录系统</UButton>
    </form>

    <div class="mt-7 border-t border-(--csms-line) pt-5">
      <div class="text-xs text-(--csms-muted)">快速填入演示账号 · 初始密码 123456</div>
      <div class="mt-3 flex flex-wrap gap-2">
        <UButton v-for="account in demoAccounts" :key="account.username" :color="form.username === account.username ? 'primary' : 'neutral'" :variant="form.username === account.username ? 'soft' : 'outline'" size="sm" class="min-h-11 px-4" @click="fillAccount(account.username)">{{ account.label }}</UButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/composables'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })
const demoAccounts = [
  { username: 'student1', label: '学生' },
  { username: 'teacher1', label: '教师' },
  { username: 'admin', label: '管理员' },
]

function fillAccount(username: string) {
  form.username = username
  form.password = '123456'
  errors.username = ''
  errors.password = ''
}

function homeOf(userType?: string) {
  if (userType === 'STUDENT') return '/student/courses'
  if (userType === 'TEACHER') return '/teacher/classes'
  if (userType === 'ADMIN') return '/admin/dashboard'
  return '/profile'
}

function resolveRedirect(): string | null {
  const raw = route.query.redirect
  const value = Array.isArray(raw) ? raw[0] : raw
  if (!value || typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/login')) return null
  return value
}

async function handleLogin() {
  errors.username = form.username.trim() ? '' : '请输入用户名'
  errors.password = form.password ? '' : '请输入密码'
  if (errors.username || errors.password || loading.value) return
  loading.value = true
  try {
    const user = await authStore.login({ username: form.username.trim(), password: form.password })
    toast.add({ title: '登录成功', color: 'success' })
    await router.replace(user?.mustChangePwd ? '/profile/password' : (resolveRedirect() ?? homeOf(user?.userType)))
  } catch {
    // 请求层展示服务端错误；保留输入供用户修改。
  } finally {
    loading.value = false
  }
}
</script>

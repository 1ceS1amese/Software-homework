<template>
  <div class="min-h-screen bg-slate-50 flex items-center justify-center p-6 text-center">
    <div class="max-w-md bg-white p-8 rounded-2xl shadow-sm border border-slate-200 space-y-4">
      <div class="text-6xl">🚫</div>
      <h1 class="text-2xl font-bold text-slate-900">403 权限受限</h1>
      <p class="text-sm text-slate-500">
        抱歉，您的角色权限不足以访问此页面。教务系统已阻断此次未授权访问。
      </p>
      <div class="pt-4">
        <UButton @click="handleBack">返回首页</UButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleBack() {
  if (authStore.isStudent) router.push('/student/courses')
  else if (authStore.isTeacher) router.push('/teacher/classes')
  else if (authStore.isAdmin) router.push('/admin/dashboard')
  else router.push('/login')
}
</script>

<template>
  <PageContainer title="修改登录密码" description="新密码不少于 8 位，须同时包含字母与数字。">
    <form class="max-w-lg space-y-5 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-6" @submit.prevent="handleSubmit">
      <UAlert v-if="authStore.mustChangePwd" color="warning" variant="soft" icon="i-lucide-shield-alert" title="首次登录，请修改密码" description="请先输入当前初始密码，再设置新密码。" />
      <UFormField label="当前密码" name="oldPassword" :error="errors.oldPassword">
        <UInput v-model="form.oldPassword" type="password" autocomplete="current-password" class="w-full" placeholder="请输入当前密码" />
      </UFormField>
      <UFormField label="新密码" name="newPassword" :error="errors.newPassword">
        <UInput v-model="form.newPassword" type="password" autocomplete="new-password" class="w-full" placeholder="至少 8 位，包含字母和数字" />
      </UFormField>
      <UFormField label="确认新密码" name="confirmPassword" :error="errors.confirmPassword">
        <UInput v-model="form.confirmPassword" type="password" autocomplete="new-password" class="w-full" placeholder="再次输入新密码" />
      </UFormField>
      <UButton type="submit" block :loading="loading">确认修改并重新登录</UButton>
    </form>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/composables'
import { useAuthStore } from '@/stores/auth'
import PageContainer from '@/components/PageContainer.vue'

const authStore = useAuthStore()
const router = useRouter()
const toast = useToast()
const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const errors = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
async function handleSubmit() {
  errors.oldPassword = form.oldPassword ? '' : '请输入当前密码'
  errors.newPassword = form.newPassword.length < 8 ? '密码至少 8 位' : !/(?=.*[A-Za-z])(?=.*\d)/.test(form.newPassword) ? '须同时包含字母与数字' : ''
  errors.confirmPassword = form.confirmPassword === form.newPassword ? '' : '两次密码不一致'
  if (errors.oldPassword || errors.newPassword || errors.confirmPassword || loading.value) return
  loading.value = true
  try {
    await authStore.changePassword(form)
    toast.add({ title: '密码已修改，请重新登录', color: 'success' })
    await router.replace('/login')
  } catch { /* 请求层已提示 */ }
  finally { loading.value = false }
}
</script>

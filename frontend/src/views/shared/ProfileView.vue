<template>
  <PageContainer title="个人中心" description="查看当前账号的档案与身份信息。">
    <div class="max-w-3xl space-y-5">
      <section class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
        <div class="flex items-center gap-4">
          <UAvatar :text="authStore.user?.realName?.slice(0, 1) || 'U'" size="3xl" />
          <div><h2 class="text-lg font-semibold">{{ authStore.user?.realName || '未设置姓名' }}</h2><p class="text-sm text-(--ui-text-muted)">{{ authStore.user?.username || '—' }}</p></div>
          <UBadge class="ml-auto" :color="authStore.user?.status === 'ACTIVE' ? 'success' : 'error'" variant="subtle">{{ authStore.user?.status === 'ACTIVE' ? '正常' : '停用' }}</UBadge>
        </div>
      </section>
      <section class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
        <h2 class="font-semibold">档案信息</h2>
        <dl class="mt-4 grid gap-4 sm:grid-cols-2">
          <div v-for="item in fields" :key="item.label"><dt class="text-xs text-(--ui-text-muted)">{{ item.label }}</dt><dd class="mt-1 text-sm">{{ item.value }}</dd></div>
        </dl>
      </section>
      <UButton to="/profile/password" color="neutral" variant="outline" icon="i-lucide-key-round">修改密码</UButton>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import PageContainer from '@/components/PageContainer.vue'

const authStore = useAuthStore()
const fields = computed(() => [
  { label: '用户角色', value: ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' } as Record<string, string>)[authStore.user?.userType || ''] || '—' },
  { label: '性别', value: authStore.user?.gender === 'MALE' ? '男' : authStore.user?.gender === 'FEMALE' ? '女' : '未设置' },
  { label: '所属院系', value: authStore.user?.deptName || (authStore.user?.deptId ? `院系 #${authStore.user.deptId}` : '未归属') },
  { label: '所属专业', value: authStore.user?.majorName || (authStore.user?.majorId ? `专业 #${authStore.user.majorId}` : '未分配') },
  { label: '联系电话', value: authStore.user?.phone || '未绑定' },
  { label: '邮箱', value: authStore.user?.email || '未绑定' },
])
</script>

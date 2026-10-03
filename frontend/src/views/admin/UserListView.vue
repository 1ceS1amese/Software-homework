<template>
  <PageContainer title="用户管理" description="查询账号并启停用；新增、编辑和密码重置接口尚未由后端提供。">
    <div class="space-y-5">
      <div class="flex flex-wrap items-end gap-3 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-4">
        <UFormField label="学工号或姓名" class="min-w-48 flex-1 sm:max-w-70"><UInput v-model="query.keyword" icon="i-lucide-search" class="w-full" placeholder="输入关键词" @keyup.enter="search" /></UFormField>
        <UFormField label="身份"><USelect v-model="query.role" :items="roleOptions" class="w-36" /></UFormField>
        <UFormField label="状态"><USelect v-model="query.status" :items="statusOptions" class="w-32" /></UFormField>
        <UButton icon="i-lucide-search" :loading="loading" @click="search">查询</UButton>
      </div>
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="用户加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="users" :columns="columns" :loading="loading" class="min-w-180">
          <template #username-cell="{ row }"><span class="font-mono font-semibold">{{ row.original.username }}</span></template>
          <template #role-cell="{ row }"><UBadge color="neutral" variant="subtle">{{ roleLabel(row.original.userType) }}</UBadge></template>
          <template #phone-cell="{ row }">{{ maskPhone(row.original.phone) }}</template>
          <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="user" /></template>
          <template #actions-cell="{ row }"><UButton size="sm" :color="row.original.status === 'ACTIVE' ? 'error' : 'success'" variant="outline" @click="pendingUser = row.original">{{ row.original.status === 'ACTIVE' ? '停用' : '启用' }}</UButton></template>
        </UTable>
        <p v-if="!loading && !error && !users.length" class="p-8 text-center text-sm text-(--ui-text-muted)">没有匹配的用户。</p>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) p-4 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 人 · 第 {{ page }} 页</span>
          <div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</UButton><UButton size="sm" color="neutral" variant="outline" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</UButton></div>
        </div>
      </div>
    </div>
  </PageContainer>
  <UModal v-model:open="confirmOpen" :title="pendingUser?.status === 'ACTIVE' ? '确认停用账号' : '确认启用账号'" :description="pendingUser?.realName">
    <template #footer><UButton color="neutral" variant="outline" @click="confirmOpen = false">取消</UButton><UButton :color="pendingUser?.status === 'ACTIVE' ? 'error' : 'success'" :loading="saving" @click="changeStatus">确认</UButton></template>
  </UModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { getUsers, updateUserStatus } from '@/api/users'
import type { UserItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
const columns: TableColumn<UserItem>[] = [
  { accessorKey: 'username', header: '学工号 / 账号' }, { accessorKey: 'realName', header: '姓名' },
  { id: 'role', header: '身份' }, { accessorKey: 'deptName', header: '院系' },
  { id: 'phone', header: '联系电话' }, { id: 'status', header: '状态' }, { id: 'actions', header: '操作' },
]
const roleOptions = [
  { label: '全部角色', value: 'all' }, { label: '学生', value: 'STUDENT' },
  { label: '教师', value: 'TEACHER' }, { label: '管理员', value: 'ADMIN' },
]
const statusOptions = [
  { label: '全部状态', value: 'all' }, { label: '正常', value: 'ACTIVE' }, { label: '停用', value: 'DISABLED' },
]
const toast = useToast()
const query = reactive({ keyword: '', role: 'all', status: 'all' })
const users = ref<UserItem[]>([])
const page = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const pendingUser = ref<UserItem | null>(null)
const confirmOpen = computed({ get: () => pendingUser.value !== null, set: open => { if (!open) pendingUser.value = null } })
function roleLabel(role: string) { return ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' } as Record<string, string>)[role] || role }
function maskPhone(phone?: string) { return phone && phone.length >= 7 ? phone.slice(0, 3) + '****' + phone.slice(-4) : phone || '未登记' }
function search() { page.value = 1; void loadData() }
function changePage(next: number) { page.value = next; void loadData() }
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getUsers({
      keyword: query.keyword.trim() || undefined,
      role: query.role === 'all' ? undefined : query.role,
      status: query.status === 'all' ? undefined : query.status,
      page: page.value, size,
    })
    users.value = result.records || []
    total.value = result.total || 0
  } catch { users.value = []; total.value = 0; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
async function changeStatus() {
  if (!pendingUser.value || saving.value) return
  const next = pendingUser.value.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  saving.value = true
  try {
    await updateUserStatus(pendingUser.value.id, next)
    toast.add({ title: next === 'ACTIVE' ? '账号已启用' : '账号已停用', color: 'success' })
    pendingUser.value = null
    await loadData()
  } catch { /* 请求层已提示 */ }
  finally { saving.value = false }
}
onMounted(() => { void loadData() })
</script>

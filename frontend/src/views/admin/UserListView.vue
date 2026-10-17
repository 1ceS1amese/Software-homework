<template>
  <PageContainer title="用户管理" description="维护账号资料、角色与状态；初始密码仅显示一次，新账号及重置后必须改密。">
    <template #actions><UButton @click="edit()">新增用户</UButton></template>
    <div class="flex flex-wrap gap-3 pb-4"><UInput v-model="keyword" placeholder="输入关键词" aria-label="学工号或姓名" @keyup.enter="search" /><USelect v-model="role" :items="roleOptions" aria-label="筛选身份" /><UButton :loading="loading" @click="search">查询</UButton></div>
    <UAlert v-if="error" color="error" title="用户加载失败" :description="error" />
    <div class="overflow-x-auto rounded-xl border border-(--ui-border)"><UTable :data="users" :columns="columns" :loading="loading" class="min-w-220">
      <template #role-cell="{ row }">{{ roleLabels[row.original.userType] }}</template>
      <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="user" /></template>
      <template #actions-cell="{ row }"><div class="flex flex-wrap gap-2">
        <UButton size="sm" variant="outline" @click="edit(row.original)">编辑</UButton><UButton size="sm" variant="outline" @click="assignRole(row.original)">分配角色</UButton>
        <UButton size="sm" variant="outline" @click="pending = { row: row.original, action: 'reset' }">重置密码</UButton>
        <UButton size="sm" variant="outline" @click="pending = { row: row.original, action: 'status' }">{{ row.original.status === 'ACTIVE' ? '停用' : '启用' }}</UButton>
        <UButton size="sm" variant="outline" color="error" @click="pending = { row: row.original, action: 'delete' }">删除</UButton>
      </div></template>
    </UTable><div class="flex justify-between gap-2 p-4"><span>共 {{ total }} 人 · 第 {{ page }} 页</span><div class="flex gap-2"><UButton :disabled="page === 1" @click="page--; loadData()">上一页</UButton><UButton :disabled="page * 20 >= total" @click="page++; loadData()">下一页</UButton></div></div></div>
  </PageContainer>
  <ManagementEditor v-model:open="editorOpen" :title="roleMode ? '分配角色' : editing ? '编辑用户' : '新增用户'" :fields="fields" :initial="initial" :saving="saving" @save="save" />
  <UModal v-model:open="confirmOpen" title="确认用户操作" :description="pending?.row.realName"><template #body>请确认操作对象。停用、角色变更和密码变更将使旧登录令牌失效。</template><template #footer><UButton color="neutral" variant="outline" @click="pending = null">取消</UButton><UButton :loading="saving" @click="operate">确认操作</UButton></template></UModal>
  <UModal v-model:open="passwordOpen" title="一次性初始密码" description="请通过安全渠道交付给本人；关闭后不再显示。"><template #body><code data-testid="initial-password" class="break-all select-all text-lg">{{ initialPassword }}</code></template><template #footer><UButton @click="initialPassword = ''">已安全保存</UButton></template></UModal>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getUsers } from '@/api/users'
import { getDepts, getMajors } from '@/api/base'
import { request } from '@/api/request'
import type { DeptItem, MajorItem, UserItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ManagementEditor, { type EditorField } from '@/components/ManagementEditor.vue'
const roleLabels: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
const roles = Object.entries(roleLabels).map(([value, label]) => ({ value, label }))
const roleOptions = [{ value: 'all', label: '全部角色' }, ...roles]
const keyword = ref(''), role = ref('all'), page = ref(1), total = ref(0), users = ref<UserItem[]>([])
const depts = ref<DeptItem[]>([]), majors = ref<MajorItem[]>([])
const loading = ref(false), saving = ref(false), error = ref(''), editorOpen = ref(false), roleMode = ref(false)
const editing = ref<number | null>(null), initial = ref<Record<string, unknown>>({}), initialPassword = ref('')
const passwordOpen = computed({ get: () => !!initialPassword.value, set: v => { if (!v) initialPassword.value = '' } })
const pending = ref<{ row: UserItem; action: 'delete' | 'reset' | 'status' } | null>(null)
const confirmOpen = computed({ get: () => !!pending.value, set: v => { if (!v) pending.value = null } })
const columns: TableColumn<UserItem>[] = [{ accessorKey: 'username', header: '登录名' }, { accessorKey: 'realName', header: '姓名' }, { id: 'role', header: '身份' }, { accessorKey: 'deptName', header: '院系' }, { id: 'status', header: '状态' }, { id: 'actions', header: '操作' }]
const fields = computed<EditorField[]>(() => roleMode.value ? [{ key: 'userType', label: '身份', required: true, options: roles }] : [
  { key: 'username', label: '登录名', required: true, maxlength: 50 }, { key: 'realName', label: '姓名', required: true, maxlength: 50 },
  ...(!editing.value ? [{ key: 'userType', label: '身份', required: true, options: roles }] : []),
  { key: 'deptId', label: '所属院系', options: [{ label: '未指定', value: '' }, ...depts.value.map(d => ({ label: d.name, value: d.id }))] },
  { key: 'majorId', label: '所属专业', options: [{ label: '未指定', value: '' }, ...majors.value.map(m => ({ label: `${m.deptName} / ${m.name}`, value: m.id }))] },
  { key: 'gender', label: '性别', options: [{ label: '未登记', value: 'UNKNOWN' }, { label: '男', value: 'MALE' }, { label: '女', value: 'FEMALE' }] },
  { key: 'phone', label: '联系电话', maxlength: 20 }, { key: 'email', label: '电子邮箱', type: 'email', maxlength: 100 },
])
function edit(row?: UserItem) { roleMode.value = false; editing.value = row?.id ?? null; initial.value = row ? { ...row } : { userType: 'STUDENT', gender: 'UNKNOWN' }; editorOpen.value = true }
function assignRole(row: UserItem) { roleMode.value = true; editing.value = row.id; initial.value = { userType: row.userType }; editorOpen.value = true }
function search() { page.value = 1; void loadData() }
async function loadData() {
  loading.value = true; error.value = ''
  try { const result = await getUsers({ keyword: keyword.value.trim() || undefined, role: role.value === 'all' ? undefined : role.value, page: page.value, size: 20 }); users.value = result.records; total.value = result.total }
  catch { error.value = '请检查服务后重试。' } finally { loading.value = false }
}
async function save(values: Record<string, string>) {
  if (saving.value) return; saving.value = true
  try {
    const url = '/users' + (editing.value ? '/' + editing.value : '') + (roleMode.value ? '/roles' : '')
    const result = await request<{ initialPassword?: string }>({ url, method: editing.value ? 'PUT' : 'POST', data: roleMode.value ? { roleCodes: [values.userType] } : values })
    editorOpen.value = false; initialPassword.value = result?.initialPassword || ''; await loadData()
  } catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
async function operate() {
  if (!pending.value || saving.value) return; saving.value = true
  try {
    const { row, action } = pending.value
    const result = await request<{ initialPassword?: string }>({ url: `/users/${row.id}` + (action === 'reset' ? '/password' : action === 'status' ? '/status' : ''), method: action === 'delete' ? 'DELETE' : 'PUT', data: action === 'status' ? { status: row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' } : undefined })
    pending.value = null; initialPassword.value = result?.initialPassword || ''; await loadData()
  } catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
onMounted(async () => { await loadData(); try { [depts.value, majors.value] = await Promise.all([getDepts(), getMajors()]) } catch { /* 加载失败时后端仍校验引用 */ } })
</script>

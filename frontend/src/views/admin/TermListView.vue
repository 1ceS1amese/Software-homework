<template>
  <PageContainer title="学期管理" description="维护教学日历和选退课时间；学期依次进入选课、授课和结束阶段。">
    <template #actions><UButton @click="edit()">新增学期</UButton><UButton color="neutral" variant="outline" @click="loadData">刷新</UButton></template>
    <UAlert v-if="error" color="error" title="学期加载失败" :description="error" />
    <div class="overflow-x-auto rounded-xl border border-(--ui-border)"><UTable :data="terms" :columns="columns" :loading="loading" class="min-w-220">
      <template #dates-cell="{ row }">{{ row.original.startDate }} – {{ row.original.endDate }}</template>
      <template #enroll-cell="{ row }">{{ row.original.enrollStart }} – {{ row.original.enrollEnd }}</template>
      <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="term" /></template>
      <template #actions-cell="{ row }"><div class="flex gap-2"><UButton size="sm" variant="outline" @click="edit(row.original)">编辑</UButton><UButton v-if="next(row.original.status)" size="sm" color="warning" @click="pending = row.original">{{ next(row.original.status)?.label }}</UButton></div></template>
    </UTable></div>
  </PageContainer>
  <ManagementEditor v-model:open="editorOpen" :title="editing ? '编辑学期' : '新增学期'" :fields="fields" :initial="initial" :saving="saving" @save="save" />
  <UModal v-model:open="confirmOpen" title="确认变更学期状态" :description="pending?.name"><template #body>状态变更后不能逆向回退，请核对教学安排。</template><template #footer><UButton color="neutral" variant="outline" @click="pending = null">取消</UButton><UButton :loading="saving" @click="changeStatus">确认变更</UButton></template></UModal>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getTerms } from '@/api/base'
import { request } from '@/api/request'
import type { TermItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ManagementEditor, { type EditorField } from '@/components/ManagementEditor.vue'
const terms = ref<TermItem[]>([]), loading = ref(false), saving = ref(false), error = ref(''), editorOpen = ref(false)
const editing = ref<number | null>(null), initial = ref<Record<string, unknown>>({}), pending = ref<TermItem | null>(null)
const confirmOpen = computed({ get: () => !!pending.value, set: value => { if (!value) pending.value = null } })
const columns: TableColumn<TermItem>[] = [{ accessorKey: 'code', header: '学期代码' }, { accessorKey: 'name', header: '学期名称' }, { id: 'dates', header: '学期起止' }, { id: 'enroll', header: '选课时间' }, { id: 'status', header: '状态' }, { id: 'actions', header: '操作' }]
const fields: EditorField[] = [
  { key: 'code', label: '学期代码', required: true, maxlength: 32 }, { key: 'name', label: '学期名称', required: true, maxlength: 64 },
  { key: 'startDate', label: '开始日期', type: 'date', required: true }, { key: 'endDate', label: '结束日期', type: 'date', required: true },
  { key: 'enrollStart', label: '选课开始', type: 'datetime-local', required: true }, { key: 'enrollEnd', label: '选课结束', type: 'datetime-local', required: true },
  { key: 'withdrawEnd', label: '退课截止', type: 'datetime-local', required: true },
]
function next(status: string) { return ({ PLANNED: { status: 'ENROLLING', label: '开放选课' }, ENROLLING: { status: 'RUNNING', label: '开始授课' }, RUNNING: { status: 'CLOSED', label: '结束学期' } } as Record<string, { status: string; label: string }>)[status] }
function edit(row?: TermItem) { editing.value = row?.id ?? null; initial.value = row ? Object.fromEntries(Object.entries(row).map(([key, value]) => [key, typeof value === 'string' ? value.replace(' ', 'T') : value])) : {}; editorOpen.value = true }
async function loadData() { loading.value = true; error.value = ''; try { terms.value = await getTerms() } catch { error.value = '请检查服务后重试。' } finally { loading.value = false } }
async function save(data: Record<string, string>) {
  if (saving.value) return; saving.value = true
  try { await request({ url: '/terms' + (editing.value ? '/' + editing.value : ''), method: editing.value ? 'PUT' : 'POST', data }); editorOpen.value = false; await loadData() } catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
async function changeStatus() {
  if (!pending.value || saving.value) return; saving.value = true
  try { await request({ url: `/terms/${pending.value.id}/status`, method: 'PUT', data: { status: next(pending.value.status)?.status } }); pending.value = null; await loadData() } catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
onMounted(loadData)
</script>

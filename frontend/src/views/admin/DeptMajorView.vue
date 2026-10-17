<template>
  <PageContainer title="院系与专业" description="维护院系与专业；存在关联数据时禁止删除。">
    <template #actions><UButton @click="edit('dept')">新增院系</UButton><UButton @click="edit('major')">新增专业</UButton></template>
    <UAlert v-if="error" color="error" title="基础数据加载失败" :description="error" />
    <div class="grid min-w-0 gap-5 xl:grid-cols-2">
      <section class="min-w-0 overflow-x-auto rounded-xl border border-(--ui-border)">
        <h2 class="p-4 font-semibold">院系 · {{ depts.length }}</h2>
        <UTable :data="depts" :columns="deptColumns" :loading="loading" class="min-w-120">
          <template #actions-cell="{ row }"><div class="flex gap-2"><UButton size="sm" variant="outline" @click="edit('dept', row.original)">编辑</UButton><UButton size="sm" color="error" variant="outline" @click="pending = { kind: 'dept', row: row.original }">删除</UButton></div></template>
        </UTable>
      </section>
      <section class="min-w-0 overflow-x-auto rounded-xl border border-(--ui-border)">
        <h2 class="p-4 font-semibold">专业 · {{ majors.length }}</h2>
        <UTable :data="majors" :columns="majorColumns" :loading="loading" class="min-w-120">
          <template #actions-cell="{ row }"><div class="flex gap-2"><UButton size="sm" variant="outline" @click="edit('major', row.original)">编辑</UButton><UButton size="sm" color="error" variant="outline" @click="pending = { kind: 'major', row: row.original }">删除</UButton></div></template>
        </UTable>
      </section>
    </div>
  </PageContainer>
  <ManagementEditor v-model:open="editorOpen" :title="(editing ? '编辑' : '新增') + (kind === 'dept' ? '院系' : '专业')" :fields="fields" :initial="initial" :saving="saving" @save="save" />
  <UModal v-model:open="deleteOpen" title="确认删除" :description="pending?.row.name"><template #footer><UButton color="neutral" variant="outline" @click="pending = null">取消</UButton><UButton color="error" :loading="saving" @click="remove">确认删除</UButton></template></UModal>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { request } from '@/api/request'
import { getDepts, getMajors } from '@/api/base'
import type { DeptItem, MajorItem } from '@/api/types'
import ManagementEditor, { type EditorField } from '@/components/ManagementEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
const depts = ref<DeptItem[]>([]), majors = ref<MajorItem[]>([])
const loading = ref(false), saving = ref(false), error = ref(''), editorOpen = ref(false)
const kind = ref<'dept' | 'major'>('dept'), editing = ref<number | null>(null)
const initial = ref<Record<string, unknown>>({}), pending = ref<{ kind: 'dept' | 'major'; row: DeptItem | MajorItem } | null>(null)
const deleteOpen = computed({ get: () => pending.value !== null, set: value => { if (!value) pending.value = null } })
const toast = useToast()
const deptColumns: TableColumn<DeptItem>[] = [{ accessorKey: 'code', header: '代码' }, { accessorKey: 'name', header: '院系名称' }, { accessorKey: 'status', header: '状态' }, { id: 'actions', header: '操作' }]
const majorColumns: TableColumn<MajorItem>[] = [{ accessorKey: 'code', header: '代码' }, { accessorKey: 'name', header: '专业名称' }, { accessorKey: 'deptName', header: '院系' }, { accessorKey: 'degreeYears', header: '学制' }, { id: 'actions', header: '操作' }]
const fields = computed<EditorField[]>(() => [
  { key: 'code', label: '代码', required: true, maxlength: 32 }, { key: 'name', label: '名称', required: true, maxlength: 64 },
  ...(kind.value === 'dept' ? [{ key: 'leader', label: '负责人', maxlength: 50 }, { key: 'sortNo', label: '排序', type: 'number', min: 0, max: 100000 }] : [
    { key: 'deptId', label: '所属院系', required: true, options: depts.value.map(d => ({ label: d.name, value: d.id })) },
    { key: 'degreeYears', label: '学制', required: true, type: 'number', min: 1, max: 8 },
  ]),
  { key: 'status', label: '状态', required: true, options: [{ label: '启用', value: 'ACTIVE' }, { label: '停用', value: 'DISABLED' }] },
])
function edit(type: 'dept' | 'major', row?: DeptItem | MajorItem) {
  kind.value = type; editing.value = row?.id ?? null
  initial.value = row ? { ...row } : { status: 'ACTIVE', sortNo: 0, degreeYears: 4, deptId: depts.value.find(d => d.status === 'ACTIVE')?.id }
  editorOpen.value = true
}
async function loadData() {
  loading.value = true; error.value = ''
  try { [depts.value, majors.value] = await Promise.all([getDepts(), getMajors()]) }
  catch { error.value = '请检查网络或后端服务后重试。' }
  finally { loading.value = false }
}
async function save(data: Record<string, string>) {
  if (saving.value) return
  saving.value = true
  try {
    await request({ url: `/${kind.value === 'dept' ? 'depts' : 'majors'}${editing.value ? '/' + editing.value : ''}`, method: editing.value ? 'PUT' : 'POST', data })
    editorOpen.value = false; toast.add({ title: '已保存', color: 'success' }); await loadData()
  } catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
async function remove() {
  if (!pending.value || saving.value) return
  saving.value = true
  try { await request({ url: `/${pending.value.kind === 'dept' ? 'depts' : 'majors'}/${pending.value.row.id}`, method: 'DELETE' }); pending.value = null; await loadData() }
  catch { /* 关联保护提示由请求层显示 */ } finally { saving.value = false }
}
onMounted(loadData)
</script>

<template>
  <PageContainer :title="teacherMode ? '教学班维护' : '教学班管理'" description="维护教学班、容量与排课，确认后执行发布、关闭或取消。">
    <template #actions><UButton :disabled="!courseOptions.length || !terms.length" @click="edit()">新增教学班</UButton><UButton color="neutral" variant="outline" @click="loadData">刷新</UButton></template>
    <div class="flex flex-wrap gap-3 pb-4"><UInput v-model="keyword" placeholder="搜索课程或班级" aria-label="搜索教学班" @keyup.enter="loadData" /><select v-model="termId" aria-label="筛选学期" class="rounded-md border border-(--ui-border) bg-(--ui-bg) p-2" @change="page = 1; loadData()"><option v-for="term in terms" :key="term.id" :value="String(term.id)">{{ term.name }}</option></select><UButton @click="page = 1; loadData()">查询</UButton></div>
    <UAlert v-if="error" color="error" title="教学班加载失败" :description="error" />
    <div class="overflow-x-auto rounded-xl border border-(--ui-border)"><UTable :data="classes" :columns="columns" :loading="loading" class="min-w-250">
      <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="class" /></template>
      <template #seats-cell="{ row }">{{ row.original.enrolledCount }} / {{ row.original.capacity }}</template>
      <template #actions-cell="{ row }"><div class="flex flex-wrap gap-2">
        <UButton size="sm" variant="outline" :disabled="terminal(row.original)" @click="edit(row.original)">编辑</UButton><UButton size="sm" variant="outline" :disabled="terminal(row.original)" @click="scheduleItem = row.original">排课</UButton>
        <UButton v-if="row.original.status === 'DRAFT'" size="sm" color="success" @click="act(row.original, 'PUBLISH')">发布</UButton>
        <UButton v-if="row.original.status === 'PUBLISHED'" size="sm" color="warning" @click="act(row.original, 'CLOSE')">关闭选课</UButton>
        <UButton v-if="!terminal(row.original)" size="sm" color="error" variant="outline" @click="act(row.original, 'CANCEL')">取消开班</UButton>
        <UButton v-if="!teacherMode && row.original.status === 'PUBLISHED'" size="sm" variant="outline" @click="act(row.original, 'ROLLBACK_DRAFT')">退回草稿</UButton>
        <UButton v-if="!teacherMode || row.original.status === 'DRAFT'" size="sm" color="error" variant="outline" @click="act(row.original, 'DELETE')">删除</UButton>
      </div></template>
    </UTable><div class="flex justify-between p-4"><span>共 {{ total }} 个 · 第 {{ page }} 页</span><div class="flex gap-2"><UButton :disabled="page === 1" @click="page--; loadData()">上一页</UButton><UButton :disabled="page * 20 >= total" @click="page++; loadData()">下一页</UButton></div></div></div>
  </PageContainer>
  <ManagementEditor v-model:open="editorOpen" :title="editing ? '编辑教学班' : '新增教学班'" :fields="fields" :initial="initial" :saving="saving" @save="save" />
  <ClassScheduleEditor v-model:item="scheduleItem" @changed="loadData" />
  <UModal v-model:open="confirmOpen" title="确认教学班操作" :description="pending?.row.className"><template #body><p>{{ actionLabels[pending?.action || ''] }}将改变教学安排，请核对后执行。</p><label v-if="pending?.action === 'CANCEL'" class="mt-3 grid gap-2">取消原因<textarea v-model="reason" aria-label="取消原因" maxlength="300" class="rounded-md border border-(--ui-border) bg-(--ui-bg) p-2" /><span class="text-sm text-(--ui-text-muted)">有选课记录时须至少5字，取消后学生选课将撤回。</span></label></template><template #footer><UButton color="neutral" variant="outline" @click="pending = null">返回</UButton><UButton :loading="saving" @click="confirm">确认执行</UButton></template></UModal>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getCourses, getTerms } from '@/api/base'
import { getUsers } from '@/api/users'
import { getMyTeachingClasses, getTeachingClasses } from '@/api/teachingClasses'
import { request } from '@/api/request'
import { useTermStore } from '@/stores/term'
import type { TeachingClassItem, TermItem, CourseItem, UserItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ManagementEditor, { type EditorField } from '@/components/ManagementEditor.vue'
import ClassScheduleEditor from '@/components/ClassScheduleEditor.vue'
const props = defineProps<{ teacherMode?: boolean }>()
const classes = ref<TeachingClassItem[]>([]), terms = ref<TermItem[]>([]), courseOptions = ref<CourseItem[]>([]), teachers = ref<UserItem[]>([])
const loading = ref(false), saving = ref(false), error = ref(''), keyword = ref(''), termId = ref(''), page = ref(1), total = ref(0), editorOpen = ref(false)
const editing = ref<number | null>(null), initial = ref<Record<string, unknown>>({}), scheduleItem = ref<TeachingClassItem | null>(null)
const pending = ref<{ row: TeachingClassItem; action: string } | null>(null), reason = ref('')
const confirmOpen = computed({ get: () => !!pending.value, set: v => { if (!v) pending.value = null } })
const actionLabels: Record<string, string> = { PUBLISH: '发布', CLOSE: '关闭选课', CANCEL: '取消开班', ROLLBACK_DRAFT: '退回草稿', DELETE: '删除' }
const columns: TableColumn<TeachingClassItem>[] = [{ accessorKey: 'className', header: '教学班' }, { accessorKey: 'courseName', header: '课程' }, { accessorKey: 'teacherName', header: '教师' }, { accessorKey: 'credit', header: '学分' }, { id: 'seats', header: '已选 / 容量' }, { id: 'status', header: '状态' }, { id: 'actions', header: '操作' }]
const fields = computed<EditorField[]>(() => [
  { key: 'className', label: '教学班名称', required: true, maxlength: 64 }, { key: 'courseId', label: '课程', required: true, options: courseOptions.value.map(c => ({ label: `${c.courseCode} ${c.name}`, value: c.id })) },
  { key: 'termId', label: '学期', required: true, options: terms.value.filter(t => t.status !== 'CLOSED').map(t => ({ label: t.name, value: t.id })) },
  ...(!props.teacherMode ? [{ key: 'teacherId', label: '授课教师', required: true, options: teachers.value.map(t => ({ label: `${t.username} ${t.realName}`, value: t.id })) }] : []),
  { key: 'capacity', label: '容量', required: true, type: 'number', min: 1, max: 100000 }, { key: 'location', label: '默认地点', maxlength: 100 },
  { key: 'startWeek', label: '开始周', required: true, type: 'number', min: 1, max: 30 }, { key: 'endWeek', label: '结束周', required: true, type: 'number', min: 1, max: 30 },
  { key: 'openClassDate', label: '开课日期', required: true, type: 'date' }, { key: 'description', label: '说明', maxlength: 500 },
])
function terminal(row: TeachingClassItem) { return row.status === 'CLOSED' || row.status === 'CANCELLED' }
function edit(row?: TeachingClassItem) { editing.value = row?.id ?? null; initial.value = row ? { ...row } : { courseId: courseOptions.value[0]?.id, termId: termId.value, teacherId: teachers.value[0]?.id, capacity: 30, startWeek: 1, endWeek: 16 }; editorOpen.value = true }
function act(row: TeachingClassItem, action: string) { pending.value = { row, action }; reason.value = '' }
async function loadData() {
  if (!termId.value) return; loading.value = true; error.value = ''
  try {
    if (props.teacherMode) { const rows = (await getMyTeachingClasses(Number(termId.value))).filter(c => !keyword.value || [c.className, c.courseName, c.courseCode].some(v => v.includes(keyword.value))); total.value = rows.length; classes.value = rows.slice((page.value - 1) * 20, page.value * 20) }
    else { const result = await getTeachingClasses({ termId: Number(termId.value), keyword: keyword.value || undefined, page: page.value, size: 20 }); classes.value = result.records; total.value = result.total }
  } catch { error.value = '请检查服务后重试。' } finally { loading.value = false }
}
async function save(data: Record<string, string>) { if (saving.value) return; saving.value = true; try { await request({ url: '/teaching-classes' + (editing.value ? '/' + editing.value : ''), method: editing.value ? 'PUT' : 'POST', data }); editorOpen.value = false; await loadData() } catch { /* 请求层显示错误 */ } finally { saving.value = false } }
async function confirm() {
  if (!pending.value || saving.value) return; saving.value = true
  try { const { row, action } = pending.value; await request({ url: `/teaching-classes/${row.id}` + (action === 'DELETE' ? '' : '/status'), method: action === 'DELETE' ? 'DELETE' : 'PUT', data: action === 'DELETE' ? undefined : { action, reason: reason.value } }); pending.value = null; await loadData() } catch { /* 请求层显示错误 */ } finally { saving.value = false }
}
onMounted(async () => {
  try {
    terms.value = await getTerms(); termId.value = String(useTermStore().currentTermId || terms.value.find(t => t.status === 'ENROLLING')?.id || terms.value[0]?.id || '')
    for (let p = 1; ; p++) { const result = await getCourses({ page: p, size: 100 }); courseOptions.value.push(...result.records.filter(c => c.status === 'ACTIVE')); if (!result.records.length || p * 100 >= result.total) break }
    if (!props.teacherMode) for (let p = 1; ; p++) { const result = await getUsers({ role: 'TEACHER', status: 'ACTIVE', page: p, size: 100 }); teachers.value.push(...result.records); if (!result.records.length || p * 100 >= result.total) break }
    await loadData()
  } catch { error.value = '维护选项加载失败，请刷新重试。' }
})
</script>

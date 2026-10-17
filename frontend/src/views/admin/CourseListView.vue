<template>
  <PageContainer title="课程目录" description="维护课程标准库；已有教学班或选课引用的课程不能删除。">
    <template #actions><UButton icon="i-lucide-plus" @click="editCourse()">新增课程</UButton><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <div class="flex flex-wrap gap-3">
        <UInput v-model="keyword" placeholder="搜索课程代码或名称" aria-label="搜索课程代码或名称" class="min-w-48 flex-1" @keyup.enter="search" />
        <UButton :loading="loading" @click="search">查询</UButton>
      </div>
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="课程加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="courses" :columns="columns" :loading="loading" class="min-w-170">
          <template #courseCode-cell="{ row }"><span class="font-mono font-semibold">{{ row.original.courseCode }}</span></template>
          <template #type-cell="{ row }"><UBadge :color="row.original.courseType === 'REQUIRED' ? 'primary' : 'neutral'" variant="subtle">{{ typeLabel(row.original.courseType) }}</UBadge></template>
          <template #prereqNames-cell="{ row }">{{ row.original.prereqNames || '无' }}</template>
          <template #actions-cell="{ row }"><div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" @click="editCourse(row.original)">编辑</UButton><UButton size="sm" color="neutral" variant="outline" @click="prereqCourse = row.original">先修课程</UButton><UButton size="sm" color="error" variant="outline" @click="deleting = row.original">删除</UButton></div></template>
        </UTable>
        <p v-if="!loading && !error && !courses.length" class="p-8 text-center text-sm text-(--ui-text-muted)">暂无课程。</p>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) p-4 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 门 · 第 {{ page }} 页</span>
          <div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</UButton><UButton size="sm" color="neutral" variant="outline" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</UButton></div>
        </div>
      </div>
    </div>
  </PageContainer>
  <UModal v-model:open="editorOpen" :title="editingId ? '编辑课程' : '新增课程'" description="保存后由后端校验并写入课程库。">
    <template #body>
      <form id="course-editor" class="grid gap-4 sm:grid-cols-2" @submit.prevent="save">
        <UFormField label="课程代码" name="courseCode" required><UInput v-model="form.courseCode" maxlength="32" required class="w-full" /></UFormField>
        <UFormField label="课程名称" name="name" required><UInput v-model="form.name" maxlength="100" required class="w-full" /></UFormField>
        <UFormField label="学分" name="credit" required><UInput v-model="form.credit" type="number" min="0.1" max="99.9" step="0.1" required class="w-full" /></UFormField>
        <UFormField label="学时" name="creditHours" required><UInput v-model="form.creditHours" type="number" min="1" max="10000" step="1" required class="w-full" /></UFormField>
        <div><label for="course-type" class="mb-1 block text-sm">课程类型</label><select id="course-type" v-model="form.courseType" class="w-full rounded-md border border-(--ui-border) bg-(--ui-bg) p-2"><option value="REQUIRED">必修</option><option value="ELECTIVE">选修</option><option value="RESTRICTED">限选</option></select></div>
        <div><label for="course-dept" class="mb-1 block text-sm">开课院系</label><select id="course-dept" v-model.number="form.deptId" required class="w-full rounded-md border border-(--ui-border) bg-(--ui-bg) p-2" @change="form.majorId = 0"><option :value="0" disabled>请选择院系</option><option v-for="dept in depts" :key="dept.id" :value="dept.id">{{ dept.name }}</option></select></div>
        <div><label for="course-major" class="mb-1 block text-sm">面向专业</label><select id="course-major" v-model.number="form.majorId" class="w-full rounded-md border border-(--ui-border) bg-(--ui-bg) p-2"><option :value="0">全专业</option><option v-for="major in availableMajors" :key="major.id" :value="major.id">{{ major.name }}</option></select></div>
        <div><label for="course-status" class="mb-1 block text-sm">课程状态</label><select id="course-status" v-model="form.status" class="w-full rounded-md border border-(--ui-border) bg-(--ui-bg) p-2"><option value="ACTIVE">启用</option><option value="DISABLED">停用</option></select></div>
        <UFormField label="课程说明" name="description" class="sm:col-span-2"><UTextarea v-model="form.description" maxlength="1000" class="w-full" /></UFormField>
        <UAlert v-if="saveError" color="error" :title="saveError" class="sm:col-span-2" />
      </form>
    </template>
    <template #footer><UButton color="neutral" variant="outline" :disabled="saving" @click="editorOpen = false">取消</UButton><UButton type="submit" form="course-editor" :loading="saving">保存课程</UButton></template>
  </UModal>
  <UModal v-model:open="deleteOpen" title="删除课程" description="课程将从目录移除，已有业务引用时后端会拒绝操作。">
    <template #body><p>确定删除“{{ deleting?.name }}”吗？</p><UAlert v-if="saveError" color="error" :title="saveError" /></template>
    <template #footer><UButton color="neutral" variant="outline" :disabled="saving" @click="deleting = null">取消</UButton><UButton color="error" :loading="saving" @click="remove">确认删除</UButton></template>
  </UModal>
  <CoursePrerequisites v-model:course="prereqCourse" @changed="loadData" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { createCourse, updateCourse, deleteCourse, getCourses, getDepts, getMajors } from '@/api/base'
import type { CourseItem, DeptItem, MajorItem } from '@/api/types'
import { useToast } from '@nuxt/ui/composables'
import PageContainer from '@/components/PageContainer.vue'
import CoursePrerequisites from '@/components/CoursePrerequisites.vue'
const prereqCourse = ref<CourseItem | null>(null)
const columns: TableColumn<CourseItem>[] = [
  { accessorKey: 'courseCode', header: '课程代码' }, { accessorKey: 'name', header: '课程名称' },
  { accessorKey: 'credit', header: '学分' }, { accessorKey: 'creditHours', header: '学时' },
  { id: 'type', header: '课程类型' }, { accessorKey: 'prereqNames', header: '先修要求' },
  { id: 'actions', header: '操作' },
]
const courses = ref<CourseItem[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
const keyword = ref('')
const depts = ref<DeptItem[]>([])
const majors = ref<MajorItem[]>([])
const toast = useToast()
const editorOpen = ref(false)
const editingId = ref<number | null>(null)
const deleting = ref<CourseItem | null>(null)
const deleteOpen = computed({ get: () => deleting.value !== null, set: value => { if (!value) deleting.value = null } })
const saving = ref(false)
const saveError = ref('')
const form = reactive({ courseCode: '', name: '', credit: '3', creditHours: '48', courseType: 'ELECTIVE' as CourseItem['courseType'], deptId: 0, majorId: 0, description: '', status: 'ACTIVE' as 'ACTIVE' | 'DISABLED' })
const availableMajors = computed(() => majors.value.filter(major => major.deptId === form.deptId))
function search() { page.value = 1; void loadData() }
async function editCourse(course?: CourseItem) {
  saveError.value = ''
  editingId.value = course?.id ?? null
  Object.assign(form, { courseCode: course?.courseCode ?? '', name: course?.name ?? '', credit: String(course?.credit ?? 3), creditHours: String(course?.creditHours ?? 48), courseType: course?.courseType ?? 'ELECTIVE', deptId: course?.deptId ?? 0, majorId: course?.majorId ?? 0, description: course?.description ?? '', status: course?.status ?? 'ACTIVE' })
  try {
    const [departments, specialities] = await Promise.all([getDepts(), getMajors()])
    depts.value = departments.filter(dept => dept.status === 'ACTIVE')
    majors.value = specialities.filter(major => major.status === 'ACTIVE')
    editorOpen.value = true
  } catch { toast.add({ title: '院系或专业加载失败，请重试', color: 'error' }) }
}
async function save() {
  if (saving.value) return
  if (!form.courseCode.trim() || !form.name.trim() || form.deptId <= 0) { saveError.value = '请填写课程代码、名称并选择开课院系'; return }
  saving.value = true
  saveError.value = ''
  try {
    const data = { ...form, credit: Number(form.credit), creditHours: Number(form.creditHours), majorId: form.majorId || undefined }
    if (editingId.value) await updateCourse(editingId.value, data)
    else await createCourse(data)
    editorOpen.value = false
    toast.add({ title: '课程已保存', color: 'success' })
    await loadData()
  } catch { saveError.value = '课程未保存，请根据错误提示修正后重试。' }
  finally { saving.value = false }
}
async function remove() {
  if (!deleting.value || saving.value) return
  saving.value = true
  saveError.value = ''
  try {
    await deleteCourse(deleting.value.id)
    deleting.value = null
    toast.add({ title: '课程已删除', color: 'success' })
    await loadData()
  } catch { saveError.value = '删除失败，请检查该课程是否仍有业务引用。' }
  finally { saving.value = false }
}
function typeLabel(type: CourseItem['courseType']) { return ({ REQUIRED: '必修', ELECTIVE: '选修', RESTRICTED: '限选' })[type] || type }
function changePage(next: number) { page.value = next; void loadData() }
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getCourses({ page: page.value, size, keyword: keyword.value.trim() || undefined })
    courses.value = result.records || []
    total.value = result.total || 0
  } catch {
    courses.value = []
    total.value = 0
    error.value = '请检查网络或后端服务，然后重试。'
  } finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

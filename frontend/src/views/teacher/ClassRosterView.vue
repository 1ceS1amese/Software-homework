<template>
  <PageContainer :title="`教学班选课名单 · #${classId}`" description="仅展示后端返回的真实选课名单。">
    <template #actions>
      <UButton to="/teacher/classes" color="neutral" variant="outline" icon="i-lucide-arrow-left">返回列表</UButton>
      <UButton icon="i-lucide-download" :disabled="!filteredStudents.length || loading" @click="exportCsv">{{ keyword.trim() ? '导出筛选名单' : '导出花名册' }}</UButton>
    </template>
    <div class="space-y-5">
      <div class="flex flex-wrap items-end gap-3 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-4">
        <UFormField label="检索学生" class="min-w-48 flex-1 sm:max-w-82">
          <UInput v-model="keyword" icon="i-lucide-search" placeholder="输入学号、姓名或教学班" class="w-full" />
        </UFormField>
        <UButton color="neutral" variant="outline" @click="keyword = ''">重置筛选</UButton>
        <span class="text-sm text-(--ui-text-muted)">显示 {{ filteredStudents.length }} / {{ students.length }} 名学生</span>
      </div>
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="名单加载失败" :description="error">
        <template #actions><UButton color="error" variant="outline" size="sm" @click="loadData">重试</UButton></template>
      </UAlert>
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="filteredStudents" :columns="columns" :loading="loading" class="min-w-165">
          <template #student-cell="{ row }"><span class="font-mono">{{ row.original.studentUsername || row.original.studentId }}</span></template>
          <template #source-cell="{ row }"><UBadge :color="row.original.source === 'PORTAL' ? 'primary' : 'neutral'" variant="subtle">{{ row.original.source === 'PORTAL' ? '自主选课' : '管理代选' }}</UBadge></template>
        </UTable>
        <p v-if="!loading && !error && !filteredStudents.length" class="p-8 text-center text-sm text-(--ui-text-muted)">{{ students.length ? '没有符合筛选条件的学生。' : '当前教学班暂无选课学生。' }}</p>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useRoute } from 'vue-router'
import { getClassStudents } from '@/api/enrollments'
import type { EnrollmentItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import { buildRosterCsv } from '@/utils/rosterCsv'

const route = useRoute()
const classId = computed(() => Number(route.params.id))
const students = ref<EnrollmentItem[]>([])
const keyword = ref('')
const filteredStudents = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase()
  return students.value.filter(student => [student.studentUsername || student.studentId, student.studentName, student.className]
    .some(value => String(value ?? '').toLocaleLowerCase().includes(query)))
})
const loading = ref(false)
const error = ref('')
let latestRequest = 0
const columns: TableColumn<EnrollmentItem>[] = [
  { id: 'student', header: '学号 / 账号' }, { accessorKey: 'studentName', header: '姓名' },
  { accessorKey: 'className', header: '教学班' }, { accessorKey: 'enrolledAt', header: '选课时间' },
  { id: 'source', header: '选课途径' },
]
async function loadData() {
  const requestId = ++latestRequest
  const id = classId.value
  loading.value = true
  error.value = ''
  students.value = []
  try {
    if (!Number.isInteger(id) || id <= 0) throw new Error('无效班级')
    const rows = await getClassStudents(id)
    if (requestId === latestRequest) students.value = rows || []
  } catch {
    if (requestId === latestRequest) error.value = '请检查网络或后端服务，然后重试。'
  } finally { if (requestId === latestRequest) loading.value = false }
}
function exportCsv() {
  if (!filteredStudents.value.length || loading.value) return
  const blob = new Blob([buildRosterCsv(filteredStudents.value)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `教学班_${classId.value}_选课花名册.csv`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
watch(classId, () => { keyword.value = ''; void loadData() }, { immediate: true })
</script>

<template>
  <PageContainer :title="`教学班选课名单 · #${classId}`" description="仅展示后端返回的真实选课名单。">
    <template #actions>
      <UButton to="/teacher/classes" color="neutral" variant="outline" icon="i-lucide-arrow-left">返回列表</UButton>
      <UButton icon="i-lucide-download" :disabled="!students.length || loading" @click="exportCsv">导出花名册</UButton>
    </template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="名单加载失败" :description="error">
        <template #actions><UButton color="error" variant="outline" size="sm" @click="loadData">重试</UButton></template>
      </UAlert>
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="students" :columns="columns" :loading="loading" class="min-w-165">
          <template #student-cell="{ row }"><span class="font-mono">{{ row.original.studentUsername || row.original.studentId }}</span></template>
          <template #source-cell="{ row }"><UBadge :color="row.original.source === 'PORTAL' ? 'primary' : 'neutral'" variant="subtle">{{ row.original.source === 'PORTAL' ? '自主选课' : '管理代选' }}</UBadge></template>
        </UTable>
        <p v-if="!loading && !error && !students.length" class="p-8 text-center text-sm text-(--ui-text-muted)">当前教学班暂无选课学生。</p>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useRoute } from 'vue-router'
import { getClassStudents } from '@/api/enrollments'
import type { EnrollmentItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

const route = useRoute()
const classId = Number(route.params.id)
const students = ref<EnrollmentItem[]>([])
const loading = ref(false)
const error = ref('')
const columns: TableColumn<EnrollmentItem>[] = [
  { id: 'student', header: '学号 / 账号' }, { accessorKey: 'studentName', header: '姓名' },
  { accessorKey: 'className', header: '教学班' }, { accessorKey: 'enrolledAt', header: '选课时间' },
  { id: 'source', header: '选课途径' },
]
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    if (!Number.isInteger(classId) || classId <= 0) throw new Error('无效班级')
    students.value = await getClassStudents(classId) || []
  } catch {
    students.value = []
    error.value = '请检查网络或后端服务，然后重试。'
  } finally { loading.value = false }
}
function csvCell(value: unknown) { return `"${String(value ?? '').replaceAll('"', '""')}"` }
function exportCsv() {
  if (!students.value.length) return
  const rows = [['学号', '姓名', '教学班', '选课时间', '选课途径'], ...students.value.map(s => [s.studentUsername || s.studentId, s.studentName, s.className, s.enrolledAt, s.source])]
  const blob = new Blob(['\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `教学班_${classId}_选课花名册.csv`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
onMounted(() => { void loadData() })
</script>

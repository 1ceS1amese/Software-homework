<template>
  <PageContainer :title="`教学班成绩 · #${classId}`" description="总评以服务端配置计算；预览仅供核对，发布后成绩将锁定。">
    <template #actions>
      <UButton to="/teacher/classes" color="neutral" variant="outline" icon="i-lucide-arrow-left">返回列表</UButton>
      <UButton :disabled="!canEdit || !weights" :loading="saving" @click="saveDraft">暂存草稿</UButton>
      <UButton color="success" :disabled="!canPublish" :loading="publishing" @click="confirmOpen = true">正式发布</UButton>
    </template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="成绩数据加载失败" :description="error">
        <template #actions><UButton color="error" variant="outline" size="sm" @click="loadData">重试</UButton></template>
      </UAlert>
      <UAlert v-if="isPublished" color="success" variant="soft" icon="i-lucide-lock-keyhole" title="成绩已发布" description="成绩已锁定。如需修改，请联系教务管理员解锁。" />
      <UAlert v-if="!weights" color="warning" variant="soft" icon="i-lucide-info" title="成绩权重不可用" description="暂无法读取系统参数，已禁用保存和发布，避免错误预览。" />
      <UAlert v-else color="info" variant="soft" icon="i-lucide-info" :title="`当前权重：平时 ${weights.regular * 100}% · 期中 ${weights.midterm * 100}% · 期末 ${weights.final * 100}%`" description="最终总评由后端按系统参数重新计算。" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="grades" :columns="columns" :loading="loading" class="min-w-195">
          <template #student-cell="{ row }"><div class="font-medium">{{ row.original.studentName || '未命名学生' }}</div><div class="text-xs text-(--ui-text-muted)">{{ row.original.studentNumber || row.original.studentId }}</div></template>
          <template #regular-cell="{ row }"><UInput :model-value="inputValue(row.original.regularScore)" type="number" min="0" max="100" step="0.1" size="sm" class="w-25" :disabled="isPublished" aria-label="平时成绩" @update:model-value="(value: string | number) => updateScore(row.original, 'regularScore', value)" /></template>
          <template #midterm-cell="{ row }"><UInput :model-value="inputValue(row.original.midtermScore)" type="number" min="0" max="100" step="0.1" size="sm" class="w-25" :disabled="isPublished" aria-label="期中成绩" @update:model-value="(value: string | number) => updateScore(row.original, 'midtermScore', value)" /></template>
          <template #final-cell="{ row }"><UInput :model-value="inputValue(row.original.finalScore)" type="number" min="0" max="100" step="0.1" size="sm" class="w-25" :disabled="isPublished" aria-label="期末成绩" @update:model-value="(value: string | number) => updateScore(row.original, 'finalScore', value)" /></template>
          <template #total-cell="{ row }"><span class="font-semibold tabular-nums">{{ previewTotal(row.original) ?? '—' }}</span></template>
          <template #status-cell="{ row }"><UBadge :color="row.original.status === 'PUBLISHED' ? 'success' : 'neutral'" variant="subtle">{{ row.original.status === 'PUBLISHED' ? '已发布' : '草稿' }}</UBadge></template>
        </UTable>
        <p v-if="!loading && !error && !grades.length" class="p-8 text-center text-sm text-(--ui-text-muted)">该教学班暂无选课学生。</p>
      </div>
    </div>
  </PageContainer>
  <UModal v-model:open="confirmOpen" title="确认发布成绩" description="发布后将向学生公示并锁定，只有教务管理员可以解锁。">
    <template #footer><UButton color="neutral" variant="outline" @click="confirmOpen = false">取消</UButton><UButton color="success" :loading="publishing" @click="publish">确认发布</UButton></template>
  </UModal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { useRoute } from 'vue-router'
import { getClassGrades, publishGrades, saveGrades } from '@/api/grades'
import { getConfigs } from '@/api/base'
import type { GradeItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

type Weights = { regular: number; midterm: number; final: number }
const columns: TableColumn<GradeItem>[] = [
  { id: 'student', header: '学生' }, { id: 'regular', header: '平时' }, { id: 'midterm', header: '期中' },
  { id: 'final', header: '期末' }, { id: 'total', header: '总评预览' }, { id: 'status', header: '状态' },
]
const route = useRoute()
const toast = useToast()
const classId = Number(route.params.id)
const grades = ref<GradeItem[]>([])
const weights = ref<Weights | null>(null)
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const confirmOpen = ref(false)
const error = ref('')
const isPublished = computed(() => grades.value.some(g => g.status === 'PUBLISHED'))
const canEdit = computed(() => grades.value.length > 0 && !isPublished.value && !loading.value && !error.value)
const canPublish = computed(() => canEdit.value && !!weights.value && grades.value.every(isComplete))
function inputValue(value: number | undefined) { return value == null ? '' : String(value) }
function validScore(value: number | undefined) { return value != null && Number.isFinite(value) && value >= 0 && value <= 100 }
function isComplete(row: GradeItem) { return validScore(row.regularScore) && validScore(row.midtermScore) && validScore(row.finalScore) }
function updateScore(row: GradeItem, field: 'regularScore' | 'midtermScore' | 'finalScore', value: string | number) {
  row[field] = value === '' ? undefined : Number(value)
}
function previewTotal(row: GradeItem) {
  if (!weights.value || !isComplete(row)) return null
  return (row.regularScore! * weights.value.regular + row.midtermScore! * weights.value.midterm + row.finalScore! * weights.value.final).toFixed(2)
}
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    if (!Number.isInteger(classId) || classId <= 0) throw new Error('无效班级')
    const [rows, configs] = await Promise.all([getClassGrades(classId), getConfigs()])
    grades.value = rows || []
    const find = (key: string) => Number(configs.find(c => c.configKey === key)?.configValue)
    const parsed = { regular: find('grade.weight.regular'), midterm: find('grade.weight.midterm'), final: find('grade.weight.final') }
    weights.value = Object.values(parsed).every(v => Number.isFinite(v) && v >= 0) && Math.abs(parsed.regular + parsed.midterm + parsed.final - 1) < 0.001 ? parsed : null
  } catch {
    grades.value = []
    weights.value = null
    error.value = '请检查网络、系统参数或后端服务，然后重试。'
  } finally { loading.value = false }
}
async function saveDraft() {
  if (!canEdit.value || !weights.value) return
  if (grades.value.some(g => [g.regularScore, g.midtermScore, g.finalScore].some(v => v != null && !validScore(v)))) {
    toast.add({ title: '成绩须为 0–100 分', color: 'warning' })
    return
  }
  saving.value = true
  try {
    await saveGrades(classId, grades.value)
    toast.add({ title: '成绩草稿已保存', color: 'success' })
    await loadData()
  } catch { /* 请求层已提示 */ }
  finally { saving.value = false }
}
async function publish() {
  if (!canPublish.value) return
  publishing.value = true
  try {
    await saveGrades(classId, grades.value)
    await publishGrades(classId)
    confirmOpen.value = false
    toast.add({ title: '成绩已正式发布', color: 'success' })
    await loadData()
  } catch { /* 请求层已提示 */ }
  finally { publishing.value = false }
}
onMounted(() => { void loadData() })
</script>

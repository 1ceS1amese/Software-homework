<template>
  <PageContainer title="我的成绩" description="仅展示任课教师已正式发布的成绩。">
    <template #actions><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="成绩加载失败" :description="error" />
      <div class="grid gap-4 sm:grid-cols-3">
        <div v-for="item in summaryCards" :key="item.label" class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
          <p class="text-sm text-(--ui-text-muted)">{{ item.label }}</p>
          <p class="mt-3 text-2xl font-semibold tabular-nums">{{ item.value }}</p>
        </div>
      </div>
      <div class="flex flex-wrap items-end gap-3 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-4">
        <UFormField label="课程名称" class="min-w-48 flex-1 sm:max-w-82">
          <UInput v-model="keyword" icon="i-lucide-search" placeholder="搜索课程名称" class="w-full" />
        </UFormField>
        <UFormField label="考核结果"><USelect v-model="resultFilter" :items="resultOptions" aria-label="筛选考核结果" class="w-36" /></UFormField>
        <UButton color="neutral" variant="outline" @click="keyword = ''; resultFilter = 'all'">重置筛选</UButton>
        <span class="text-sm text-(--ui-text-muted)">显示 {{ grades.length }} / {{ publishedGrades.length }} 门课程</span>
      </div>
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="grades" :columns="columns" :loading="loading" class="min-w-185">
          <template #regularScore-cell="{ row }">{{ format(row.original.regularScore) }}</template>
          <template #midtermScore-cell="{ row }">{{ format(row.original.midtermScore) }}</template>
          <template #finalScore-cell="{ row }">{{ format(row.original.finalScore) }}</template>
          <template #totalScore-cell="{ row }"><strong>{{ format(row.original.totalScore) }}</strong></template>
          <template #gradePoint-cell="{ row }">{{ format(row.original.gradePoint) }}</template>
          <template #result-cell="{ row }"><UBadge :color="row.original.isPass === 1 ? 'success' : row.original.isPass === 0 ? 'error' : 'neutral'" variant="subtle">{{ row.original.isPass === 1 ? '通过' : row.original.isPass === 0 ? '未通过' : '待评定' }}</UBadge></template>
        </UTable>
        <p v-if="!loading && !error && !grades.length" class="p-8 text-center text-sm text-(--ui-text-muted)">{{ publishedGrades.length ? '没有符合筛选条件的成绩。' : '本学期暂无已发布成绩。' }}</p>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getMyGrades } from '@/api/grades'
import { useTermStore } from '@/stores/term'
import type { GradeItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

const termStore = useTermStore()
const rawGrades = ref<GradeItem[]>([])
const publishedGrades = computed(() => rawGrades.value.filter(g => g.status === 'PUBLISHED'))
const keyword = ref('')
const resultFilter = ref('all')
const resultOptions = [{ label: '全部结果', value: 'all' }, { label: '通过', value: 'pass' }, { label: '未通过', value: 'fail' }]
const grades = computed(() => publishedGrades.value.filter(g => {
  const matchesName = (g.courseName || '').toLocaleLowerCase().includes(keyword.value.trim().toLocaleLowerCase())
  const matchesResult = resultFilter.value === 'all' || g.isPass === (resultFilter.value === 'pass' ? 1 : 0)
  return matchesName && matchesResult
}))
const loading = ref(false)
const error = ref('')
let latestRequest = 0
const columns: TableColumn<GradeItem>[] = [
  { accessorKey: 'courseName', header: '课程' }, { accessorKey: 'credit', header: '学分' },
  { accessorKey: 'regularScore', header: '平时' }, { accessorKey: 'midtermScore', header: '期中' },
  { accessorKey: 'finalScore', header: '期末' }, { accessorKey: 'totalScore', header: '总评' },
  { accessorKey: 'gradePoint', header: '绩点' }, { id: 'result', header: '结果' },
]
function format(value: number | undefined) { return value == null ? '—' : value }
function weightedAverage(field: 'totalScore' | 'gradePoint') {
  const valid = publishedGrades.value.filter(g => g[field] != null && g.credit != null && g.credit > 0)
  const credits = valid.reduce((sum, g) => sum + g.credit!, 0)
  return credits ? (valid.reduce((sum, g) => sum + g[field]! * g.credit!, 0) / credits).toFixed(2) : '—'
}
const summaryCards = computed(() => [
  { label: '已获学分', value: publishedGrades.value.reduce((sum, g) => sum + (g.isPass === 1 ? (g.credit ?? 0) : 0), 0) },
  { label: '学分加权平均分', value: weightedAverage('totalScore') },
  { label: '学分加权绩点', value: weightedAverage('gradePoint') },
])
async function loadData() {
  const requestId = ++latestRequest
  const termId = termStore.currentTermId
  rawGrades.value = []
  error.value = ''
  if (termId == null) { loading.value = false; return }
  loading.value = true
  try {
    const rows = await getMyGrades(termId)
    if (requestId === latestRequest) rawGrades.value = rows || []
  } catch {
    if (requestId === latestRequest) error.value = '请检查网络或后端服务，然后重试。'
  } finally { if (requestId === latestRequest) loading.value = false }
}
watch(() => termStore.currentTermId, () => { void loadData() }, { immediate: true })
</script>

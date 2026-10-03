<template>
  <PageContainer title="统计分析与报表" description="按当前学期汇总选课与容量数据，导出与页面一致的真实报表。">
    <template #actions><UButton icon="i-lucide-download" :disabled="!hasExportData || loading" @click="exportCsv">导出当前报表</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="统计数据加载失败" :description="error">
        <template #actions><UButton size="sm" color="error" variant="outline" @click="loadData">重试</UButton></template>
      </UAlert>
      <UTabs v-model="activeTab" :items="tabs" class="w-full" />
      <div v-if="activeTab === 'overview'" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <div v-for="item in overviewCards" :key="item.label" class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
          <div class="text-sm text-(--ui-text-muted)">{{ item.label }}</div>
          <div class="mt-3 text-3xl font-semibold tabular-nums text-(--ui-text-highlighted)">{{ item.value }}</div>
        </div>
      </div>
      <section v-else-if="activeTab === 'byCourse'" class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <p class="border-b border-(--ui-border) px-4 py-3 text-xs text-(--ui-text-muted)">按选课人数排序，最多展示 100 门课程；导出内容与当前表格一致。</p>
        <UTable :data="courseStats" :columns="courseColumns" :loading="loading" class="min-w-150">
          <template #rate-cell="{ row }"><div class="flex items-center gap-2"><UProgress :model-value="row.original.fillRate" size="sm" class="w-24" /><span>{{ row.original.fillRate.toFixed(1) }}%</span></div></template>
        </UTable>
        <p v-if="!loading && !error && !courseStats.length" class="p-8 text-center text-sm text-(--ui-text-muted)">暂无课程统计数据。</p>
      </section>
      <section v-else-if="activeTab === 'capacity'" class="space-y-4">
        <div v-for="group in capacityGroups" :key="group.key" class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
          <div class="border-b border-(--ui-border) p-4 font-semibold">{{ group.label }} · {{ group.rows.length }}</div>
          <UTable :data="group.rows" :columns="capacityColumns" :loading="loading" class="min-w-150">
            <template #seats-cell="{ row }">{{ row.original.enrolledCount }} / {{ row.original.capacity }}</template>
          </UTable>
          <p v-if="!loading && !error && !group.rows.length" class="p-5 text-sm text-(--ui-text-muted)">暂无数据。</p>
        </div>
      </section>
      <UAlert v-else color="info" variant="soft" icon="i-lucide-info" title="成绩分布暂不可用" description="后端目前只提供单个教学班的成绩分布接口，尚无全校汇总接口；此处不展示推测或样例数据。" />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getCapacityAnalysis, getEnrollmentOverview, getStatsByCourse } from '@/api/stats'
import { useTermStore } from '@/stores/term'
import type { StatsOverview } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

type CourseStat = { courseId: number; courseName: string; capacity: number; enrolledCount: number; fillRate: number }
type CapacityClass = { id: number; className: string; courseName: string; teacherName: string | null; capacity: number; enrolledCount: number }
type CapacityResult = { fullClasses: CapacityClass[]; idleClasses: CapacityClass[]; normalClasses: CapacityClass[] }
const tabs = [
  { label: '选课总览', value: 'overview' }, { label: '按课程统计', value: 'byCourse' },
  { label: '容量分析', value: 'capacity' }, { label: '成绩分布', value: 'gradeDist' },
]
const courseColumns: TableColumn<CourseStat>[] = [
  { accessorKey: 'courseName', header: '课程' }, { accessorKey: 'capacity', header: '总容量' },
  { accessorKey: 'enrolledCount', header: '选课人数' }, { id: 'rate', header: '满员率' },
]
const capacityColumns: TableColumn<CapacityClass>[] = [
  { accessorKey: 'className', header: '教学班' }, { accessorKey: 'courseName', header: '课程' },
  { accessorKey: 'teacherName', header: '教师' }, { id: 'seats', header: '已选 / 容量' },
]
const termStore = useTermStore()
const activeTab = ref('overview')
const overview = ref<StatsOverview | null>(null)
const courseStats = ref<CourseStat[]>([])
const capacity = ref<CapacityResult>({ fullClasses: [], idleClasses: [], normalClasses: [] })
const loading = ref(false)
const error = ref('')
const overviewCards = computed(() => [
  { label: '教学班数', value: overview.value?.classCount ?? '—' },
  { label: '总席位容量', value: overview.value?.totalCapacity ?? '—' },
  { label: '选课人次', value: overview.value?.totalEnrollments ?? '—' },
  { label: '在册学生', value: overview.value?.studentCount ?? '—' },
  { label: '平均满员率', value: overview.value ? `${overview.value.avgFillRate.toFixed(1)}%` : '—' },
  { label: '统计学期', value: termStore.currentTerm?.name ?? '全部学期' },
])
const capacityGroups = computed(() => [
  { key: 'fullClasses', label: '满员班级', rows: capacity.value.fullClasses },
  { key: 'idleClasses', label: '闲置班级（低于 30%）', rows: capacity.value.idleClasses },
  { key: 'normalClasses', label: '其他班级', rows: capacity.value.normalClasses },
])
const hasExportData = computed(() => activeTab.value === 'overview' ? !!overview.value
  : activeTab.value === 'byCourse' ? courseStats.value.length > 0
  : activeTab.value === 'capacity' ? capacityGroups.value.some(group => group.rows.length > 0) : false)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const termId = termStore.currentTermId ?? undefined
    const [summary, courses, analysis] = await Promise.all([
      getEnrollmentOverview(termId), getStatsByCourse({ termId, limit: 100 }), getCapacityAnalysis(termId),
    ])
    overview.value = summary
    courseStats.value = courses || []
    capacity.value = analysis as CapacityResult
  } catch {
    overview.value = null
    courseStats.value = []
    capacity.value = { fullClasses: [], idleClasses: [], normalClasses: [] }
    error.value = '请检查网络或后端服务，然后重试。'
  } finally { loading.value = false }
}
function csvCell(value: unknown) { return `"${String(value ?? '').replaceAll('"', '""')}"` }
function exportCsv() {
  if (!hasExportData.value) return
  let rows: unknown[][] = []
  if (activeTab.value === 'overview') rows = [['指标', '数值'], ...overviewCards.value.map(card => [card.label, card.value])]
  else if (activeTab.value === 'byCourse') rows = [['课程', '容量', '选课人数', '满员率(%)'], ...courseStats.value.map(c => [c.courseName, c.capacity, c.enrolledCount, c.fillRate])]
  else rows = [['类别', '教学班', '课程', '教师', '已选', '容量'], ...capacityGroups.value.flatMap(g => g.rows.map(c => [g.label, c.className, c.courseName, c.teacherName, c.enrolledCount, c.capacity]))]
  const blob = new Blob(['\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `教务报表_${activeTab.value}.csv`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
onMounted(() => { void loadData() })
</script>

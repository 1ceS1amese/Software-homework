<template>
  <PageContainer title="教务概览" description="本学期选课与教学班数据实时汇总。">
    <template #actions><UButton icon="i-lucide-refresh-cw" color="neutral" variant="outline" :loading="loading" @click="loadData">刷新数据</UButton></template>

    <div class="space-y-6">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="统计数据加载失败" :description="error" />
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="card in cards" :key="card.label" class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
          <div class="flex items-center justify-between"><span class="text-sm text-(--ui-text-muted)">{{ card.label }}</span><UIcon :name="card.icon" class="size-5 text-indigo-500" /></div>
          <div class="mt-4 text-3xl font-semibold tabular-nums text-(--ui-text-highlighted)">{{ card.value }}</div>
          <div class="mt-2 text-xs text-(--ui-text-muted)">{{ card.hint }}</div>
        </div>
      </div>

      <div class="grid gap-6 lg:grid-cols-[1.4fr_0.6fr]">
        <section class="min-w-0 overflow-hidden rounded-xl border border-(--ui-border) bg-(--ui-bg)">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-(--ui-border) p-5">
            <div><h2 class="font-semibold text-(--ui-text-highlighted)">热门课程</h2><p class="mt-1 text-xs text-(--ui-text-muted)">按实际选课人数展示前五项</p></div>
            <UButton to="/admin/stats" color="neutral" variant="ghost" size="sm" trailing-icon="i-lucide-arrow-right">查看统计</UButton>
          </div>
          <div class="overflow-x-auto"><UTable :data="hotCourses" :columns="columns" :loading="loading" class="min-w-110">
            <template #seats-cell="{ row }">{{ row.original.enrolledCount }} / {{ row.original.capacity }}</template>
            <template #rate-cell="{ row }"><div class="flex items-center gap-3"><UProgress :model-value="row.original.fillRate" size="sm" class="w-22" /><span class="text-xs tabular-nums">{{ row.original.fillRate.toFixed(1) }}%</span></div></template>
          </UTable></div>
          <div v-if="!loading && !error && !hotCourses.length" class="p-8 text-center text-sm text-(--ui-text-muted)">本学期暂无课程统计数据。</div>
        </section>

        <section class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
          <h2 class="font-semibold text-(--ui-text-highlighted)">教务工作入口</h2>
          <p class="mt-1 text-xs text-(--ui-text-muted)">常用管理操作</p>
          <div class="mt-5 space-y-2">
            <UButton to="/admin/teaching-classes" block color="neutral" variant="outline" icon="i-lucide-presentation" class="justify-start">管理教学班</UButton>
            <UButton to="/admin/courses" block color="neutral" variant="outline" icon="i-lucide-library-big" class="justify-start">管理课程</UButton>
            <UButton to="/admin/audit-logs" block color="neutral" variant="outline" icon="i-lucide-shield-check" class="justify-start">查看审计日志</UButton>
          </div>
        </section>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getEnrollmentOverview, getStatsByCourse } from '@/api/stats'
import { useTermStore } from '@/stores/term'
import type { StatsOverview } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

type CourseStat = { courseId: number; courseName: string; capacity: number; enrolledCount: number; fillRate: number }
const columns: TableColumn<CourseStat>[] = [
  { accessorKey: 'courseName', header: '课程' },
  { id: 'seats', header: '已选 / 容量' },
  { id: 'rate', header: '满员率' },
]
const termStore = useTermStore()
const overview = ref<StatsOverview | null>(null)
const hotCourses = ref<CourseStat[]>([])
const loading = ref(false)
const error = ref('')
const cards = computed(() => [
  { label: '教学班数', value: overview.value?.classCount ?? '—', hint: '本学期开设', icon: 'i-lucide-presentation' },
  { label: '选课人次', value: overview.value?.totalEnrollments ?? '—', hint: '已生效选课', icon: 'i-lucide-users-round' },
  { label: '平均满员率', value: overview.value ? `${overview.value.avgFillRate.toFixed(1)}%` : '—', hint: '全部教学班', icon: 'i-lucide-chart-pie' },
  { label: '在册学生', value: overview.value?.studentCount ?? '—', hint: '系统中全部学生账号', icon: 'i-lucide-graduation-cap' },
])

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const termId = termStore.currentTermId ?? undefined
    const [summary, courses] = await Promise.all([getEnrollmentOverview(termId), getStatsByCourse({ termId, limit: 5 })])
    overview.value = summary
    hotCourses.value = courses || []
  } catch {
    overview.value = null
    hotCourses.value = []
    error.value = '请检查网络或后端服务后重试。'
  } finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

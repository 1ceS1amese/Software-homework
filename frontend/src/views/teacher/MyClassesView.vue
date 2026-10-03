<template>
  <PageContainer title="我的教学班" description="查看本人负责的课程、选课人数与成绩管理入口。">
    <template #actions><UButton icon="i-lucide-refresh-cw" color="neutral" variant="outline" :loading="loading" @click="loadClasses">刷新列表</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="教学班加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="classes" :columns="columns" :loading="loading" class="min-w-190">
          <template #course-cell="{ row }"><div class="font-semibold">{{ row.original.courseName }}</div><div class="mt-1 text-xs text-(--ui-text-muted)">{{ row.original.className }} · {{ row.original.courseCode }}</div></template>
          <template #seats-cell="{ row }"><div class="w-36 space-y-1"><div class="text-xs">{{ row.original.enrolledCount }} / {{ row.original.capacity }}</div><UProgress :model-value="fillRate(row.original)" size="sm" /></div></template>
          <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="class" /></template>
          <template #schedule-cell="{ row }"><span v-if="row.original.schedules?.length">{{ formatSchedule(row.original.schedules[0]) }}</span><span v-else class="text-(--ui-text-muted)">待安排</span></template>
          <template #actions-cell="{ row }"><div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :to="`/teacher/classes/${row.original.id}/roster`">选课名单</UButton><UButton size="sm" :to="`/teacher/classes/${row.original.id}/grades`">成绩录入</UButton></div></template>
        </UTable>
        <p v-if="!loading && !error && !classes.length" class="p-8 text-center text-sm text-(--ui-text-muted)">当前学期暂无负责的教学班。</p>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getMyTeachingClasses } from '@/api/teachingClasses'
import { useTermStore } from '@/stores/term'
import type { ClassScheduleItem, TeachingClassItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'

const termStore = useTermStore()
const classes = ref<TeachingClassItem[]>([])
const loading = ref(false)
const error = ref('')
const columns: TableColumn<TeachingClassItem>[] = [
  { id: 'course', header: '课程 / 班号' }, { accessorKey: 'credit', header: '学分' },
  { id: 'seats', header: '选课人数' }, { id: 'status', header: '状态' },
  { id: 'schedule', header: '上课安排' }, { id: 'actions', header: '操作' },
]
function fillRate(row: TeachingClassItem) { return row.capacity ? Math.min(100, Math.round(row.enrolledCount / row.capacity * 100)) : 0 }
function formatSchedule(s: ClassScheduleItem) { return `周${['一', '二', '三', '四', '五', '六', '日'][s.dayOfWeek - 1] || s.dayOfWeek} 第${s.startSection}–${s.endSection}节 · ${s.location || '地点待定'}` }
async function loadClasses() {
  loading.value = true
  error.value = ''
  try { classes.value = await getMyTeachingClasses(termStore.currentTermId ?? undefined) || [] }
  catch { classes.value = []; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
onMounted(() => { void loadClasses() })
</script>

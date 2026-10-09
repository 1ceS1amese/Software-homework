<template>
  <PageContainer title="课程选择" description="查找本学期开放的教学班，核对时间与余量后完成选课。">
    <template #actions>
      <UBadge color="primary" variant="subtle" size="lg">本学期已选 {{ enrollStore.totalCredits }} 学分</UBadge>
    </template>

    <div class="space-y-5">
      <div class="flex flex-wrap items-end gap-3 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-4">
        <UFormField label="课程、代码或教师" class="min-w-48 flex-1 sm:max-w-82">
          <UInput v-model="filters.keyword" icon="i-lucide-search" placeholder="输入关键词" class="w-full" @keyup.enter="search" />
        </UFormField>
        <USwitch v-model="filters.onlyAvailable" label="只看有余量" class="min-h-10" @update:model-value="search" />
        <UButton icon="i-lucide-search" :loading="loading" @click="search">查询</UButton>
        <UButton color="neutral" variant="outline" @click="resetFilters">重置</UButton>
      </div>

      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="课程列表加载失败" :description="error">
        <template #actions><UButton color="error" variant="outline" size="sm" @click="loadClasses">重试</UButton></template>
      </UAlert>
      <UAlert v-if="enrollmentError" color="warning" variant="soft" icon="i-lucide-info" title="已选课程未能同步" :description="enrollmentError" />

      <div class="min-w-0 overflow-hidden rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <div class="overflow-x-auto">
          <UTable :data="classes" :columns="columns" :loading="loading" class="min-w-220">
            <template #course-cell="{ row }">
              <div class="font-semibold text-(--ui-text-highlighted)">{{ row.original.courseName }}</div>
              <div class="mt-1 text-xs text-(--ui-text-muted)">{{ row.original.courseCode }} · {{ row.original.className }}</div>
            </template>
            <template #schedule-cell="{ row }">
              <span v-if="row.original.schedules?.length">{{ formatSchedule(row.original.schedules[0]) }}</span>
              <span v-else class="text-(--ui-text-muted)">时间待安排</span>
            </template>
            <template #seats-cell="{ row }">
              <div class="w-36 space-y-1.5">
                <div class="flex justify-between text-xs"><span>{{ row.original.enrolledCount }}/{{ row.original.capacity }}</span><span :class="remaining(row.original) ? 'text-emerald-700' : 'text-rose-700'">{{ remaining(row.original) ? `余 ${remaining(row.original)}` : '已满' }}</span></div>
                <UProgress :model-value="fillRate(row.original)" size="sm" :color="remaining(row.original) ? 'primary' : 'error'" />
              </div>
            </template>
            <template #status-cell="{ row }">
              <UBadge v-if="isEnrolled(row.original.id)" color="success" variant="subtle">已选</UBadge>
              <UBadge v-else-if="!remaining(row.original)" color="error" variant="subtle">已满</UBadge>
              <UBadge v-else-if="enrollStore.hasTimeConflict(row.original)" color="warning" variant="subtle">时间冲突</UBadge>
              <UBadge v-else color="primary" variant="subtle">可选</UBadge>
            </template>
            <template #actions-cell="{ row }">
              <div class="flex flex-wrap gap-2">
                <UButton size="sm" color="neutral" variant="ghost" @click="detailClass = row.original">详情</UButton>
                <UButton v-if="isEnrolled(row.original.id)" size="sm" color="error" variant="outline" :disabled="loading || !!enrollmentError || !termStore.isWithdrawAllowedNow" :loading="actionLoading === row.original.id" @click="requestAction(row.original, 'withdraw')">退课</UButton>
                <UButton v-else size="sm" :disabled="loading || !!enrollmentError || !remaining(row.original) || enrollStore.hasTimeConflict(row.original) || !termStore.isEnrollingNow" :loading="actionLoading === row.original.id" @click="requestAction(row.original, 'enroll')">选课</UButton>
              </div>
            </template>
          </UTable>
        </div>
        <div v-if="!loading && !error && classes.length === 0" class="p-8 text-center text-sm text-(--ui-text-muted)">当前筛选条件下没有开放的教学班。</div>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) px-4 py-3 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 个教学班 · 第 {{ page }} 页</span>
          <div class="flex gap-2">
            <UButton color="neutral" variant="outline" size="sm" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</UButton>
            <UButton color="neutral" variant="outline" size="sm" :disabled="page * pageSize >= total || loading" @click="changePage(page + 1)">下一页</UButton>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>

  <UModal v-model:open="detailOpen" title="教学班详情" :description="detailClass?.courseName">
    <template #body>
      <div v-if="detailClass" class="space-y-4 text-sm">
        <p><strong>教师：</strong>{{ detailClass.teacherName }}　<strong>学分：</strong>{{ detailClass.credit }}</p>
        <p><strong>已选 / 容量：</strong>{{ detailClass.enrolledCount }} / {{ detailClass.capacity }}</p>
        <div><strong>上课安排：</strong><ul class="mt-2 space-y-2"><li v-for="(s, index) in detailClass.schedules || []" :key="index" class="rounded-lg bg-(--ui-bg-muted) p-2">{{ formatSchedule(s) }} · {{ s.location || '地点待定' }} · 第 {{ s.startWeek }}–{{ s.endWeek }} 周</li></ul></div>
      </div>
    </template>
  </UModal>

  <UModal v-model:open="confirmOpen" :title="pendingAction === 'enroll' ? '确认选课' : '确认退课'">
    <template #body>
      <p v-if="pendingClass" class="text-sm leading-7">{{ pendingAction === 'enroll' ? `确定选择“${pendingClass.courseName}”吗？课程为 ${pendingClass.credit} 学分。` : `确定退出“${pendingClass.courseName}”吗？名额将释放给其他同学。` }}</p>
    </template>
    <template #footer>
      <UButton color="neutral" variant="outline" @click="confirmOpen = false">取消</UButton>
      <UButton :color="pendingAction === 'withdraw' ? 'error' : 'primary'" :loading="actionLoading !== null" @click="confirmAction">确认</UButton>
    </template>
  </UModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { getTeachingClasses } from '@/api/teachingClasses'
import { useEnrollStore } from '@/stores/enroll'
import { useTermStore } from '@/stores/term'
import type { ClassScheduleItem, TeachingClassItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

const enrollStore = useEnrollStore()
const termStore = useTermStore()
const toast = useToast()
const columns: TableColumn<TeachingClassItem>[] = [
  { id: 'course', header: '课程 / 教学班' },
  { accessorKey: 'teacherName', header: '授课教师' },
  { accessorKey: 'credit', header: '学分' },
  { id: 'schedule', header: '上课时间' },
  { id: 'seats', header: '选课名额' },
  { id: 'status', header: '状态' },
  { id: 'actions', header: '操作' },
]
const filters = reactive({ keyword: '', onlyAvailable: false })
const classes = ref<TeachingClassItem[]>([])
const loading = ref(false)
const error = ref('')
const enrollmentError = ref('')
const actionLoading = ref<number | null>(null)
const page = ref(1)
const pageSize = 10
const total = ref(0)
let latestClassRequest = 0
const detailClass = ref<TeachingClassItem | null>(null)
const detailOpen = computed({ get: () => detailClass.value !== null, set: open => { if (!open) detailClass.value = null } })
const pendingClass = ref<TeachingClassItem | null>(null)
const pendingAction = ref<'enroll' | 'withdraw'>('enroll')
const confirmOpen = ref(false)

function remaining(row: TeachingClassItem) { return Math.max(0, row.capacity - row.enrolledCount) }
function fillRate(row: TeachingClassItem) { return row.capacity ? Math.min(100, Math.round(row.enrolledCount / row.capacity * 100)) : 0 }
function isEnrolled(id: number) { return enrollStore.enrolledClassIds.has(id) }
function formatSchedule(s: ClassScheduleItem) { return `周${['一','二','三','四','五','六','日'][s.dayOfWeek - 1] || s.dayOfWeek} 第 ${s.startSection}–${s.endSection} 节` }
function search() { page.value = 1; void loadClasses() }
function resetFilters() { filters.keyword = ''; filters.onlyAvailable = false; search() }
function changePage(next: number) { page.value = next; void loadClasses() }

async function loadClasses() {
  const requestId = ++latestClassRequest
  loading.value = true
  error.value = ''
  try {
    const result = await getTeachingClasses({ termId: termStore.currentTermId ?? undefined, keyword: filters.keyword.trim() || undefined, status: 'PUBLISHED', onlyAvailable: filters.onlyAvailable || undefined, page: page.value, size: pageSize })
    if (requestId !== latestClassRequest) return
    classes.value = result.records || []
    total.value = result.total || 0
  } catch {
    if (requestId !== latestClassRequest) return
    classes.value = []
    total.value = 0
    error.value = '请检查网络或后端服务，然后重试。'
  } finally { if (requestId === latestClassRequest) loading.value = false }
}

function requestAction(row: TeachingClassItem, action: 'enroll' | 'withdraw') {
  pendingClass.value = row
  pendingAction.value = action
  confirmOpen.value = true
}

async function confirmAction() {
  const row = pendingClass.value
  if (!row || actionLoading.value !== null) return
  actionLoading.value = row.id
  try {
    if (pendingAction.value === 'enroll') await enrollStore.add(row)
    else await enrollStore.remove(row.id)
    toast.add({ title: pendingAction.value === 'enroll' ? '选课成功' : '退课成功', description: row.courseName, color: 'success' })
    confirmOpen.value = false
    const [, enrolled] = await Promise.allSettled([loadClasses(), enrollStore.fetchMine(termStore.currentTermId ?? undefined)])
    enrollmentError.value = enrolled.status === 'rejected' ? '选课操作已完成，但状态未能同步，请刷新重试。' : ''
  } catch {
    // 请求层已展示具体业务错误。重新查询服务端状态，以便同步名额。
    await loadClasses()
  } finally { actionLoading.value = null }
}

onMounted(async () => {
  try { await enrollStore.fetchMine(termStore.currentTermId ?? undefined) }
  catch { enrollmentError.value = '请刷新页面重试；选课状态暂不可用。' }
  await loadClasses()
})
watch(() => termStore.currentTermId, async () => {
  ++latestClassRequest
  classes.value = []
  total.value = 0
  loading.value = true
  page.value = 1
  try { await enrollStore.fetchMine(termStore.currentTermId ?? undefined); enrollmentError.value = '' }
  catch { enrollmentError.value = '请刷新页面重试；选课状态暂不可用。' }
  await loadClasses()
})
</script>

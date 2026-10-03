<template>
  <PageContainer title="课程目录" description="查看全校课程标准库；当前后端尚未提供课程创建或编辑接口。">
    <template #actions><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="课程加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="courses" :columns="columns" :loading="loading" class="min-w-170">
          <template #courseCode-cell="{ row }"><span class="font-mono font-semibold">{{ row.original.courseCode }}</span></template>
          <template #type-cell="{ row }"><UBadge :color="row.original.courseType === 'REQUIRED' ? 'primary' : 'neutral'" variant="subtle">{{ typeLabel(row.original.courseType) }}</UBadge></template>
          <template #prereqNames-cell="{ row }">{{ row.original.prereqNames || '无' }}</template>
        </UTable>
        <p v-if="!loading && !error && !courses.length" class="p-8 text-center text-sm text-(--ui-text-muted)">暂无课程。</p>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) p-4 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 门 · 第 {{ page }} 页</span>
          <div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</UButton><UButton size="sm" color="neutral" variant="outline" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</UButton></div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getCourses } from '@/api/base'
import type { CourseItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
const columns: TableColumn<CourseItem>[] = [
  { accessorKey: 'courseCode', header: '课程代码' }, { accessorKey: 'name', header: '课程名称' },
  { accessorKey: 'credit', header: '学分' }, { accessorKey: 'creditHours', header: '学时' },
  { id: 'type', header: '课程类型' }, { accessorKey: 'prereqNames', header: '先修要求' },
]
const courses = ref<CourseItem[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
function typeLabel(type: CourseItem['courseType']) { return ({ REQUIRED: '必修', ELECTIVE: '选修', RESTRICTED: '限选' })[type] || type }
function changePage(next: number) { page.value = next; void loadData() }
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getCourses({ page: page.value, size })
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

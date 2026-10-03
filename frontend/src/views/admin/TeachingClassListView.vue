<template>
  <PageContainer title="教学班管理" description="查看当前学期教学班、任课教师与名额；当前后端暂未提供教学班变更接口。">
    <template #actions><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="教学班加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="classes" :columns="columns" :loading="loading" class="min-w-180">
          <template #class-cell="{ row }"><div class="font-semibold">{{ row.original.className }}</div><div class="text-xs text-(--ui-text-muted)">#{{ row.original.id }}</div></template>
          <template #seats-cell="{ row }">{{ row.original.enrolledCount }} / {{ row.original.capacity }}</template>
          <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="class" /></template>
        </UTable>
        <p v-if="!loading && !error && !classes.length" class="p-8 text-center text-sm text-(--ui-text-muted)">当前学期暂无教学班。</p>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) p-4 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 个 · 第 {{ page }} 页</span>
          <div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</UButton><UButton size="sm" color="neutral" variant="outline" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</UButton></div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getTeachingClasses } from '@/api/teachingClasses'
import { useTermStore } from '@/stores/term'
import type { TeachingClassItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
const columns: TableColumn<TeachingClassItem>[] = [
  { id: 'class', header: '教学班' }, { accessorKey: 'courseName', header: '课程' },
  { accessorKey: 'teacherName', header: '教师' }, { accessorKey: 'credit', header: '学分' },
  { id: 'seats', header: '已选 / 容量' }, { id: 'status', header: '状态' },
]
const termStore = useTermStore()
const classes = ref<TeachingClassItem[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
function changePage(next: number) { page.value = next; void loadData() }
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getTeachingClasses({ termId: termStore.currentTermId ?? undefined, page: page.value, size })
    classes.value = result.records || []
    total.value = result.total || 0
  } catch { classes.value = []; total.value = 0; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

<template>
  <PageContainer title="院系与专业" description="查看现有院系及专业；当前后端尚未提供新增和编辑接口。">
    <template #actions><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="基础数据加载失败" :description="error" />
      <div class="grid min-w-0 gap-5 xl:grid-cols-2">
        <section class="min-w-0 overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
          <h2 class="border-b border-(--ui-border) p-4 font-semibold">院系 · {{ depts.length }}</h2>
          <UTable :data="depts" :columns="deptColumns" :loading="loading" class="min-w-110">
            <template #status-cell="{ row }"><UBadge :color="row.original.status === 'ACTIVE' ? 'success' : 'error'" variant="subtle">{{ row.original.status === 'ACTIVE' ? '启用' : '停用' }}</UBadge></template>
          </UTable>
          <p v-if="!loading && !error && !depts.length" class="p-6 text-sm text-(--ui-text-muted)">暂无院系。</p>
        </section>
        <section class="min-w-0 overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
          <h2 class="border-b border-(--ui-border) p-4 font-semibold">专业 · {{ majors.length }}</h2>
          <UTable :data="majors" :columns="majorColumns" :loading="loading" class="min-w-110">
            <template #status-cell="{ row }"><UBadge :color="row.original.status === 'ACTIVE' ? 'success' : 'error'" variant="subtle">{{ row.original.status === 'ACTIVE' ? '启用' : '停用' }}</UBadge></template>
          </UTable>
          <p v-if="!loading && !error && !majors.length" class="p-6 text-sm text-(--ui-text-muted)">暂无专业。</p>
        </section>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getDepts, getMajors } from '@/api/base'
import type { DeptItem, MajorItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
const deptColumns: TableColumn<DeptItem>[] = [
  { accessorKey: 'code', header: '代码' }, { accessorKey: 'name', header: '院系名称' },
  { accessorKey: 'leader', header: '负责人' }, { id: 'status', header: '状态' },
]
const majorColumns: TableColumn<MajorItem>[] = [
  { accessorKey: 'code', header: '代码' }, { accessorKey: 'name', header: '专业名称' },
  { accessorKey: 'degreeYears', header: '学制' }, { id: 'status', header: '状态' },
]
const depts = ref<DeptItem[]>([])
const majors = ref<MajorItem[]>([])
const loading = ref(false)
const error = ref('')
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [d, m] = await Promise.all([getDepts(), getMajors()])
    depts.value = d || []
    majors.value = m || []
  } catch { depts.value = []; majors.value = []; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

<template>
  <PageContainer title="学期管理" description="查看教学日历与选退课时间；当前后端暂未提供学期变更接口。">
    <template #actions><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="学期加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="terms" :columns="columns" :loading="loading" class="min-w-180">
          <template #dates-cell="{ row }">{{ row.original.startDate }} – {{ row.original.endDate }}</template>
          <template #enroll-cell="{ row }">{{ row.original.enrollStart }} – {{ row.original.enrollEnd }}</template>
          <template #status-cell="{ row }"><StatusTag :status="row.original.status" category="term" /></template>
        </UTable>
        <p v-if="!loading && !error && !terms.length" class="p-8 text-center text-sm text-(--ui-text-muted)">暂无学期。</p>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getTerms } from '@/api/base'
import type { TermItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
const columns: TableColumn<TermItem>[] = [
  { accessorKey: 'code', header: '学期代码' }, { accessorKey: 'name', header: '学期名称' },
  { id: 'dates', header: '学期起止' }, { id: 'enroll', header: '选课时间' }, { id: 'status', header: '状态' },
]
const terms = ref<TermItem[]>([])
const loading = ref(false)
const error = ref('')
async function loadData() {
  loading.value = true
  error.value = ''
  try { terms.value = await getTerms() || [] }
  catch { terms.value = []; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

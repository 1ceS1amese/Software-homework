<template>
  <PageContainer title="操作审计日志" description="查询系统操作与变更详情。">
    <div class="space-y-5">
      <div class="flex flex-wrap items-end gap-3 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-4">
        <UFormField label="操作人"><UInput v-model="query.username" placeholder="用户名" @keyup.enter="search" /></UFormField>
        <UFormField label="模块"><USelect v-model="query.module" :items="moduleOptions" class="w-38" /></UFormField>
        <UFormField label="结果"><USelect v-model="query.result" :items="resultOptions" class="w-32" /></UFormField>
        <UButton icon="i-lucide-search" :loading="loading" @click="search">查询</UButton>
      </div>
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="审计日志加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="logs" :columns="columns" :loading="loading" class="min-w-190">
          <template #result-cell="{ row }"><UBadge :color="row.original.result === 'SUCCESS' ? 'success' : 'error'" variant="subtle">{{ row.original.result === 'SUCCESS' ? '成功' : '失败' }}</UBadge></template>
          <template #actions-cell="{ row }"><UButton size="sm" color="neutral" variant="outline" @click="selectedLog = row.original">详情</UButton></template>
        </UTable>
        <p v-if="!loading && !error && !logs.length" class="p-8 text-center text-sm text-(--ui-text-muted)">没有匹配的审计记录。</p>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) p-4 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 条 · 第 {{ page }} 页</span>
          <div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</UButton><UButton size="sm" color="neutral" variant="outline" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</UButton></div>
        </div>
      </div>
    </div>
  </PageContainer>
  <UModal v-model:open="detailOpen" title="操作记录详情">
    <template #body><div v-if="selectedLog" class="space-y-4 text-sm">
      <p><strong>操作人：</strong>{{ selectedLog.username || '—' }}</p>
      <p><strong>模块 / 动作：</strong>{{ selectedLog.module }} / {{ selectedLog.action }}</p>
      <p><strong>时间：</strong>{{ selectedLog.createdAt }}</p>
      <div><strong>变更前</strong><pre class="mt-2 max-h-50 overflow-auto rounded-lg bg-(--ui-bg-muted) p-3 text-xs whitespace-pre-wrap">{{ selectedLog.beforeJson || '无' }}</pre></div>
      <div><strong>变更后</strong><pre class="mt-2 max-h-50 overflow-auto rounded-lg bg-(--ui-bg-muted) p-3 text-xs whitespace-pre-wrap">{{ selectedLog.afterJson || '无' }}</pre></div>
    </div></template>
  </UModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getAuditLogs } from '@/api/audit'
import type { AuditLogItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
const columns: TableColumn<AuditLogItem>[] = [
  { accessorKey: 'createdAt', header: '操作时间' }, { accessorKey: 'username', header: '操作人' },
  { accessorKey: 'module', header: '模块' }, { accessorKey: 'action', header: '动作' },
  { id: 'result', header: '结果' }, { accessorKey: 'ip', header: 'IP' }, { id: 'actions', header: '操作' },
]
const moduleOptions = [
  { label: '全部模块', value: 'all' }, { label: '选课', value: 'ENROLL' }, { label: '成绩', value: 'GRADE' },
  { label: '认证', value: 'AUTH' }, { label: '用户', value: 'USER' }, { label: '基础数据', value: 'BASE' },
]
const resultOptions = [{ label: '全部结果', value: 'all' }, { label: '成功', value: 'SUCCESS' }, { label: '失败', value: 'FAIL' }]
const query = reactive({ username: '', module: 'all', result: 'all' })
const logs = ref<AuditLogItem[]>([])
const selectedLog = ref<AuditLogItem | null>(null)
const detailOpen = computed({ get: () => selectedLog.value !== null, set: open => { if (!open) selectedLog.value = null } })
const page = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const error = ref('')
function search() { page.value = 1; void loadData() }
function changePage(next: number) { page.value = next; void loadData() }
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getAuditLogs({
      username: query.username.trim() || undefined,
      module: query.module === 'all' ? undefined : query.module,
      result: query.result === 'all' ? undefined : query.result,
      page: page.value, size,
    })
    logs.value = result.records || []
    total.value = result.total || 0
  } catch { logs.value = []; total.value = 0; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

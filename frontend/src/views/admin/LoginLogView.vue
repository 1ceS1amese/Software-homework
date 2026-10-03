<template>
  <PageContainer title="登录日志" description="查询系统认证尝试与来源 IP。">
    <div class="space-y-5">
      <div class="flex flex-wrap items-end gap-3 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-4">
        <UFormField label="用户名"><UInput v-model="query.username" placeholder="输入用户名" @keyup.enter="search" /></UFormField>
        <UFormField label="结果"><USelect v-model="query.result" :items="resultOptions" class="w-35" /></UFormField>
        <UButton icon="i-lucide-search" :loading="loading" @click="search">查询</UButton>
      </div>
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="登录日志加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="logs" :columns="columns" :loading="loading" class="min-w-170">
          <template #result-cell="{ row }"><UBadge :color="row.original.result === 'SUCCESS' ? 'success' : 'error'" variant="subtle">{{ row.original.result === 'SUCCESS' ? '成功' : row.original.result }}</UBadge></template>
          <template #userAgent-cell="{ row }"><span class="block max-w-70 truncate text-xs" :title="row.original.userAgent">{{ row.original.userAgent || '未记录' }}</span></template>
        </UTable>
        <p v-if="!loading && !error && !logs.length" class="p-8 text-center text-sm text-(--ui-text-muted)">没有匹配的登录记录。</p>
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-(--ui-border) p-4 text-sm">
          <span class="text-(--ui-text-muted)">共 {{ total }} 条 · 第 {{ page }} 页</span>
          <div class="flex gap-2"><UButton size="sm" color="neutral" variant="outline" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</UButton><UButton size="sm" color="neutral" variant="outline" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</UButton></div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { getLoginLogs } from '@/api/audit'
import type { LoginLogItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'
const columns: TableColumn<LoginLogItem>[] = [
  { accessorKey: 'loginAt', header: '登录时间' }, { accessorKey: 'username', header: '用户名' },
  { id: 'result', header: '结果' }, { accessorKey: 'ip', header: 'IP' }, { accessorKey: 'userAgent', header: '客户端' },
]
const resultOptions = [{ label: '全部结果', value: 'all' }, { label: '成功', value: 'SUCCESS' }, { label: '失败', value: 'FAIL' }]
const query = reactive({ username: '', result: 'all' })
const logs = ref<LoginLogItem[]>([])
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
    const result = await getLoginLogs({ username: query.username.trim() || undefined, result: query.result === 'all' ? undefined : query.result, page: page.value, size })
    logs.value = result.records || []
    total.value = result.total || 0
  } catch { logs.value = []; total.value = 0; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>

<template>
  <PageContainer title="系统参数" description="维护选课规则与成绩权重。修改后以后端返回结果为准。">
    <template #actions><UButton icon="i-lucide-refresh-cw" color="neutral" variant="outline" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="参数加载失败" :description="error" />
      <div class="overflow-x-auto rounded-xl border border-(--ui-border) bg-(--ui-bg)">
        <UTable :data="configs" :columns="columns" :loading="loading" class="min-w-180">
          <template #configKey-cell="{ row }"><span class="font-mono text-xs font-semibold">{{ row.original.configKey }}</span></template>
          <template #value-cell="{ row }"><UInput v-model="row.original.configValue" size="sm" :disabled="!row.original.editable" aria-label="参数值" class="w-42" /></template>
          <template #valueType-cell="{ row }"><UBadge color="neutral" variant="subtle">{{ row.original.valueType }}</UBadge></template>
          <template #actions-cell="{ row }"><UButton size="sm" :disabled="!row.original.editable || savingId !== null" :loading="savingId === row.original.id" @click="save(row.original)">保存</UButton></template>
        </UTable>
        <p v-if="!loading && !error && !configs.length" class="p-8 text-center text-sm text-(--ui-text-muted)">暂无可配置参数。</p>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { getConfigs, updateConfig } from '@/api/base'
import type { SysConfigItem } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

const toast = useToast()
const loading = ref(false)
const savingId = ref<number | null>(null)
const error = ref('')
const configs = ref<SysConfigItem[]>([])
const columns: TableColumn<SysConfigItem>[] = [
  { accessorKey: 'configKey', header: '参数键' }, { id: 'value', header: '配置值' },
  { accessorKey: 'valueType', header: '类型' }, { accessorKey: 'description', header: '说明' },
  { id: 'actions', header: '操作' },
]
async function loadData() {
  loading.value = true
  error.value = ''
  try { configs.value = await getConfigs() || [] }
  catch { configs.value = []; error.value = '请检查网络或后端服务，然后重试。' }
  finally { loading.value = false }
}
async function save(row: SysConfigItem) {
  if (!row.editable || savingId.value !== null) return
  savingId.value = row.id
  try {
    await updateConfig(row.id, row.configValue)
    toast.add({ title: '参数已保存', description: row.configKey, color: 'success' })
    await loadData()
  } catch {
    await loadData()
  } finally { savingId.value = null }
}
onMounted(() => { void loadData() })
</script>

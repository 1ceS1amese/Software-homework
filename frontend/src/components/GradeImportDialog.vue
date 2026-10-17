<template>
  <UModal v-model:open="open" title="CSV成绩导入" description="先核对预览差异，再确认整批保存草稿；导入不会自动发布。">
    <template #body><div class="space-y-4">
      <p class="text-sm">UTF-8 编码，最多500条、1MB。空成绩表示暂不录入；选课关系ID必须来自当前教学班。</p>
      <UButton variant="outline" @click="download">下载当前班模板</UButton>
      <label class="grid gap-2 text-sm">选择CSV文件<input type="file" accept=".csv,text/csv" aria-label="选择CSV文件" @change="choose" /></label>
      <UButton :disabled="!file || loading || saving" :loading="loading" @click="preview">预览导入</UButton>
      <div v-if="differences.length" class="overflow-x-auto"><UTable :data="differences" :columns="columns" class="min-w-180">
        <template #before-cell="{ row }">{{ scores(row.original.before) }}</template><template #after-cell="{ row }">{{ scores(row.original.after) }}</template>
        <template #valid-cell="{ row }"><UBadge :color="row.original.valid ? 'success' : 'error'">{{ row.original.valid ? '通过' : '须修正' }}</UBadge></template>
      </UTable></div>
      <p v-if="differences.length" role="status" class="text-sm">共 {{ differences.length }} 条，{{ differences.filter(r => !r.valid).length }} 条须修正。预览10分钟有效；成绩、名单、参数变更或服务重启后需重新预览。</p>
    </div></template>
    <template #footer><UButton color="neutral" variant="outline" @click="open = false">取消</UButton><UButton :disabled="!token || !differences.length || saving || loading" :loading="saving" @click="confirm">确认导入</UButton></template>
  </UModal>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { request, requestClient } from '@/api/request'
import type { GradeItem, ApiResponse } from '@/api/types'
type Scores = Record<string, number | string | null>
interface Difference { rowNumber: number; enrollmentId: number | null; before: Scores | null; after: Scores; valid: boolean; message: string }
const props = defineProps<{ classId: number; grades: GradeItem[] }>()
const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ imported: [] }>()
const file = ref<File | null>(null), differences = ref<Difference[]>([]), token = ref(''), loading = ref(false), saving = ref(false)
const toast = useToast()
const columns: TableColumn<Difference>[] = [{ accessorKey: 'rowNumber', header: '文件行' }, { accessorKey: 'enrollmentId', header: '选课关系' }, { id: 'before', header: '原平时 / 期中 / 期末' }, { id: 'after', header: '新平时 / 期中 / 期末' }, { id: 'valid', header: '校验' }, { accessorKey: 'message', header: '说明' }]
function scores(row: Scores | null) { return row ? ['regularScore', 'midtermScore', 'finalScore'].map(k => row[k] ?? '—').join(' / ') : '—' }
function choose(event: Event) { file.value = (event.target as HTMLInputElement).files?.[0] || null; differences.value = []; token.value = '' }
function download() {
  const csv = '\uFEFFenrollmentId,regularScore,midtermScore,finalScore\r\n' + props.grades.map(g => [g.enrollmentId, g.regularScore ?? '', g.midtermScore ?? '', g.finalScore ?? ''].join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' })); const link = document.createElement('a'); link.href = url; link.download = `成绩模板_${props.classId}.csv`; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
async function preview() {
  if (!file.value || loading.value || saving.value) return
  if (file.value.size > 1024 * 1024) { toast.add({ title: '文件须在1MB以内', color: 'warning' }); return }
  loading.value = true; token.value = ''; differences.value = []
  try { const data = new FormData(); data.append('file', file.value); const response = await requestClient.request<ApiResponse<Difference[]>>({ url: `/teaching-classes/${props.classId}/grades/import`, method: 'POST', data, headers: { 'Content-Type': undefined } }); differences.value = response.data.data; token.value = String(response.headers['x-import-token'] || '') }
  catch { /* 请求层提示格式问题 */ } finally { loading.value = false }
}
async function confirm() {
  if (!token.value || saving.value || loading.value) return; saving.value = true
  try { await request({ url: `/teaching-classes/${props.classId}/grades/import/confirm`, method: 'POST', data: { previewToken: token.value } }); open.value = false; emit('imported'); toast.add({ title: '成绩草稿已导入，请核对后发布', color: 'success' }) }
  catch { token.value = '' } finally { saving.value = false }
}
watch(open, value => { if (!value) { file.value = null; token.value = ''; differences.value = [] } })
</script>

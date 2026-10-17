<template>
  <UModal v-model:open="open" title="教学班排课" :description="item?.className">
    <template #body><div class="space-y-4"><UButton @click="edit()">新增排课</UButton>
      <p class="text-sm text-(--ui-text-muted)">时段不能重叠；已发布教学班须保留排课，有学生时变更还会核对其其他课程。</p>
      <ul class="space-y-3"><li v-for="row in rows" :key="row.id" class="flex flex-wrap items-center justify-between gap-2 border-b border-(--ui-border) pb-2">
        <span>周{{ row.dayOfWeek }} {{ time(row.startMinute) }}–{{ time(row.endMinute) }} · {{ row.weekDesc }} · {{ row.location }}</span>
        <div class="flex gap-2"><UButton size="sm" variant="outline" @click="edit(row)">编辑排课</UButton><UButton size="sm" color="error" variant="outline" @click="deleting = row.id || null">删除排课</UButton></div>
      </li></ul><p v-if="!rows.length">尚未排课。</p>
    </div></template>
  </UModal>
  <ManagementEditor v-model:open="editorOpen" :title="editing ? '编辑排课' : '新增排课'" :fields="fields" :initial="initial" :saving="saving" @save="save" />
  <UModal v-model:open="deleteOpen" title="确认删除排课" description="请核对学生安排；已发布教学班的最后一个时段不能删除。"><template #footer><UButton color="neutral" variant="outline" @click="deleting = null">取消</UButton><UButton color="error" :loading="saving" @click="remove">确认删除排课</UButton></template></UModal>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { request } from '@/api/request'
import type { TeachingClassItem, ClassScheduleItem } from '@/api/types'
import ManagementEditor, { type EditorField } from '@/components/ManagementEditor.vue'
const item = defineModel<TeachingClassItem | null>('item', { required: true })
const emit = defineEmits<{ changed: [] }>()
const open = computed({ get: () => !!item.value, set: v => { if (!v) item.value = null } })
const rows = ref<ClassScheduleItem[]>([]), editorOpen = ref(false), saving = ref(false), editing = ref<number | null>(null), deleting = ref<number | null>(null), initial = ref<Record<string, unknown>>({})
const deleteOpen = computed({ get: () => deleting.value !== null, set: v => { if (!v) deleting.value = null } })
const fields: EditorField[] = [
  { key: 'dayOfWeek', label: '星期', required: true, options: ['一', '二', '三', '四', '五', '六', '日'].map((d, i) => ({ label: '周' + d, value: i + 1 })) },
  { key: 'startSection', label: '开始节次', type: 'number', min: 1, max: 12, required: true }, { key: 'endSection', label: '结束节次', type: 'number', min: 1, max: 12, required: true },
  { key: 'startTime', label: '开始时刻', type: 'time', required: true }, { key: 'endTime', label: '结束时刻', type: 'time', required: true },
  { key: 'startWeek', label: '开始周', type: 'number', min: 1, max: 30, required: true }, { key: 'endWeek', label: '结束周', type: 'number', min: 1, max: 30, required: true }, { key: 'location', label: '上课地点', maxlength: 100 },
]
function time(minute: number) { return `${Math.floor(minute / 60).toString().padStart(2, '0')}:${(minute % 60).toString().padStart(2, '0')}` }
function minutes(value: string) { const [h, m] = value.split(':').map(Number); return h! * 60 + m! }
async function load() { if (item.value) rows.value = await request({ url: `/teaching-classes/${item.value.id}/schedules` }) }
function edit(row?: ClassScheduleItem) { editing.value = row?.id ?? null; initial.value = row ? { ...row, startTime: time(row.startMinute), endTime: time(row.endMinute) } : { dayOfWeek: 1, startSection: 1, endSection: 2, startTime: '08:00', endTime: '09:40', startWeek: item.value?.startWeek || 1, endWeek: item.value?.endWeek || 16, location: item.value?.location || '' }; editorOpen.value = true }
async function save(data: Record<string, string>) {
  if (!item.value || saving.value) return; saving.value = true
  try { await request({ url: `/teaching-classes/${item.value.id}/schedules` + (editing.value ? '/' + editing.value : ''), method: editing.value ? 'PUT' : 'POST', data: { ...data, startMinute: minutes(data.startTime!), endMinute: minutes(data.endTime!) } }); editorOpen.value = false; await load(); emit('changed') }
  catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
async function remove() {
  if (!item.value || !deleting.value || saving.value) return; saving.value = true
  try { await request({ url: `/teaching-classes/${item.value.id}/schedules/${deleting.value}`, method: 'DELETE', params: { confirm: true } }); deleting.value = null; await load(); emit('changed') }
  catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
watch(item, async value => { if (!value) return; rows.value = []; await load() })
</script>

<template>
  <UModal v-model:open="open" title="维护先修课程" :description="course?.name">
    <template #body><div class="space-y-4">
      <div class="flex gap-2"><UInput v-model="keyword" placeholder="搜索候选课程代码或名称" aria-label="搜索先修候选" @keyup.enter="search" /><UButton :loading="loading" @click="search">查询先修候选</UButton></div>
      <label class="grid gap-1 text-sm">先修课程<select v-model="selected" aria-label="先修课程" class="rounded-md border border-(--ui-border) bg-(--ui-bg) p-2"><option value="">请选择</option><option v-for="item in candidates" :key="item.id" :value="String(item.id)">{{ item.courseCode }} · {{ item.name }}</option></select></label>
      <UButton :disabled="!selected || saving" :loading="saving" @click="add">添加先修要求</UButton>
      <ul class="space-y-3"><li v-for="item in current" :key="item.prereqCourseId" class="flex items-center justify-between gap-2"><span>{{ item.courseCode }} · {{ item.name }}</span><UButton size="sm" color="error" variant="outline" :disabled="saving" @click="remove(item.prereqCourseId)">移除</UButton></li></ul>
      <p v-if="!current.length" class="text-sm text-(--ui-text-muted)">当前没有先修要求。循环依赖与重复关系将被服务器拒绝。</p>
    </div></template>
  </UModal>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { request } from '@/api/request'
import { getCourses } from '@/api/base'
import type { CourseItem } from '@/api/types'
const course = defineModel<CourseItem | null>('course', { required: true })
const emit = defineEmits<{ changed: [] }>()
const open = computed({ get: () => !!course.value, set: v => { if (!v) course.value = null } })
const keyword = ref(''), selected = ref(''), loading = ref(false), saving = ref(false)
const candidates = ref<CourseItem[]>([]), current = ref<{ prereqCourseId: number; courseCode: string; name: string }[]>([])
async function load() { if (course.value) current.value = await request({ url: `/courses/${course.value.id}/prereqs` }) }
async function search() {
  loading.value = true
  try { candidates.value = (await getCourses({ keyword: keyword.value || undefined, size: 100 })).records.filter(c => c.id !== course.value?.id) }
  catch { candidates.value = [] } finally { loading.value = false }
}
async function add() {
  if (!course.value || !selected.value || saving.value) return; saving.value = true
  try { await request({ url: `/courses/${course.value.id}/prereqs`, method: 'POST', data: { prereqCourseId: selected.value } }); await load(); emit('changed') }
  catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
async function remove(id: number) {
  if (!course.value || saving.value) return; saving.value = true
  try { await request({ url: `/courses/${course.value.id}/prereqs/${id}`, method: 'DELETE' }); await load(); emit('changed') }
  catch { /* 请求层显示业务错误 */ } finally { saving.value = false }
}
watch(course, async value => { if (!value) return; keyword.value = ''; selected.value = ''; current.value = []; await Promise.all([load(), search()]) })
</script>
